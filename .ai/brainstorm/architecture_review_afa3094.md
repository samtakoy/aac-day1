# Multi-Agent Brainstorm: Architecture Review — Commit afa3094

## 📋 Context

### Commit Summary
- **SHA:** afa3094a63a90df88269b9389fe2eaa62fb2981f
- **Author:** Galimov Ruslan
- **Date:** 2026-03-25
- **Changes:** Major refactoring of `AIAgent.kt` + new HITL (Human-In-The-Loop) system

### Problem Statement
1. **Issue A:** In [`AIAgent.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt) — two arrays (`llmMessages`, `newMessages`) are processed and mutated in parallel throughout `runToolLoop`
2. **Issue B:** [`HitlSession.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/hitl/HitlSession.kt) stores `loopMessages` which may be redundant since `buildLlmContext()` rebuilds context from scratch

---

## 🤖 AI System Design

**Agent:** AI Systems Architect  
**Confidence:** High  
**Assumptions:** 
- HITL sessions are short-lived (24hr timeout)
- No distributed session state needed
- Single-device usage

### System Design Analysis

| Component | Current State | Issue |
|-----------|---------------|-------|
| `AIAgent` | Main orchestrator | ❌ Dual message arrays create confusion |
| `HitlSession` | Stores session state for HITL | ❌ `loopMessages` redundancy |
| `ToolExecutor` | Interface for auto/HITL execution | ✅ Clean abstraction |
| `HitlSessionManager` | In-memory session store | ✅ Simple, effective |

### Agent Interaction Flow (Current)

```
User Prompt → buildLlmContext() → [llmMessages + newMessages]
     ↓
runToolLoop() → orchestrator.execute()
     ↓
[Tool Calls?] → toolExecutor.submit()
     ↓
HITL: HitlSession created → await approval
     ↓
resumeWithDecisions() → rebuild context + append loopMessages
```

### Memory Strategy
- **Short-term:** In-memory `ConcurrentHashMap` for HITL sessions
- **Long-term:** Strategy-based context management (snapshot approach)
- **Problem:** `loopMessages` duplication in HITL session

### Risks
1. **Data inconsistency:** `llmMessages` and `newMessages` can diverge during loop iterations
2. **Memory waste:** HITL session stores full `loopMessages` when context rebuild is possible
3. **Debug complexity:** Two parallel mutable lists make tracing difficult

**Status: Provided** — Ready for Senior Architect review

---

## 🏗 Architecture

**Agent:** Senior Architect  
**Confidence:** High  
**Assumptions:**
- HITL is a temporary pause, not a branching scenario
- Message history is append-only during a run

### Problem Analysis

#### Problem 1: Dual Message Arrays

**Current State:**
```kotlin
// In buildLlmContext():
val llmMessages = buildList {  // FULL context: memory + snapshot + prompt
    addAll(memoryMessages.map { it.toModelRequestMessage() })
    addAll(snapshot.messages.toModelRequestMessages())
    addAll(promptMessages.context...)
    if (prompt.content.isNotBlank()) add(promptMessages.prompt...)
}.toMutableList()

val newMessages = buildList {  // NEW messages only: snapshot + prompt
    addAll(snapshot.messages...)
    if (prompt.content.isNotBlank()) add(promptMessages.prompt...)
}.toMutableList()
```

**Problem:** Both arrays are passed to `runToolLoop` and mutated together, but serve different purposes:
- `llmMessages` — full LLM context (grows with tool results)
- `newMessages` — messages to be persisted via strategy

#### Problem 2: HITL Session Redundancy

**Current State:**
```kotlin
// In resumeWithDecisions():
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)  // Rebuilds from scratch
llmMessages.addAll(session.loopMessages)  // Appends stored messages
```

**Problem:** If `buildLlmContext(session.prompt)` reconstructs the same messages that were stored in `loopMessages`, this is redundant storage.

### Options

| Option | Description | Trade-off |
|--------|-------------|-----------|
| **A: Single Source of Truth** | Keep only `llmMessages`, derive persistence messages on-demand | Simpler, but requires careful tracking |
| **B: Message Roles** | Use message role/type to distinguish LLM-only vs persistable | More explicit, adds complexity |
| **C: Context Rebuild + Cursor** | Remove `loopMessages` from HitlSession, use prompt + cursor position | Cleanest, but requires cursor tracking |

### Recommended Approach: Option C (Context Rebuild + Cursor)

**Rationale:**
1. `HitlSession` should store minimal state: `runId`, `agentId`, `prompt`, `pendingToolCalls`, `decisions`, `messageCursor`
2. `messageCursor` = index into context history indicating where this HITL pause started
3. On resume, reconstruct from context history up to cursor

### Risks
- **Breaking change:** HITL session schema would change
- **Cursor validity:** Need to ensure context history hasn't been compacted

**Status: Provided** — Ready for Kotlin Developer

---

## 💻 Implementation

**Agent:** Kotlin Developer  
**Confidence:** Medium  
**Assumptions:**
- HITL sessions can be rebuilt from persisted context
- `AIAgent` has access to context repository during resume

### Proposed Refactoring

#### 1. Simplify AIAgent to Single Message Array

```kotlin
class AIAgent(
    // ... other deps
) {
    // Single message list - serves both LLM and persistence
    private val messageHistory = mutableListOf<ModelRequest.Message>()
    
    suspend fun process(prompt: AContextMessage, onEvent: ...): Result<ProcessResult> {
        // Build initial context
        val memoryMessages = memoryProvider.getMemoryContext()
        val snapshot = strategy.process(config, contextRepository)
        
        // Initialize with full context
        messageHistory.clear()
        messageHistory.addAll(memoryMessages.map { it.toModelRequestMessage() })
        messageHistory.addAll(snapshot.messages.toModelRequestMessages())
        messageHistory.addAll(promptMessages.context.filter { it.content.isNotBlank() }.map { it.toModelRequestMessage() })
        if (prompt.content.isNotBlank()) {
            messageHistory.add(promptMessages.prompt.toModelRequestMessage())
        }
        
        return runToolLoop(messageHistory, snapshot, onEvent)
    }
    
    internal suspend fun runToolLoop(
        messages: MutableList<ModelRequest.Message>,
        snapshot: ContextStrategyResult,
        onEvent: ...
    ): Result<ProcessResult> {
        // ... loop logic - messages is the single source of truth
        // For persistence: strategy.afterResponse receives messages.copy() or sublist
    }
}
```

#### 2. Simplify HitlSession

```kotlin
data class HitlSession(
    val runId: String,
    val agentId: Long,
    val prompt: AContextMessage,
    // REMOVED: loopMessages - redundant, rebuild from context
    val messageHistorySnapshot: List<ModelRequest.Message>,  // Or use cursor index
    val pendingToolCalls: List<ModelResult.Success.ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap(),
    val status: HitlStatus = HitlStatus.AWAITING_APPROVAL,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Alternative - Use Cursor Position:**
```kotlin
data class HitlSession(
    val runId: String,
    val agentId: Long,
    val prompt: AContextMessage,
    val messageCursor: Int,  // Index into messageHistory when HITL started
    val pendingToolCalls: List<ModelResult.Success.ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap(),
    val status: HitlStatus = HitlStatus.AWAITING_APPROVAL,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Module Structure
```
agent/domain/
├── AIAgent.kt                    # Simplified, single message array
├── tools/hitl/
│   ├── HitlSession.kt           # Minimal state (no loopMessages)
│   ├── HitlSessionManager.kt
│   └── HitlToolExecutor.kt
└── (other tools unchanged)
```

### Data Flow (Refactored)

```
1. process(prompt) → buildLlmContext() → messageHistory
2. runToolLoop(messageHistory) → orchestrator.execute()
3. If tool calls need HITL:
   - Create HitlSession with messageCursor (not loopMessages!)
   - Return Pending
4. resumeWithDecisions(runId):
   - Get session, check decisions
   - Reconstruct: messageHistory.subList(0, session.messageCursor)
   - Append tool results based on decisions
   - Continue runToolLoop
```

**Status: Provided** — Ready for Debate

---

## ⚔️ Debate

**Agents:** All agents challenge each other

### AI System Architect challenges Kotlin Developer:
> "Your refactoring assumes message history is always rebuildable. What if the strategy uses summarization that discards old messages? The `loopMessages` in HitlSession might be the ONLY complete record."

**Counter:** The `snapshot` passed to `strategy.afterResponse()` should capture the complete message state. If summarization happens, the strategy is responsible for maintaining the canonical history.

### Senior Architect challenges AI System Architect:
> "You suggest using cursor position, but cursor validity after context compaction isn't guaranteed. What's the fallback?"

**Counter:** HITL sessions are short-lived (24hr timeout). Context compaction happens on longer timescales. The risk is minimal for typical usage.

### Quality Reviewer challenges both:
> "Is HITL even necessary for v1? Could we just auto-execute with confirmation UI?"

**Counter:** The requirement explicitly mentions HITL for safety-critical tool calls. This is a design decision, not overengineering.

### Unresolved Questions:
1. Can message history always be reconstructed after summarization?
2. Should HitlSession store minimal state (cursor) or full state (loopMessages)?
3. Is the dual-array approach intentional for some edge case?

---

## 🔍 Review

**Agent:** Quality Reviewer  
**Confidence:** High

### Issues Found

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| 1 | Dual message arrays (`llmMessages`, `newMessages`) create confusion and potential divergence | Medium | `AIAgent.kt:46-64` |
| 2 | `HitlSession.loopMessages` may be redundant if context is rebuildable | Medium | `HitlSession.kt:15` |
| 3 | No test coverage for message array synchronization edge cases | Medium | Test gap |
| 4 | `messageCursor` approach needs validation against summarization strategies | Low | Design |

### Suggested Fixes

1. **Merge message arrays into single `messageHistory`**
   - Single source of truth
   - Persist via `messages.toList()` or sublist

2. **Remove `loopMessages` from HitlSession, use `messageCursor: Int`**
   - Minimal session state
   - Rebuild on resume from context store

3. **Add synchronization tests**
   - Verify `llmMessages` and `newMessages` don't diverge
   - Or verify single array invariant

4. **Validate cursor approach with all strategy types**
   - ContextSlidingWindowStrategy
   - ContextSummaryStrategy
   - ContextStickyFactsStrategy

### Veto Criteria Check
- ✅ No more than 3 abstraction layers (currently 2)
- ✅ LLM usage justified (tool orchestration)
- ✅ ✅ Testing strategy exists (unit tests added in commit)
- ✅ No circular dependencies
- ✅ Cost estimate: N/A (on-device)

### Final Verdict
**0 High Severity Issues** — Proceed with Medium fixes

---

## ✅ Final Decision

### Approved Changes

| Change | Priority | Owner |
|--------|----------|-------|
| Merge `llmMessages` + `newMessages` into single `messageHistory` | High | Kotlin Developer |
| Replace `loopMessages` with `messageCursor: Int` in HitlSession | High | Kotlin Developer |
| Add message synchronization tests | Medium | QA |
| Document cursor validity contract for each ContextStrategy | Medium | Architect |

### Implementation Plan

1. **Sprint 1:**
   - Refactor `AIAgent` to single message array
   - Update `HitlSession` to use cursor
   - Update `HitlToolExecutor` and `HitlSessionManager`
   - Update `resumeWithDecisions` logic

2. **Sprint 2:**
   - Add synchronization tests
   - Validate with all context strategies
   - Update documentation

### Open Trade-offs

| Trade-off | Decision |
|----------|----------|
| Simplicity vs Rebuild Risk | Accept minimal session state (cursor) |
| Dual arrays vs Single array | Single array preferred, but verify no edge cases |

### Status
**Ready for Implementation** — All phases complete
