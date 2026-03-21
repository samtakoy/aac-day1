# Remaining Implementation Plan

## Status: Phase 4 was reverted (AgentBehaviorMode deleted as dead code)

## What was done:
- ✅ Phase 1: ToolRegistry Rename
- ✅ Phase 2: LlmExecutionRequest  
- ✅ Phase 3: WorkerEvent.Grouped Structure
- ❌ Phase 4: AgentBehaviorMode — **DELETED** (was dead code)

---

## Phase 4: AgentBehaviorMode Integration (NOT DONE)

**Original plan required:**
1. Create `AgentBehaviorMode.kt` enum — ✅ DONE (but DELETED)
2. Integrate into `AIAgentFactory.kt` — NOT DONE
3. Integrate into `CommandHandlerModule.kt` — NOT DONE

### Step-by-step implementation:

#### Step 1: Recreate AgentBehaviorMode enum
```kotlin
enum class AgentBehaviorMode {
    SINGLE_RUN,      // → SimpleWorker
    CONVERSATIONAL,  // → TalkWorker
    TASK_ORCHESTRATOR // → TaskWorker
}
```

#### Step 2: Add factory method to AIAgentFactory
```kotlin
fun createAgentForMode(
    mode: AgentBehaviorMode,
    systemName: String,
    chatId: Long,
    ...
): AIAgent
```

#### Step 3: Modify CommandHandlerModule
Add `@IntoMap` binding so workers can be selected by mode:
```kotlin
@Binds
@IntoMap
abstract fun bindTalkWorker(handler: TalkWorker): AWorker

@Binds
@IntoMap  
abstract fun bindTaskWorker(handler: TaskWorker): AWorker

abstract fun workerForMode(mode: AgentBehaviorMode): Provider<AWorker>
```

---

## Phase 5: AIAgent.process() using LlmExecutionRequest (Optional)

**Current state:** AIAgent.process() uses old 8-parameter signature
**Target:** Use new LlmExecutionRequest

#### Step-by-step:
1. Modify AIAgent.process() to create LlmExecutionRequest
2. Call orchestrator.execute(request, onEvent)
3. Test that behavior is unchanged

---

## Phase 6: User in the Loop UI (Future Work)

**Current:** Infrastructure ready (events emit)
**Needed:** UI layer to listen and show confirmation dialogs

This is OUTSIDE the original plan scope - requires UI changes.
