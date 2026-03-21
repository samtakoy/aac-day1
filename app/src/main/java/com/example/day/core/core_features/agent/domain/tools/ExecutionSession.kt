package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest

/**
 * Execution state for a single run.
 *
 * This is intentionally separate from the long-lived context managed by ContextStrategy.
 */
data class ExecutionSession(
    val runId: String,
    val status: ExecutionSessionStatus = ExecutionSessionStatus.Running,
    val requestSnapshot: LlmExecutionRequest,
    val pendingToolCall: PendingToolCall? = null,
    val toolResultsBuffer: List<ToolExecutionResult> = emptyList(),
    val llmMessages: List<ModelRequest.Message> = emptyList(),
    val newMessages: List<ModelRequest.Message> = emptyList(),
    val toolCallSessions: List<ToolCallSession> = emptyList(),
    val currentAssistantToolCall: AssistantToolCall? = null,
    val currentToolResults: List<ToolResult> = emptyList(),
    val currentToolIndex: Int = 0,
    val finalResponseText: String = "",
    val resolvedConfirmationId: String? = null
)
