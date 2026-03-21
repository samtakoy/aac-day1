# Stage 4: Agent + Worker + UI Resume Flow

## Goal
Wire pause/resume into `AIAgent`, workers, and UI actions. Ensure user confirmation resumes the LLM loop.

## Files modified
- `app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgentFactory.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/model/AIAgentResult.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/WorkerEvent.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TaskWorker.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TalkWorker.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/JustWorkWorker.kt`
- `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/PlannerWorker.kt`
- `app/src/main/java/com/example/day/features/console/impl/domain/agents/AgMessageHandler.kt`
- `app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`
- `app/src/main/java/com/example/day/features/console/impl/ui/delegates/TalkDelegate.kt`
- `app/src/main/java/com/example/day/features/console/impl/ui/delegates/PlannerTalkDelegate.kt`
- `app/src/main/java/com/example/day/features/console/impl/ui/delegates/AgentsTalkDelegate.kt`

## Step-by-step
- [x] Introduce run id provider in `AIAgent` constructor.
- [x] Wire `runIdProvider` from `AIAgentFactory` with `${agentId}:${uuid}` policy.
- [x] In `AIAgent.process()` create/ensure run session before orchestrator start.
- [x] Add explicit `resume(runId, confirmationId, approved, onEvent)` entrypoint in `AIAgent`.
- [x] Extend `AIAgentResult` with pause/resume fields.
- [x] Replace `WorkerEvent.UserConfirmation` variants with one unified `Requested` event.
- [x] Extend `TalkDelegate` with confirmation API.
- [x] Wire `ConsoleViewModelImpl` confirm/reject actions through `tryHandleConfirmation(...)`.
- [x] Map confirmation events to UI in `PlannerTalkDelegate`.
- [x] Apply pause-safe processing and explicit resume support to `TaskWorker`, `TalkWorker`, `JustWorkWorker`, and `PlannerWorker`.
- [x] Keep rejection reason optional in UI for now.

## Notes
- Agent-mode console flow now also forwards confirmation requests through `AgentsTalkDelegate` and `AgMessageHandler`.
- `JustWorkWorker` exposes `resume(...)`, but still surfaces paused runs as failure because it has no built-in interactive UI layer.
- Reminder/background callers can now avoid silent empty responses because paused `TalkWorker` runs write an info message to chat.

## Next
See `stage-5-di-tests.md`.

## Previous
See `stage-3-session-manager-orchestrator.md`.
