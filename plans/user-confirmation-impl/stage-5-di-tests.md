# Stage 5: DI Wiring, Clean-up, and Tests

## Goal
Wire new services into DI, remove deprecated paths, and add minimal tests to validate pause/resume.

## Files to modify
- `app/src/main/java/com/example/day/core/core_features/agent/di/AgentCoreFeatureModule.kt`

## Files to add (tests)
- `app/src/test/java/.../ExecutionSessionManagerTest.kt` (if test infra exists)
- `app/src/test/java/.../ToolCallOrchestratorPauseResumeTest.kt`
- `app/src/test/java/.../ToolCallOrchestratorConcurrencyTest.kt`

## Step-by-step
1. Add DI bindings/providers for:
   - `ExecutionSessionRepository` -> `ExecutionSessionRepositoryImpl`
   - `ExecutionSessionManager` -> `ExecutionSessionManagerImpl`
   - `ConfirmationHandler` -> `DefaultConfirmationHandler`
   - `ToolCallManager` -> `ToolCallManagerImpl`

2. Update `ToolCallOrchestratorImpl` constructor parameters to include:
   - `ExecutionSessionManager`
   - `ToolCallManager`

3. Update `AIAgent` and `AIAgentFactory` DI wiring for run id provider lambda.

4. Remove outdated event types and comments:
   - remove deprecated `UserConfirmation` variants
   - remove old "informational only" comment once pause/resume is real

5. Add tests for session lifecycle:
   - `ensure` creates session
   - `update` changes status
   - `close` removes

6. Add pause/resume flow tests:
   - dangerous tool call returns paused result and emits confirmation request
   - confirm path executes tool and continues loop
   - reject path emits denied tool result and continues loop

7. Add concurrency/idempotency tests:
   - duplicate confirm for same `confirmationId` is no-op success
   - stale `confirmationId` is rejected
   - one pending confirmation per `runId` invariant is preserved

8. Quick manual smoke check in UI for all enabled worker modes:
   - trigger dangerous tool call
   - verify dialog appears
   - confirm and ensure tool executes and LLM continues

9. Add explicit TODO comments in in-memory session repository about planned Room migration.

## Notes
- If test infra is not ready, log a TODO in `plans/` with test cases and expected behavior.

## Previous
See stage-4-agent-worker-ui.md.
