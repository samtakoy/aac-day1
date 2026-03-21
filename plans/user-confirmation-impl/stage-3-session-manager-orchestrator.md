# Stage 3: Session Manager and Orchestrator Refactor (Variant B)

## Goal
Move session creation out of `ToolCallOrchestratorImpl` and refactor the orchestrator into a pure strategy runner that operates on sessions.

## Files to add
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ExecutionSessionManager.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/data/ExecutionSessionManagerImpl.kt`

## Files to modify
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallOrchestrator.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallingResult.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallSession.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/WorkerEvent.kt`

## Step-by-step
1. Create `ExecutionSessionManager` interface:
   - `suspend fun ensure(runId: String, request: LlmExecutionRequest): ExecutionSession`
   - `suspend fun get(runId: String): ExecutionSession?`
   - `suspend fun update(session: ExecutionSession)`
   - `suspend fun close(runId: String)`

2. Implement `ExecutionSessionManagerImpl` using `ExecutionSessionRepository`.
   - `ensure` creates new session if missing.
   - `update` persists session.

3. Update `ToolCallOrchestrator` contract to include explicit start and resume paths:
   - `suspend fun execute(request: LlmExecutionRequest, runId: String, onEvent: ...): Result<ToolCallingResult>`
   - `suspend fun resume(runId: String, confirmationId: String, approved: Boolean, onEvent: ...): Result<ToolCallingResult>`

4. Refactor `ToolCallOrchestratorImpl` to:
   - NOT create sessions internally.
   - Assume `ExecutionSessionManager.ensure(...)` was already called by the caller for `execute(...)`.
   - Load session using `ExecutionSessionManager.get(runId)`.
   - Run loop until:
     - final response (`Completed`), or
     - confirmation required (`WaitingUserConfirmation`)

5. On confirmation-required branch:
   - save session with `pendingToolCall`
   - emit unified event `WorkerEvent.UserConfirmation.Requested`
   - include at least: `runId`, `confirmationId`, `title`, `message`, `actionLabel`
   - return paused `ToolCallingResult`

6. Implement `resume(...)` behavior:
   - validate session exists and has `pendingToolCall`
   - reject stale `confirmationId`
   - if approved: execute pending tool and continue loop
   - if rejected: create `ToolExecutionResult.Denied` and continue loop

7. Extend `ToolCallingResult` to include:
   - `isPaused: Boolean`
   - `pendingConfirmationId: String?`
   - optional `runId: String` for easier caller tracing

8. Keep `ToolCallSession` updated with tool outcomes.
   - for `Denied`, use `isError = false` and denial reason as content.

9. Apply strict minimal concurrency rules:
   - one pending confirmation per `runId`
   - repeated same decision for same `confirmationId` is idempotent (no-op success)
   - stale or mismatched `confirmationId` returns failure

## Notes
- This stage makes Orchestrator depend on `ExecutionSessionManager` and `ToolCallManager`.
- It does not change UI or `AIAgent` yet.

## Next
See stage-4-agent-worker-ui.md.

## Previous
See stage-2-confirmation-toolcall-manager.md.
