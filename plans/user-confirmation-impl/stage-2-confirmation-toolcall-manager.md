# Stage 2: Confirmation Policy and ToolCallManager

## Goal
Centralize tool execution and introduce a confirmation policy that can block dangerous tools before execution.

## Files to add
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ConfirmationHandler.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/data/tools/DefaultConfirmationHandler.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallManager.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/data/tools/ToolCallManagerImpl.kt`

## Files to modify
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallingConstants.kt`

## Step-by-step
1. Create `ConfirmationHandler` interface with:
   - `fun requiresConfirmation(toolName: String, arguments: String): Boolean`
   - `fun buildRequest(toolName: String, arguments: String, confirmationId: String): ConfirmationRequest`

2. Implement `DefaultConfirmationHandler` with a conservative policy:
   - Detect dangerous verbs in toolName (delete, rm, remove, drop, truncate, write, overwrite, exec, shell, etc.)
   - Optionally check arguments size or paths.
   - Keep policy constants in a constants object (no magic values in code).

3. Create `ToolCallManager` interface with:
   - `suspend fun handleToolCall(call: ModelResult.Success.ToolCall, context: ToolCallContext): ToolCallManagerResult`

4. Create `ToolCallManagerResult` sealed class:
   - `Executed(result: ToolExecutionResult)`
   - `ConfirmationRequired(request: ConfirmationRequest, pending: PendingToolCall)`

5. Implement `ToolCallManagerImpl`:
   - If confirmation required:
     - generate `confirmationId` (UUID)
     - build `ConfirmationRequest`
     - return `ConfirmationRequired`
   - If safe:
     - call `ToolRegistry.executeToolCall(...)`
     - map success to `ToolExecutionResult.Success`
     - map error to `ToolExecutionResult.Failed`

6. Ensure tool execution results are normalized and never returned as raw strings.

## Notes
- No changes to Orchestrator yet. That happens in Stage 3.
- Keep `ToolCallManager` as the single point of tool execution.

## Next
See stage-3-session-manager-orchestrator.md.

## Previous
See stage-1-session-contracts.md.
