# Implementation Plan: Koog-Style Agent Architecture for Day Project

## Problem Statement

The current `AIAgent` implementation in the Day project has architectural issues that make it:
- **Inflexible** for user interaction scenarios (no streaming, no confirmation hooks)
- **Inconsistent naming** compared to Koog framework

### Current State

```kotlin
class AIAgent(
    val config: AgentConfig,
    private val contextRepository: AgentContextRepository,
    private val llmProvider: LlmRequestUseCase,
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,
    private val toolProvider: ToolProvider,
    private val orchestrator: ToolCallOrchestrator
)
```

### Reference Architecture (JetBrains Koog)

Koog provides a cleaner naming convention:
```kotlin
class AIAgent(
    val config: AgentConfig,
    val strategy: AgentStrategy,
    val llm: LlmRequestUseCase,
    val tools: ToolRegistry
)
```

---

## Motivation

### 1. Consistent Naming with Koog
Day uses `ToolProvider`, Koog uses `ToolRegistry`. Consistent naming helps when switching between codebases.

### 2. User in the Loop
Koog uses event-based streaming (`Flow<StreamFrame>`) where:
- Orchestrator emits events without blocking
- UI layer observes events and can pause/resume
- User confirmation happens in UI, not in orchestrator

Current Day implementation executes ALL tool calls automatically with no way to:
- Show real-time tool call progress
- Ask user confirmation before dangerous operations
- Cancel tool execution

### 3. Extensibility
The current architecture mixes concerns. Adding new behavior modes (single-run, conversational, planner) requires adding new Workers.

---

## Implementation Phases

### Phase 1: ToolRegistry Rename

**Problem:** `ToolProvider` interface name is misleading and inconsistent with Koog

**Solution:** Rename `ToolProvider` → `ToolRegistry`

**Changes:**

```kotlin
// File: app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolRegistry.kt
interface ToolRegistry {
    suspend fun getTools(agentId: Long? = null): List<ModelRequest.Tool>
    suspend fun getToolToServerMap(): Map<String, String>
    suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String>
}
```

**Deprecation for backward compatibility:**
```kotlin
@Deprecated("Use ToolRegistry instead", ReplaceWith("ToolRegistry"))
typealias ToolProvider = ToolRegistry
```

**Files to modify:**
| File | Change |
|------|--------|
| `ToolProvider.kt` | Rename to `ToolRegistry.kt`, rename interface inside |
| `McpToolProvider.kt` | Update class declaration to implement `ToolRegistry` |
| `AIAgent.kt` | Update import and constructor parameter |
| `AIAgentFactory.kt` | Update field type and constructor |
| `AgentCoreFeatureModule.kt` | Check for any type references |
| Any other files using `ToolProvider` | Update imports |

**Backward Compatibility:** ✅ Full — typealias provides alias, no breaking changes

**Motivation:**
- Consistent with Koog naming (`ToolRegistry`)
- Better reflects purpose: "registry" of available tools

---

### Phase 2: LlmExecutionRequest

**Problem:** `ToolCallOrchestrator.execute()` has 8 parameters — unwieldy

**Solution:** Create a request data class

**Changes:**

```kotlin
// File: app/src/main/java/com/example/day/core/core_features/agent/domain/tools/LlmExecutionRequest.kt
data class LlmExecutionRequest(
    val initialHistory: List<ModelRequest.Message>,
    val memoryMessages: List<AContextMessage>,
    val prompt: AContextMessage,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>,
    val context: ToolCallContext
) {
    companion object {
        const val DEFAULT_MAX_TOOL_LOOPS = 10
    }
}
```

**Modified interface:**
```kotlin
// File: ToolCallOrchestrator.kt
interface ToolCallOrchestrator {
    // New preferred signature
    suspend fun execute(
        request: LlmExecutionRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<ToolCallingResult>

    // Keep old signature as deprecated for backward compatibility
    @Deprecated("Use execute(request, onEvent) instead")
    suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> = execute(
        LlmExecutionRequest(
            initialHistory, memoryMessages, prompt, systemPrompt,
            modelSettings, tools, context
        ),
        onEvent
    )
}
```

**Files to modify:**
| File | Change |
|------|--------|
| `LlmExecutionRequest.kt` | Create new file |
| `ToolCallOrchestrator.kt` | Add new signature, keep old as deprecated |
| `ToolCallOrchestratorImpl.kt` | Implement new signature |
| `AIAgent.kt` | Optionally use new request in process() |

**Backward Compatibility:** ✅ Full — old API delegates to new

**Motivation:**
- Single object to pass instead of 8 parameters
- Easier to test (mock one object vs 8 params)
- Can add new fields to request without breaking signature
- Prepare for future streaming support

---

### Phase 3: WorkerEvent.Grouped Structure

**Problem:** `WorkerEvent` sealed class mixes unrelated events — hard to discover related events

**Solution:** Add sealed interface grouping (keeping existing class names)

**Changes:**

```kotlin
// File: app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/WorkerEvent.kt
sealed interface WorkerEvent {
    // Lifecycle events
    object RequestStart : WorkerEvent
    class RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    class RequestError(val text: String) : WorkerEvent

    // Tool events — grouped under sealed interface
    sealed interface Tool : WorkerEvent {
        // KEEP existing class names (NOT renamed!)
        class ToolCallStarted(
            val toolCallId: String,
            val toolName: String,
            val arguments: String
        ) : Tool

        class ToolCallFinished(
            val toolCallId: String,
            val toolName: String,
            val result: String,
            val isError: Boolean
        ) : Tool
    }

    // Planner events — grouped under sealed interface
    sealed interface Planner : WorkerEvent {
        class StageCreationSuggested(
            val stageTitle: String,
            val workingSummary: String
        ) : Planner

        class StageCompleted(
            val chatId: Long,
            val artifactContent: String
        ) : Planner

        class FactSaved(
            val memoryKey: String,
            val category: String,
            val fact: String
        ) : Planner
    }
}
```

**Note:** Existing class names are PRESERVED. Only interface hierarchy is added for grouping.

**Files to modify:**
| File | Change |
|------|--------|
| `WorkerEvent.kt` | Restructure to add interface grouping |
| `ToolCallOrchestratorImpl.kt` | Update emit calls to `WorkerEvent.Tool.ToolCallStarted` |
| `TalkWorker.kt` | Update `is WorkerEvent.ToolCallStarted` → `is WorkerEvent.Tool.ToolCallStarted` |
| All other files using WorkerEvent | May need to update `is` checks to include new interface path |

**⚠️ Breaking Change:** Code using `is WorkerEvent.ToolCallStarted` will break. Must change to `is WorkerEvent.Tool.ToolCallStarted`.

**Motivation:**
- Clearer grouping without new event types
- Easier to find all tool-related events
- IDE autocomplete helps discover related events

---

### Phase 4: AgentBehaviorMode (Optional Enhancement)

**Problem:** Workers (`SimpleWorker`, `TalkWorker`, `TaskWorker`) are hardcoded in `CommandHandlerModule`

**Solution:** Add enum for configurable behavior modes

**Changes:**

```kotlin
// File: app/src/main/java/com/example/day/core/core_features/agent/domain/AgentBehaviorMode.kt
enum class AgentBehaviorMode {
    /** One-shot request, no conversation history */
    SINGLE_RUN,
    
    /** Conversational with memory and context compression */
    CONVERSATIONAL,
    
    /** Multi-step task planning and execution */
    TASK_ORCHESTRATOR
}
```

**Note:** Don't add `AgentStrategy` interface — it would conflict with existing `ContextStrategy`.

The existing Worker pattern already provides behavior mode selection. This phase just makes it configurable.

**Files to modify (optional):**
| File | Change |
|------|--------|
| `AgentBehaviorMode.kt` | Create enum |
| `AIAgentFactory.kt` | Add worker selection based on mode |
| `CommandHandlerModule.kt` | Inject modes and select worker |

**Backward Compatibility:** ✅ Optional — existing code continues to work

---

## User in the Loop: How It Really Works

### Current Architecture
```
ToolCallOrchestrator
    ↓ execute()
    ↓ emits events via onEvent
TalkWorker
    ↓ adds messages to chat
Chat UI
```

### Problem
- All tool calls execute automatically
- No way to ask user before dangerous operations
- No streaming — user sees results only after completion

### Correct Approach (Event-Based)

```
ToolCallOrchestrator
    ↓ emits WorkerEvent.Tool.Started/Finished
    ↓ (does NOT block)
TalkWorker/ViewModel
    ↓ observes events
    ↓ if dangerous tool → show confirmation dialog
    ↓ stores decision in shared state
User
    ↓ confirms/denies in UI
[Continue or cancel based on user decision]
```

**Key insight:** Orchestrator does NOT need to know about confirmation. It just emits events. User confirmation is handled by the UI layer observing events.

**No new APIs needed for this** — existing `WorkerEvent.Tool.*` events are sufficient. The change is in how the UI layer uses them.

---

## Summary: Files to Modify

| Phase | File | Change Type | Priority |
|-------|------|-------------|----------|
| 1 | `ToolProvider.kt` → `ToolRegistry.kt` | Rename interface | HIGH |
| 1 | `McpToolProvider.kt` | Update declaration | HIGH |
| 1 | `AIAgent.kt` | Update import/param type | HIGH |
| 1 | `AIAgentFactory.kt` | Update field type | HIGH |
| 2 | `LlmExecutionRequest.kt` (new) | Request data class | MEDIUM |
| 2 | `ToolCallOrchestrator.kt` | Add new signature | MEDIUM |
| 2 | `ToolCallOrchestratorImpl.kt` | Implement new | MEDIUM |
| 3 | `WorkerEvent.kt` | Add interface grouping | LOW |
| 3 | `TalkWorker.kt` | Update is-checks | LOW |
| 4 | `AgentBehaviorMode.kt` (new) | Enum | LOW |

---

## Execution Order

1. **Phase 1** — ToolRegistry rename (SIMPLEST, safest)
2. **Phase 2** — LlmExecutionRequest (useful, low risk)
3. **Phase 3** — WorkerEvent grouping (breaking change — do after phases 1-2)
4. **Phase 4** — AgentBehaviorMode (optional enhancement)

**Recommended:** Complete phases 1-2 before adding new features. Phase 3 is breaking — test thoroughly.

---

## Testing Strategy

| Phase | Test Approach |
|-------|---------------|
| 1 | Rename with typealias = no test changes needed |
| 2 | Add tests for new `execute(request, onEvent)` signature |
| 3 | Update test event references — WILL break tests |
| 4 | Add tests for behavior mode selection (optional) |

---

## Success Criteria

After implementation:
1. ✅ `ToolRegistry` interface with Koog-consistent naming
2. ✅ `ToolCallOrchestrator.execute(request, onEvent)` with request object
3. ✅ `WorkerEvent.Tool` interface grouping for tool-related events
4. ⚠️ Phase 3 is breaking - tests will need updates
5. ✅ Optional: `AgentBehaviorMode` enum for configurable behavior
6. ✅ User in the loop handled via existing `onEvent` callback
