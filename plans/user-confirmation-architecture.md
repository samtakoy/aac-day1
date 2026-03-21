# User Confirmation Architecture (Request-Response Mode) — v2

## Goal
Safely confirm dangerous tools in request-response mode **without** making `AIAgent` or `Orchestrator` stateful. State lives in an **ExecutionSession** (run/session scope), and every tool outcome is returned back into the LLM loop.

## Core Principles
- `AIAgent.process()` remains stateless.
- Orchestration is a **strategy loop** (LLM > tools > results > LLM) and is **pure** (state stored externally).
- **Tool execution** is centralized in `ToolCallManager`.
- **Confirmation** is a policy/handler around tool execution (not in LLM logic).
- **User decision** is treated as a normal tool result (`Denied`), so the LLM can continue.

---

## ExecutionSession (stateful run, not agent)
A session is the only stateful unit. It survives between UI requests and is tied to `runId` (and usually `chatId` / `agentId`).

Suggested conceptual fields:
- `runId`
- `status`: Running | WaitingUserConfirmation | Completed | Failed | Canceled
- `requestSnapshot`: last `LlmExecutionRequest` (or minimal serializable slice)
- `pendingToolCall`: current tool awaiting confirmation
- `toolResultsBuffer`: results to inject into next LLM request
- `lastAssistantMessage` (optional UI recovery)

---

## Event Model (inputs to strategy)
Events drive the state machine and generate **effects**.

LLM Events:
- `LlmResponseReceived(response)`

Tool Events:
- `ToolCallDetected(toolCall)`
- `ToolExecuted(result)`

User Events:
- `UserConfirmationRequested(confirmationId, toolCall)`
- `UserConfirmed(confirmationId)`
- `UserRejected(confirmationId, reason?)`

System Events:
- `Timeout(confirmationId)`
- `SessionCanceled`
- `SessionFailed`

### Effects (what Orchestrator executes)
- `InvokeLlm(nextRequest)`
- `RequestUserConfirmation(toolCall, confirmationId)`
- `ExecuteTool(toolCall)`
- `EmitUiMessage(...)`

---

## Tool Execution Policy (ConfirmationHandler)
Dangerous tools are intercepted **before execution** by a confirmation policy.

### ToolExecutionResult
All outcomes are normalized:
- `Success(payload)`
- `Denied(reason)`
- `Failed(error)`

If the user rejects, we still return `Denied` to LLM. No special branches needed.

---

## Strategy Flow (request-response)
1. `InvokeLlm` with current request.
2. LLM returns tool calls.
3. For each tool call:
   - If safe > `ExecuteTool` immediately.
   - If dangerous > `RequestUserConfirmation` and **pause** session.
4. UI prompts user.
5. User responds:
   - Confirm > `ExecuteTool`.
   - Reject > create `Denied` result.
6. `ToolExecuted(result)` > buffer result > `InvokeLlm` with updated request.
7. Finish when no tool calls remain.

---

## Responsibilities (clean separation)
- **AIAgent**: stateless LLM call.
- **Orchestrator**: strategy runner + effects executor (no internal state).
- **ExecutionSession**: persistent run state.
- **ToolCallManager**: executes tools.
- **ConfirmationHandler**: determines if tool needs approval and handles confirm/deny.
- **UI**: only renders confirmation and emits user decision event.

---

## Why this is simpler
- One stateful object (`ExecutionSession`) instead of stateful agent/orchestrator.
- Confirmation is just a policy for tool execution.
- Denial is just another tool result > no extra branches in LLM loop.
- Extensible: new confirmation policies, new tool types, multi-step flows.

---

## Mapping to Existing Files (informational)
- `ToolCallManager` owns execution
- `ExecutionSession` lives in agent domain/state
- Orchestrator implements strategy loop
- UI consumes `UserConfirmationRequested` and emits `UserConfirmed/Rejected`

No code changes in this document. It is an architecture-only spec.

---

## ADR
See plans/adr-0001-user-confirmation.md.
