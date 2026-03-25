# Multi-Agent Brainstorm: Architecture Review — Commit afa3094

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
| `AIAgent` | Main orchestrator | ⚠️ Dual message arrays - confusion potential |
| `HitlSession` | Stores session state for HITL | ⚠️ `loopMessages` - partially redundant |
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
- **Problem:** `loopMessages` duplication analysis needed

### Risks
1. **Data inconsistency:** `llmMessages` and `newMessages` can diverge during loop iterations
2. **Memory waste:** HITL session stores full `loopMessages` when partial state might suffice
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

val newMessages = buildList {  // NEW messages only: snapshot + prompt (NO memory)
    addAll(snapshot.messages...)
    if (prompt.content.isNotBlank()) add(promptMessages.prompt...)
}.toMutableList()
```

**Analysis:** The arrays serve DIFFERENT purposes intentionally:
- `llmMessages` — full LLM context (grows with tool results) including memory
- `newMessages` — messages for persistence (NO memory to avoid re-saving)

**But in resumeWithDecisions():**
```kotlin
// Line 151:
val newMessages = session.loopMessages.toMutableList()  // ⚠️ BUG!

// This loses base context (snapshot.messages) - only has loop content!
```

#### Problem 2: HITL Session Redundancy

**Current State:**
```kotlin
// In resumeWithDecisions():
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)  // Rebuilds from scratch
llmMessages.addAll(session.loopMessages)  // Appends stored messages
```

**Finding:** `loopMessages` is NOT fully redundant because:
1. Needed for HITL approval UI display
2. Needed for state restoration when pausing
3. Contains conversation context for tool execution

**BUT `prompt` field IS redundant** — it's the first message in `loopMessages`.

### Options

| Option | Description | Trade-off |
|--------|-------------|-----------|
| **A: Single Array** | Merge into one `messageHistory` | Simpler but loses separation of concerns |
| **B: Keep Dual Arrays** | Fix `resumeWithDecisions()` bug | Preserves design intent, minimal change |
| **C: Cursor-based** | Replace `loopMessages` with cursor | Cleaner but breaks HITL UI state |

### Recommended Approach: **Option B** (Fix resumeWithDecisions Bug)

**Rationale:**
1. The dual array design is intentional — preserves memory/persistence separation
2. Bug is in `resumeWithDecisions()` where `newMessages` is incorrectly derived
3. Minimal change, preserves existing behavior
4. HITL session state (`loopMessages`) IS needed for UI

### Risks
- **Bug fix:** `newMessages` construction in resume path
- **Design clarification:** Document why dual arrays exist

**Status: Provided** — Ready for Kotlin Developer

---

## 💻 Implementation

**Agent:** Kotlin Developer  
**Confidence:** Medium  
**Assumptions:**
- HITL sessions can be rebuilt from persisted context
- `AIAgent` has access to context repository during resume

### Proposed Refactoring

#### 1. Fix newMessages in resumeWithDecisions

```kotlin
// In resumeWithDecisions() - line ~151:
// BEFORE (buggy):
val newMessages = session.loopMessages.toMutableList()

// AFTER (fixed):
val newMessages = buildList {
    addAll(snapshot.messages)  // Base context from strategy
    addAll(session.loopMessages)  // New content from this session
}.toMutableList()
```

#### 2. Remove Redundant prompt from HitlSession

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

But requires updating `resumeWithDecisions()`:
```kotlin
val prompt = session.loopMessages.firstOrNull()  // Get from loopMessages
    ?: return Result.failure(IllegalStateException("No prompt in session"))
```

### Module Structure
```
agent/domain/
├── AIAgent.kt                    # Bug fix in resumeWithDecisions
├── tools/hitl/
│   ├── HitlSession.kt           # Remove redundant prompt field
│   ├── HitlSessionManager.kt     # No changes needed
│   └── HitlToolExecutor.kt       # Update to not pass prompt separately
└── (other tools unchanged)
```

### Data Flow (After Fix)

```
1. process(prompt) → buildLlmContext() → llmMessages + newMessages
2. runToolLoop(llmMessages, newMessages) → orchestrator.execute()
3. If tool calls need HITL:
   - Create HitlSession (loopMessages contains full state for UI)
   - Return Pending
4. resumeWithDecisions(runId):
   - Get session
   - buildLlmContext(session.prompt) → snapshot + base context
   - Derive newMessages = snapshot.messages + session.loopMessages
   - Execute approved/rejected calls
   - Continue runToolLoop
```

**Status: Provided** — Ready for Debate

---

## ⚔️ Debate

**Agents:** All agents challenge each other

### AI System Architect challenges Senior Architect:
> "Your Option B keeps dual arrays. But isn't this overengineering? Could we use a single message history and derive persistence on-demand?"

**Counter:** The separation is intentional because:
1. Memory messages should NOT be re-persisted every turn (wasteful)
2. Strategy manages its own context window separately
3. Single array would require complex tracking of "which messages to persist"

### Senior Architect challenges AI System Architect:
> "You claim HITL session needs `loopMessages` for UI. But could we store minimal state and reconstruct UI state from strategy snapshot + tool calls?"

**Counter:** The `loopMessages` contains:
- User prompt
- Assistant's tool call request (with arguments)
- Tool execution results (before HITL pause)

Reconstructing this requires knowing the LLM response at pause point. The alternative is to store LLM response separately, which adds complexity.

### Kotlin Developer challenges both:
> "The `prompt` field in HitlSession IS clearly redundant. Why are we debating keeping it? This is dead code."

**Concession:** Both architects agree — `prompt` field should be removed. It's duplicative.

### Quality Reviewer challenges everyone:
> "Three issues found but only one is a real bug. The dual arrays and `loopMessages` are intentional design. Why are we overanalyzing?"

**Counter:** The bug in `resumeWithDecisions()` IS a real issue:
```kotlin
val newMessages = session.loopMessages.toMutableList()  // Line 151
// This means newMessages doesn't include snapshot.messages!
// When strategy.afterResponse() is called, it receives wrong context
```

### Unresolved Questions:
1. ~~Can message history always be reconstructed?~~ — YES (for persistence)
2. ~~Is HITL session storing too much?~~ — NO (need UI state)
3. ~~Is dual-array intentional?~~ — YES (memory/persistence separation)

**Debate Resolution:** All agents agree on:
1. Fix `newMessages` bug in `resumeWithDecisions()`
2. Remove redundant `prompt` field from HitlSession
3. Keep dual arrays as intentional design

---

## 🔍 Review

**Agent:** Quality Reviewer  
**Confidence:** High

### Issues Found

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| 1 | `newMessages` incorrectly derived in `resumeWithDecisions()` | **HIGH** | `AIAgent.kt:151` |
| 2 | `prompt` field redundant in HitlSession | Low | `HitlSession.kt:14` |
| 3 | Dual arrays may confuse future maintainers | Medium | Design doc needed |

### Suggested Fixes

1. **Fix `newMessages` construction:**
   ```kotlin
   val newMessages = buildList {
       addAll(snapshot.messages)
       addAll(session.loopMessages)
   }.toMutableList()
   ```

2. **Remove redundant `prompt` field:**
   - Update `HitlToolExecutor` to get prompt from `loopMessages.first()`
   - Update `HitlSession` data class

3. **Add architectural documentation:**
   - Document why dual arrays exist
   - Document HITL session state requirements

### Veto Criteria Check
- ✅ No more than 3 abstraction layers (currently 2)
- ✅ LLM usage justified (tool orchestration)
- ✅ Testing strategy exists (unit tests in commit)
- ✅ No circular dependencies
- ✅ Cost estimate: N/A (on-device)

### Final Verdict
**1 High Severity Issue** — Request revision for bug fix

---

## ✅ Final Decision

### Approved Changes

| Change | Priority | Owner |
|--------|----------|-------|
| Fix `newMessages` derivation in `resumeWithDecisions()` | **High** | Kotlin Developer |
| Remove redundant `prompt` field from HitlSession | Medium | Kotlin Developer |
| Add architecture documentation | Low | Senior Architect |

### Implementation Plan

1. **Step 1:** Fix `AIAgent.kt:151` - correct `newMessages` construction
2. **Step 2:** Update `HitlSession.kt` - remove `prompt` field
3. **Step 3:** Update `HitlToolExecutor.kt` - derive prompt from `loopMessages.first()`
4. **Step 4:** Update `AIAgent.resumeWithDecisions()` - get prompt from session

### Open Trade-offs

| Trade-off | Decision |
|-----------|----------|
| Dual arrays complexity | Accept - intentional design |
| `loopMessages` storage | Accept - needed for HITL UI |
| `prompt` redundancy | Remove - cleanup |

### Status
**Ready for Implementation** — Bug fix prioritized