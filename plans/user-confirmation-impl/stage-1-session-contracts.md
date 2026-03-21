# Stage 1: Session and Result Contracts (Domain + Repo)

## Goal
Introduce the session-scoped state and normalized tool execution outcomes. Provide a repository to persist sessions between requests.

## Files to add
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ExecutionSession.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ExecutionSessionStatus.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolExecutionResult.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/PendingToolCall.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ConfirmationRequest.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ExecutionSessionRepository.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/data/ExecutionSessionRepositoryImpl.kt`

## Step-by-step
1. Create `ExecutionSessionStatus` as an enum with values:
   - `Running`
   - `WaitingUserConfirmation`
   - `Completed`
   - `Failed`
   - `Canceled`

2. Create `ToolExecutionResult` sealed class with:
   - `Success(payload: String)`
   - `Denied(reason: String)`
   - `Failed(error: String)`

3. Create `PendingToolCall` data class with:
   - `toolCallId: String`
   - `toolName: String`
   - `arguments: String`
   - `confirmationId: String`

4. Create `ConfirmationRequest` data class with:
   - `confirmationId: String`
   - `title: String`
   - `message: String`
   - `actionLabel: String`

5. Create `ExecutionSession` data class containing:
   - `runId: String`
   - `status: ExecutionSessionStatus`
   - `requestSnapshot: LlmExecutionRequest`
   - `pendingToolCall: PendingToolCall?`
   - `toolResultsBuffer: List<ToolExecutionResult>`

6. Create `ExecutionSessionRepository` interface with:
   - `suspend fun get(runId: String): ExecutionSession?`
   - `suspend fun save(session: ExecutionSession)`
   - `suspend fun remove(runId: String)`

7. Implement `ExecutionSessionRepositoryImpl` as an in-memory map:
   - Use `MutableMap<String, ExecutionSession>`.
   - Use synchronization if needed, or keep it simple for now.
   - Add explicit code TODO comments about migration to Room for process-death resilience.

8. Add basic unit tests if you already have a test setup for repositories. Otherwise skip and do it in Stage 5.

## Notes
- This stage is only contract + storage; no wiring and no behavior changes yet.
- Keep models in domain layer and repo impl in data layer.
- `ExecutionSession` represents execution state (pause/resume), not long-term agent history. `ContextStrategy` remains unchanged and continues to own the agent persisted context.

## Next
See stage-2-confirmation-toolcall-manager.md.
