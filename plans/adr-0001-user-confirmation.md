# ADR-0001: User Confirmation for Dangerous Tools via ExecutionSession

## Status
Accepted

## Context
The system must support user confirmation for dangerous tools in request-response mode. The LLM loop is driven by `AIAgent.process()` which is intentionally stateless. Previous approaches risked making `AIAgent` or `Orchestrator` stateful, or splitting tool execution across UI/orchestrator layers, which reduced clarity and increased branching.

We need:
- Safe confirmation before dangerous tool execution
- Return tool outcomes back into the LLM loop
- Preserve clean separation of responsibilities
- Maintain extensibility for future tools/policies

## Decision
Adopt a session-based execution model and treat confirmation as a tool-execution policy.

Key decisions:
- Introduce **ExecutionSession** as the only stateful unit (run/session scope, tied to `runId` and typically `chatId`/`agentId`).
- Keep `AIAgent.process()` stateless; it only performs a single LLM call.
- Keep `Orchestrator` as a **strategy runner** that reads/writes session state and executes effects; it holds no internal state.
- Centralize tool execution in `ToolCallManager`.
- Add a **ConfirmationHandler** (policy) around tool execution to decide whether a tool needs user approval.
- Normalize tool outcomes as `ToolExecutionResult` with `Success`, `Denied`, `Failed`.
- Treat user rejection as `Denied` and feed it back into the LLM loop like any other tool result.

## Consequences
- Single source of state: `ExecutionSession` enables pause/resume between UI requests.
- Confirmation is a policy layer, not a special-case branch in LLM logic.
- The LLM loop remains uniform: tool results (including denial) are always injected into the next request.
- Clear separation of responsibilities improves maintainability and extensibility.

## Alternatives Considered
- **Stateful AIAgent**: rejected, violates stateless design and complicates testing.
- **Stateful Orchestrator**: rejected, blurs strategy vs state responsibilities.
- **ToolCallManager invoking LLM directly**: rejected, breaks separation of concerns.
- **Callback/continuation approach**: rejected, increases API complexity and coupling.

## Notes
This ADR aligns with the Koog-inspired strategy loop and session-scoped state model while remaining fully internal to the Day architecture.
