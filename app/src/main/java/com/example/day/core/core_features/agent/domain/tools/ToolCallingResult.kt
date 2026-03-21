package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage

/**
 * Result of tool calling cycle execution.
 */
data class ToolCallingResult(
    val finalResponseText: String,
    val toolCallSessions: List<ToolCallSession>,
    val allMessages: List<AContextMessage>,
    val isPaused: Boolean = false,
    val pendingConfirmationId: String? = null,
    val runId: String? = null
)
