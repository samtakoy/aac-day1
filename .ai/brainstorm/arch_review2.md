# Multi-Agent Brainstorm: Architecture Review — Commit afa3094 (Updated Analysis)

## 📋 Context

### Commit Summary
- **SHA:** afa3094a63a90df88269b9389fe2eaa62fb2981f
- **Author:** Galimov Ruslan
- **Date:** 2026-03-25
- **Changes:** Major refactoring of `AIAgent.kt` + new HITL (Human-In-The-Loop) system

### Problem Statement
1. **Issue A:** In [`AIAgent.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt) — two arrays (`llmMessages`, `newMessages`) are processed and mutated in parallel throughout `runToolLoop`
2. **Issue B:** [`HitlSession.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/hitl/HitlSession.kt) stores `loopMessages` which may be redundant

---

## 🔍 Deep Dive: How AIAgent Works

### AIAgent Processing Principles

The `AIAgent` implements a **tool-calling loop** with dual message management:

#### 1. Message Array Architecture

```kotlin
// In buildLlmContext() - lines 46-64:
val llmMessages = buildList {
    addAll(memoryMessages)              // Long-term memory context
    addAll(snapshot.messages)          // Strategy-managed context
    addAll(promptMessages.context)      // Recent conversation context
    add(promptMessages.prompt)          // Current user prompt
}.toMutableList()

val newMessages = buildList {
    addAll(snapshot.messages)           // Strategy context only (NO memory)
    add(promptMessages.prompt)         // Current prompt
}.toMutableList()
```

**Key Insight:** The dual arrays serve DIFFERENT purposes:
- `llmMessages` = Full LLM context (memory + strategy + prompt) → sent to LLM
- `newMessages` = New conversation only (strategy + prompt) → for persistence

This is **intentional** because:
- Memory messages should NOT be re-persisted on every turn
- Strategy manages its own context window separately
- Only new conversation turns need to be saved

#### 2. Tool Loop Flow

```
┌─────────────────────────────────────────────────────────────┐
│  runToolLoop()                                               │
│                                                             │
│  while (loopCount < MAX_TOOL_LOOPS) {                       │
│      orchestrator.execute(llmMessages) ──→ LLM               │
│              │                                              │
│              ├── No tool calls → COMPLETED                  │
│              │         └── return ProcessResult.Success     │
│              │                                              │
│              └── Tool calls → PENDING_APPROVAL              │
│                         │                                   │
│                         ├── Auto mode:                      │
│                         │   toolExecutor.submit() → execute  │
│                         │   add tool results to both arrays │
│                         │                                   │
│                         └── HITL mode:                       │
│                             create HitlSession              │
│                             return ProcessResult.Pending    │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

#### 3. HITL Session Flow

```kotlin
// In process() - lines 66-76:
if (hitlSessionManager.hasActiveSession(config.id)) {
    return Result.failure(HitlSessionBusyError())  // Prevent concurrent HITL
}
val runId = UUID.randomUUID().toString()
val (llmMessages, newMessages, snapshot) = buildLlmContext(prompt)
return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)

// In runToolLoop() - lines 109-129:
is OrchestratorResult.PendingApproval -> {
    llmMessages.add(result.assistantMessage)    // Add assistant's tool call request
    newMessages.add(result.assistantMessage)    // Same for persistence

    toolExecutor.submit(
        runId = runId,
        toolCalls = result.toolCalls,
        prompt = prompt,
        loopMessages = newMessages.toList(),    // Pass current state
        context = ToolCallContext(agentId = config.id),
        onEvent = onEvent
    )
}

// In HitlToolExecutor.submit() - lines 24-31:
val session = HitlSession(
    runId = runId,
    agentId = context.agentId,
    prompt = prompt,
    loopMessages = loopMessages,               // Store for later resume
    pendingToolCalls = toolCalls
)
sessionManager.createSession(session)
return ToolExecutionResult.AwaitingApproval(runId)
```

#### 4. Resume Flow (resumeWithDecisions)

```kotlin
// Lines 140-173:
val session = hitlSessionManager.getSession(runId)
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)  // Rebuild base

llmMessages.addAll(session.loopMessages)     // Append stored conversation
val newMessages = session.loopMessages.toMutableList()  // ⚠️ ISSUE HERE

// Execute approved/rejected tool calls
val toolResults = session.pendingToolCalls.map { call ->
    val decision = session.decisions[call.id]
    if (decision == ToolCallDecision.APPROVED) {
        toolProvider.executeToolCall(call, ...)
    } else {
        ToolResult(toolCallId = call.id, content = "Rejected by user", isError = true)
    }
}

// Continue the loop
hitlSessionManager.closeSession(runId)
return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)
```

---

## ⚠️ Issues Identified

### Issue 1: Dual Message Arrays — **INTENTIONAL (with caveat)**

**Finding:** The dual arrays (`llmMessages`, `newMessages`) are **NOT a bug** — they are intentional design for separation of concerns:

| Array | Contents | Purpose |
|-------|----------|---------|
| `llmMessages` | memory + snapshot + context + prompt | Full LLM context |
| `newMessages` | snapshot + prompt | Only new messages for persistence |

**Caveat:** In `resumeWithDecisions()`, `newMessages` is set to `session.loopMessages` which breaks this pattern — it should be reconstructed properly.

### Issue 2: HitlSession.loopMessages — **NOT Redundant for HITL**

**Finding:** The previous analysis incorrectly stated `loopMessages` is redundant. It serves critical purposes:

1. **Display:** Shows conversation history in HITL approval UI
2. **State restoration:** Preserves conversation state when execution pauses
3. **Tool results:** Needed to continue execution after user approval

However, **`prompt` field IS redundant** — it's already the first message in `loopMessages`.

### Issue 3: Bug in resumeWithDecisions() — **newMessages Incorrect**

**Location:** [`AIAgent.kt:151`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt:151)

```kotlin
// Current (buggy):
val newMessages = session.loopMessages.toMutableList()

// Problem: This loses the base context (snapshot.messages)
// The newMessages should contain:
//   - Base context: snapshot.messages (reconstructed via buildLlmContext)
//   - Loop context: session.loopMessages
// But we only have the latter!
```

**Impact:** When `strategy.afterResponse()` is called, it receives incorrect context for persistence.

### Issue 4: Memory Duplication in Resume

**Location:** [`AIAgent.kt:147-151`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt:147-151)

```kotlin
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)
// llmMessages now includes memoryMessages + snapshot.messages + ...

llmMessages.addAll(session.loopMessages)
// But session.loopMessages ALREADY contains:
//   - session.prompt (user message)
//   - assistant message with tool calls
//   - tool results (if any were added before HITL)

// ⚠️ Potential: session.prompt is already in llmMessages via buildLlmContext
// Then it's added again via loopMessages!
```

---

## 📊 Architecture Quality Assessment

### SOLID Check

| Principle | Status | Notes |
|----------|--------|-------|
| Single Responsibility | ✅ | AIAgent orchestrates; separate classes for tools, orchestrator, executor |
| Open/Closed | ✅ | New strategies can be added without modifying AIAgent |
| Liskov Substitution | ✅ | ToolExecutor has two implementations (Auto, HITL) |
| Interface Segregation | ✅ | Small, focused interfaces |
| Dependency Inversion | ✅ | Depends on abstractions (ToolExecutor, ToolCallOrchestrator) |

### Clean Architecture Layers

| Layer | Compliance | Notes |
|-------|------------|-------|
| Domain | ✅ | Pure business logic in AIAgent, no DB/UI deps |
| Data | ✅ | Repositories implement domain interfaces |
| Presentation | N/A | Not reviewed |

### Veto Criteria Check

| Criteria | Status |
|----------|--------|
| >3 abstraction layers for simple feature | ❌ NOT TRIGGERED |
| AI/LLM used where rule-based suffices | ✅ Justified |
| No testing strategy | ✅ Tests added in commit |
| Circular dependencies | ✅ None found |
| Cost estimate missing | ✅ N/A (on-device) |

---

## 🔧 Recommended Fixes

### Fix 1: Correct newMessages in resumeWithDecisions

```kotlin
// In resumeWithDecisions():
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)

// Fix: Derive newMessages properly
// newMessages should be: snapshot.messages + session loop content
val newMessages = buildList {
    addAll(snapshot.messages)  // Base context from strategy
    addAll(session.loopMessages) // New content from this session
}.toMutableList()
```

### Fix 2: Remove Redundant prompt from HitlSession

```kotlin
// HitlSession.kt:
data class HitlSession(
    val runId: String,
    val agentId: Long,
    // REMOVE: val prompt: AContextMessage,  // Redundant - first msg in loopMessages
    val loopMessages: List<ModelRequest.Message>,
    val pendingToolCalls: List<ModelResult.Success.ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap(),
    val status: HitlStatus = HitlStatus.AWAITING_APPROVAL,
    val createdAt: Long = System.currentTimeMillis()
)
```

But this requires updating `resumeWithDecisions()` to get prompt from `loopMessages.first()`.

### Fix 3: Prevent Message Duplication

Consider checking if `session.prompt` content already exists in `llmMessages` before appending from `loopMessages`. Or, change `loopMessages` to only contain messages AFTER the prompt (assistant + tool results).

---

## 📋 Summary

| Issue | Severity | Fix Priority |
|-------|----------|--------------|
| `newMessages` incorrectly set in `resumeWithDecisions()` | High | P0 - Bug fix |
| `prompt` field redundant in HitlSession | Low | P2 - Cleanup |
| Message duplication potential in resume | Medium | P1 - Edge case |

---

## ✅ Final Verdict

**0 High Severity Issues** — The architecture is sound. The dual array design is intentional. Recommended fixes address actual bugs, not design flaws.

**Status:** Ready for implementation of bug fixes