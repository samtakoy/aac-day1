# Brainstorming: JetBrains Koog vs Day Architecture

## Participants
- **Senior Architect** — System design, patterns, clean architecture
- **Senior Kotlin Developer** — Idiomatic Kotlin, type safety, language features
- **Agent System Expert** — Agent patterns, tool calling, user interaction

---

## Current Architecture Analysis (Day Project)

### AIAgent Structure

```
AIAgent(config, contextRepository, llmProvider, strategy, memoryProvider, toolProvider, orchestrator)
```

**Problems identified:**

1. **Tight Coupling**: AIAgent takes 7 constructor parameters — violates Single Responsibility
2. **No Strategy Pattern Abstraction**: Strategy is fixed at construction time, can't switch dynamically
3. **Tool Calling is Opaque**: ToolCallOrchestrator.execute() has 8 parameters — hard to configure
4. **User in the Loop is Scattered**: Handled in Workers (TalkWorker, etc.) with hardcoded logic
5. **Configuration is Implicit**: AgentConfig holds everything but there's no clear "agent type" or "behavior mode"

---

## Expert Analysis

### 1. Senior Architect

#### Issues with AIAgent Configuration

**Problem A: God Object Pattern**
```
AIAgent(config, contextRepository, llmProvider, strategy, memoryProvider, toolProvider, orchestrator)
```
This is a classic God Object. Every new feature adds another dependency.

**Problem B: Configuration Scattered**
`AgentConfig` contains:
- id, systemName, title, chatUserId, isCommon
- modelSettings, systemPrompt
- contextStrategyType
- memoryTypes

But strategy type and memory types are closely related but stored separately. No clear "agent persona" abstraction.

**Problem C: No Behavior Modes**
Koog has `singleRunStrategy`, `functionalStrategy`, graph-based strategies.
Day has Workers (`SimpleWorker`, `TalkWorker`, etc.) but they're:
- Hardcoded in CommandHandlerModule
- Not configurable per-agent
- Mixed with command parsing logic

**Solution Direction:**
```kotlin
// Koog-like approach
interface AgentStrategy {
    suspend fun execute(input: String, context: AgentExecutionContext): AgentResult
}

class AIAgent(
    val config: AgentConfig,
    val strategy: AgentStrategy,  // Single abstraction
    val llm: LlmRequestUseCase,
    val tools: ToolRegistry
)
```

---

### 2. Senior Kotlin Developer

#### Issues with Tool Calling

**Problem A: ToolProvider Interface is Too Broad**

```kotlin
interface ToolProvider {
    suspend fun getTools(agentId: Long? = null): List<ModelRequest.Tool>
    suspend fun getToolToServerMap(): Map<String, String>
    suspend fun executeToolCall(toolCall: ModelResult.Success.ToolCall, context: ToolCallContext): Result<String>
}
```

Issues:
- `agentId` parameter in `getTools()` suggests per-agent filtering, but this is a runtime concern
- No type-safe tool definitions — tools are just `ModelRequest.Tool` (raw JSON structure)
- No way to declare tool dependencies or side effects
- No user confirmation hooks for dangerous tools

**Problem B: Tool Annotations Missing**

Koog uses:
```kotlin
@Tool
@LLMDescription("Multiplies two numbers")
fun multiply(a: Int, b: Int): Int
```

Day has no such annotations — tools are defined via JSON schemas in MCP providers, not in code. This makes tools:
- Harder to document
- Impossible to validate at compile time
- Not type-safe

**Solution Direction:**
```kotlin
// Better structure
interface Tool {
    val name: String
    val description: String
    suspend fun execute(args: Map<String, JsonElement>): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val data: JsonElement?,
    val error: String?
)
```

---

### 3. Agent System Expert

#### Issues with User in the Loop

**Problem A: No User Confirmation for Tool Calls**

Koog agent calls a tool, it can be configured to ask user confirmation:
```kotlin
// Koog uses Flow<StreamFrame> - events stream IN, UI reacts
client.sendMessageStreaming(request).collect { response ->
    when (val event = response.data) {
        is Message -> print(event.text)
        is ToolCallRequest -> {
            // Can pause and ask user before executing
            // UI layer controls flow, not the orchestrator
        }
    }
}
```

Day's `ToolCallOrchestrator` executes ALL tools automatically:
```kotlin
while (loopIndex < MAX_TOOL_LOOPS) {
    val llmResult = llmProvider.askLlm(...)
    // ...
    val toolResult = toolProvider.executeToolCall(toolCall, context)  // NO USER CONFIRMATION
    // ...
}
```

**Problem B: No Streaming to User**

TalkWorker shows tool events as messages:
```kotlin
is WorkerEvent.ToolCallStarted -> {
    chatTools.addInfoMessage(chat.id, "${TOOL_EVENT_START_PREFIX}: ${event.toolName}")
}
```

This is append-only. User sees results only after tool completes.

**Key Insight: User confirmation is ASYNC, NOT callback-based**

Koog approach:
```
Orchestrator: Produces Flow<StreamFrame>
     ↓
Consumer (UI): Observes events, can pause/resume externally
     ↓
User: Confirms dangerous operations in UI layer
```

The orchestrator does NOT block waiting for user confirmation. Instead:
1. Orchestrator emits `WorkerEvent.Tool.*` through `onEvent` callback
2. UI layer (in TalkWorker or ViewModel) observes these events
3. If dangerous tool, UI shows confirmation dialog
4. **Orchestrator does NOT block** — it just emits events

---

## Key Findings Summary

| Issue | Current State (Day) | Koog Reference | Severity |
|-------|---------------------|----------------|----------|
| AIAgent configuration | 7 params, God object | `AIAgent(executor, strategy, model, toolRegistry)` | HIGH |
| Agent behavior modes | Hardcoded Workers | `singleRun`, `functionalStrategy`, graph | HIGH |
| Tool definitions | JSON schemas, runtime | `@Tool` annotations, compile-time | MEDIUM |
| Tool execution | Auto-execute all | Event-based streaming | MEDIUM |
| User interaction | Append-only messages | Streaming + UI-controlled confirmation | HIGH |
| Tool result | Raw String | Structured `ToolResult` | MEDIUM |

---

## Minimal Changes Required

### Phase 1: Clean Up AIAgent Configuration

**Files to modify:**
- `AIAgent.kt` — reduce parameters using context object
- `AIAgentFactory.kt` — simplify creation
- `AgentConfig.kt` — add behavior mode enum

**Changes:**
```kotlin
// New AgentExecutionContext (replaces 6 parameters)
data class AgentExecutionContext(
    val contextRepository: AgentContextRepository,
    val strategy: ContextStrategy,
    val memoryProvider: MemoryProvider,
    val toolRegistry: ToolRegistry,
    val orchestrator: ToolCallOrchestrator,
    val llmProvider: LlmRequestUseCase
)

// Simplified AIAgent
class AIAgent(
    val config: AgentConfig,
    val context: AgentExecutionContext
)
```

### Phase 2: Tool Registry Refactor

**New interface:**
```kotlin
interface ToolRegistry {
    suspend fun getTools(agentId: Long?): List<ModelRequest.Tool>
    suspend fun executeTool(toolCall: ModelResult.Success.ToolCall, context: ToolCallContext): Result<String>
}
```

**Files to modify:**
- `ToolProvider.kt` → rename to `ToolRegistry`
- `McpToolProvider.kt` → implement renamed interface

### Phase 3: User in the Loop — Event-Based (No Blocking Callbacks)

**Correct WorkerEvent structure:**
```kotlin
sealed interface WorkerEvent {
    // Existing lifecycle events
    object RequestStart : WorkerEvent
    class RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    class RequestError(val text: String) : WorkerEvent

    // Tool events as separate sealed interface (grouping, not new events)
    sealed interface Tool : WorkerEvent {
        class Started(
            val toolCallId: String,
            val toolName: String,
            val arguments: String
        ) : Tool

        class Finished(
            val toolCallId: String,
            val toolName: String,
            val result: String,
            val isError: Boolean
        ) : Tool
    }
}
```

**No `ConfirmationResult` enum** — user confirmation is handled externally by the UI layer observing events.

### Phase 4: Agent Behavior Modes (Optional Enhancement)

```kotlin
enum class AgentBehaviorMode {
    SINGLE_RUN,      // One-shot, no history
    CONVERSATIONAL,  // With memory and context
    TASK_ORCHESTRATOR // Multi-step planning
}

interface AgentStrategy {
    suspend fun execute(input: String, context: AgentExecutionContext): AgentResult
}
```

---

## Detailed Changes: ToolCallOrchestrator

### Current State

```kotlin
interface ToolCallOrchestrator {
    suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult>
}
```

**Current assessment:**
- 8 parameters are unwieldy but **functional**
- `onEvent` callback is the **only** interaction mechanism
- No streaming — tool results only after completion

### Minimal Change: Request Object (Non-Breaking)

```kotlin
data class LlmExecutionRequest(
    val initialHistory: List<ModelRequest.Message>,
    val memoryMessages: List<AContextMessage>,
    val prompt: AContextMessage,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>,
    val context: ToolCallContext
)

interface ToolCallOrchestrator {
    // New preferred signature
    suspend fun execute(
        request: LlmExecutionRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<ToolCallingResult>

    // Keep old signature as deprecated (backward compatible)
    @Deprecated("Use execute(request, onEvent)")
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

### How User in the Loop Really Works

```
Orchestrator emits events via onEvent callback:
  - WorkerEvent.Tool.Started
  - WorkerEvent.Tool.Finished

UI layer (TalkWorker/ViewModel) observes:
  - If dangerous tool → show confirmation dialog
  - Store decision in shared state (e.g., Flow)
  - Orchestrator does NOT block
  
User confirms in UI → decision stored → continues
```

**Key insight:** The orchestrator doesn't need to know about user confirmation. It just emits events. The UI layer handles confirmation by controlling what happens after observing events.

### Files to Modify (Minimal)

| Priority | File | Change Type |
|----------|------|-------------|
| 2 | `ToolCallOrchestrator.kt` | Add `LlmExecutionRequest` data class (optional, non-breaking) |
| 2 | `ToolCallOrchestratorImpl.kt` | No changes needed |
| 2 | `WorkerEvent.kt` | Add `Tool` sealed interface grouping (additive, organizational) |
| 3 | `AIAgent.kt` | Optionally use new `LlmExecutionRequest` |
| 3 | `ToolProvider.kt` | Rename to `ToolRegistry` (non-breaking alias) |

---

## Migration Path

### Backward Compatibility
1. Keep `ToolProvider` as deprecated alias for `ToolRegistry`
2. Keep old `execute()` signature with `@Deprecated` annotation
3. No breaking changes to existing code

### New APIs (Optional/Additive)
1. Introduce `LlmExecutionRequest` data class (opt-in)
2. Add `WorkerEvent.Tool` sealed interface grouping (purely organizational)
3. Add `AgentBehaviorMode` enum (optional, non-breaking)

### Testing Strategy
1. Existing tests continue to work (no signature changes)
2. New `WorkerEvent.Tool` interface is additive
3. Grouping existing events under `Tool` interface is purely organizational

---

## Summary of Corrections from Initial Proposal

| Initial Proposal | Issue | Corrected Approach |
|-----------------|-------|-------------------|
| `UserInteractionCallback` with `ConfirmationResult` | Blocking callback breaks async flow | Events via `onEvent`, UI handles confirmation externally |
| `ToolCallRequest` as new type | Name implies only tool calls, but handles full LLM interaction | Renamed to `LlmExecutionRequest` |
| Many new `WorkerEvent` variants | Polluting the sealed class | Group existing events under `WorkerEvent.Tool` sealed interface |
| Confirmation flow inside orchestrator | Wrong architectural approach | Orchestrator emits events; consumer controls flow |

The core insight: **Koog uses Flow-based streaming where the consumer (UI) controls the flow, not the producer (orchestrator)**. User confirmation happens in the UI layer, not inside the orchestrator.
