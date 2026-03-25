# Architecture Review: Moving Agent System to ai-gateway

## Current State Analysis

### AIAgent.kt (Android)

**Core Responsibilities:**
- LLM context building and management (memory + prompt messages)
- Tool call orchestration loop (max 10 iterations)
- HITL (Human-In-The-Loop) session management for tool approval
- Context strategy processing (Summarization, SlidingWindow, etc.)

**Dependencies:**
| Dependency | Type | Can Migrate? |
|------------|------|--------------|
| `AgentContextRepository` | Room/DB | ❌ Android-specific |
| `ContextStrategy` | Domain | ⚠️ Needs extraction |
| `MemoryProvider` | Domain | ⚠️ Abstract first |
| `ToolProvider` | Domain | ⚠️ Abstract first |
| `ToolCallOrchestrator` | Domain | ⚠️ Abstract first |
| `ToolExecutor` | Domain | ⚠️ Abstract first |
| `HitlSessionManager` | Domain | ⚠️ Abstract first |

### AWorker.kt (Android)

```kotlin
interface AWorker {
    suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role = AContextMessage.Role.USER,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    )
}
```

**Issues for Migration:**
- `Chat` domain model is Android/Room specific
- `AContextMessage` is Android-specific
- `WorkerEvent` is Android-specific

### AiGatewayServer.kt (Current)

Ktor server with single `/v1/chat/completions` endpoint using Ollama.

---

## Proposed Architecture: Remote Agent System

### High-Level Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android App                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │   Workers   │───▶│ AgentClient │───▶│ WebSocket + REST    │  │
│  │  (thin UI)  │    │   (DTOs)    │    │  (Server-Sent Evts) │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/WS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     ai-gateway Server                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ AgentRoutes │───▶│ AIAgent     │───▶│ LlmProvider         │  │
│  │ (Ktor)      │    │ (Server)    │    │ (Ollama/OpenAI/...) │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
│                              │                                    │
│                              ▼                                    │
│                      ┌─────────────┐                             │
│                      │ Tool System │ (Server-side tools)         │
│                      └─────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Architecture Components

### 1. Shared Module Structure

```
shared/
├── simple-chat-api/           # Existing DTOs
│   └── dto/
│       └── OpenAiDtos.kt     # ChatCompletionRequest/Response
│
└── agent-api/                 # NEW: Agent system API
    └── src/main/kotlin/com/example/day/shared/agent/
        ├── api/
        │   ├── AgentRequest.kt
        │   ├── AgentResponse.kt
        │   └── WorkerEventDto.kt
        ├── model/
        │   ├── AgentConfigDto.kt
        │   ├── ContextMessageDto.kt
        │   └── ToolCallDto.kt
        └── tools/
            └── ToolDefinitionDto.kt
```

### 2. New DTOs for Agent API

```kotlin
// AgentRequest.kt
@Serializable
data class AgentProcessRequest(
    val agentId: String,
    val prompt: ContextMessageDto,
    val sessionId: String? = null  // For HITL resumption
)

@Serializable  
data class AgentWorkerRequest(
    val workerType: String,  // "simple", "step", "team", "prompt", etc.
    val userPrompt: String,
    val chatContext: ChatContextDto,
    val config: AgentConfigDto
)

// WorkerEventDto.kt - Server → Client events
@Serializable
sealed class WorkerEventDto {
    @SerialName("request_start")
    object RequestStart : WorkerEventDto()
    
    @SerialName("request_success") 
    data class RequestSuccess(val text: String, val toolCalls: List<ToolCallDto>?) : WorkerEventDto()
    
    @SerialName("request_error")
    data class RequestError(val error: String) : WorkerEventDto()
    
    @SerialName("tool_call_started")
    data class ToolCallStarted(val toolCallId: String, val toolName: String, val arguments: String) : WorkerEventDto()
    
    @SerialName("tool_call_finished")
    data class ToolCallFinished(val toolCallId: String, val toolName: String, val result: String, val isError: Boolean) : WorkerEventDto()
    
    @SerialName("approval_required")
    data class ApprovalRequired(val runId: String, val toolCallId: String, val toolName: String, val arguments: String) : WorkerEventDto()
}

// AgentResponse.kt
@Serializable
data class AgentProcessResponse(
    val runId: String,
    val status: AgentStatus,
    val result: String? = null,
    val reportMessage: String? = null,
    val debugInfo: String? = null
)

@Serializable
enum class AgentStatus {
    COMPLETED,
    PENDING_APPROVAL,
    ERROR
}
```

### 3. ai-gateway Server Extensions

```
ai-gateway/src/main/kotlin/com/example/day/aigateway/
├── AgentServer.kt                          # Main entry point
├── api/
│   └── routes/
│       ├── AgentRoutes.kt                  # NEW: Agent REST endpoints
│       └── ChatRoutes.kt                   # Existing
├── agent/                                  # NEW: Agent domain (server-side)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ServerAgentConfig.kt
│   │   │   ├── ServerContextMessage.kt
│   │   │   └── ServerTool.kt
│   │   ├── AIAgentServer.kt               # Adapted from AIAgent.kt
│   │   ├── AgentContextRepository.kt       # Interface (server impl)
│   │   ├── ContextStrategy.kt              # Interface (can reuse Android impl)
│   │   └── workers/
│   │       ├── base/
│   │       │   ├── AWorker.kt             # Interface (adapted)
│   │       │   └── WorkerEvent.kt          # Server-side events
│   │       └── concrete/
│   │           ├── SimpleWorker.kt
│   │           ├── StepWorker.kt
│   │           └── TeamWorker.kt
│   ├── infrastructure/
│   │   ├── repository/
│   │   │   └── InMemoryAgentContextRepository.kt
│   │   └── tools/
│   │       ├── ServerToolProvider.kt
│   │       ├── ServerToolExecutor.kt
│   │       └── ServerToolCallOrchestrator.kt
│   └── di/
│       └── AgentServerModule.kt
├── llm/
│   └── LlmProvider.kt                      # Existing (reused)
└── websocket/
    └── AgentWebSocketServer.kt             # NEW: HITL WebSocket handler
```

### 4. AIAgentServer (Adapted from AIAgent.kt)

Key changes from Android AIAgent:

```kotlin
// Differences from AIAgent.kt:
// 1. No Android dependencies (Chat, Room, etc.)
// 2. Uses server-side implementations of repositories
// 3. Emits WorkerEventDto instead of WorkerEvent
// 4. HITL handled via WebSocket

class AIAgentServer(
    val config: ServerAgentConfig,
    private val contextRepository: AgentContextRepository,
    private val strategy: ContextStrategy,
    private val memoryProvider: ServerMemoryProvider,
    private val toolProvider: ServerToolProvider,
    private val orchestrator: ServerToolCallOrchestrator,
    private val toolExecutor: ServerToolExecutor,
    private val hitlSessionManager: ServerHitlSessionManager
) {
    // Core logic similar to AIAgent.process() and runToolLoop()
    // But adapted for server context
}
```

### 5. Worker Interface (Server-side)

```kotlin
// AWorker.kt (Server version)
interface AWorker {
    suspend fun doWork(
        userPrompt: String,
        context: WorkerContext,
        onEvent: (suspend (WorkerEventDto) -> Unit)? = null
    ): WorkerResult
}

data class WorkerContext(
    val sessionId: String,
    val agentConfig: ServerAgentConfig,
    val history: List<ServerContextMessage>,
    val metadata: Map<String, String>
)

sealed class WorkerResult {
    data class Success(val response: AgentProcessResponse) : WorkerResult()
    data class Error(val message: String) : WorkerResult()
}
```

---

## Migration Strategy

### Phase 1: Shared API Extraction
1. Create `shared/agent-api` module
2. Extract DTOs for agent communication
3. Define interfaces that both Android and server will implement

### Phase 2: Server Infrastructure
1. Create `ServerAgentConfig`, `ServerContextMessage`, `ServerTool` models
2. Implement `AgentContextRepository` (in-memory for MVP)
3. Adapt `ContextStrategy` interfaces
4. Create server-side `ToolProvider`, `ToolExecutor`, `ToolCallOrchestrator`

### Phase 3: AIAgentServer Implementation
1. Copy and adapt `AIAgent.kt` → `AIAgentServer.kt`
2. Replace Android-specific types with server equivalents
3. Implement HITL via WebSocket session management

### Phase 4: Worker Migration
1. Adapt `AWorker.kt` interface for server
2. Migrate workers that make sense server-side:
   - `SimpleWorker` ✅
   - `StepWorker` ✅
   - `TeamWorker` ✅
   - `PromptWorker` ✅
3. Mark Android-specific workers (RAG, file operations) as NOT migratable

### Phase 5: Android Client Adaptation
1. Create `AgentClient` that calls server REST API
2. Wrap server events via WebSocket
3. Keep UI-layer workers on Android (thin wrappers)

---

## Key Interfaces to Extract

```kotlin
// In shared/agent-api/

interface AgentRuntimeContext {
    val agentId: String
    val sessionId: String
    val history: List<ContextMessage>
    val metadata: Map<String, String>
}

interface AgentContextRepository {
    suspend fun loadContext(agentId: String): AgentContext?
    suspend fun saveContext(agentId: String, context: AgentContext)
    suspend fun clearContext(agentId: String)
}

interface ContextStrategy {
    suspend fun process(config: AgentRuntimeConfig, repository: AgentContextRepository): ContextStrategyResult
    suspend fun afterResponse(config: AgentRuntimeConfig, response: String, repository: AgentContextRepository, fullContext: ContextStrategyResult): ContextStrategyResult
}

interface ToolProvider {
    fun getTools(agentId: String): List<Tool>
    suspend fun executeToolCall(call: ToolCall, context: ToolCallContext): Result<String>
}

interface HitlSessionManager {
    fun hasActiveSession(agentId: String): Boolean
    fun createSession(runId: String, agentId: String, prompt: ContextMessage, loopMessages: List<Message>): HitlSession
    fun getSession(runId: String): HitlSession?
    fun closeSession(runId: String)
    fun updateSession(runId: String, decisions: Map<String, ToolCallDecision>)
}
```

---

## What Stays on Android

The following cannot/be should not be migrated to server:

| Component | Reason |
|-----------|--------|
| Room Database | Android local storage |
| `RagWorker` | RAG relies on local document index |
| File system tools | Server has no access to Android FS |
| Notification tools | Android-specific |
| UI state management | Already on Android |

---

## API Contract

### REST Endpoints

```
POST /v1/agents/process
  Request:  AgentProcessRequest
  Response: AgentProcessResponse
  → For simple agent invocations

POST /v1/agents/{agentId}/resume
  Request:  { runId: String, decisions: Map<String, "APPROVED"|"REJECTED"> }
  Response: AgentProcessResponse
  → For HITL resumption after user approval

GET /v1/agents/{agentId}/context
  Response: AgentContext
  → Get current agent context

DELETE /v1/agents/{agentId}/context
  Response: 204 No Content
  → Clear agent context
```

### WebSocket Endpoint

```
WS /v1/agents/events?session={sessionId}
  
  Server → Client Events:
    - WorkerEventDto (all types)
    - Heartbeat
  
  Client → Server Messages:
    - Approval/Rejection decisions
```

---

## HITL Flow with WebSocket

```
1. Client sends POST /v1/agents/process
2. Server starts agent processing
3. If tool needs approval:
   - Server sends WorkerEventDto.ApprovalRequired via WS
   - Server returns 202 Accepted with runId
4. Client shows approval UI to user
5. User approves/rejects
6. Client sends decision via WS or POST /v1/agents/{agentId}/resume
7. Server resumes processing
8. Server sends final response via WS + HTTP response
```

---

## Benefits of This Architecture

1. **Single Source of Truth**: Agent logic runs on server, no sync issues
2. **Resource Savings**: Heavy LLM processing offloaded from Android
3. **Consistent Tool Execution**: Tools run in consistent server environment
4. **Real HITL**: True WebSocket-based human approval loop
5. **Extensibility**: Easy to add new workers and tools on server
6. **Testability**: Server code is easier to unit test

---

## Files to Create/Modify

### New Files

| File | Purpose |
|------|---------|
| `shared/agent-api/build.gradle.kts` | New shared module |
| `shared/agent-api/src/main/kotlin/.../dto/*.kt` | Shared DTOs |
| `ai-gateway/src/main/kotlin/.../agent/domain/*.kt` | Server agent domain |
| `ai-gateway/src/main/kotlin/.../agent/infrastructure/*.kt` | Server implementations |
| `ai-gateway/src/main/kotlin/.../websocket/*.kt` | WebSocket support |
| `ai-gateway/src/main/kotlin/.../api/routes/AgentRoutes.kt` | Agent REST endpoints |

### Modified Files

| File | Change |
|------|--------|
| `settings.gradle.kts` | Include new shared module |
| `ai-gateway/build.gradle.kts` | Add shared module dependency |
| `ai-gateway/src/main/kotlin/.../AiGatewayServer.kt` | Wire in AgentRoutes |
| `app/build.gradle.kts` | Add shared module, remove agent dependencies (optional) |

---

## Decision Points

1. **Database**: Use in-memory for MVP, or add PostgreSQL/MongoDB later?
2. **Auth**: How should Android authenticate with ai-gateway? (API key, JWT, etc.)
3. **Scaling**: Should agent sessions be sticky to specific server instances?
4. **Backward Compatibility**: Keep Android AIAgent for offline mode?
