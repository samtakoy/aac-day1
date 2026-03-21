package com.example.day.core.core_features.agent.domain.tools

/**
 * Tool call waiting for user confirmation.
 */
data class PendingToolCall(
    val toolCallId: String,
    val toolName: String,
    val arguments: String,
    val type: String,
    val confirmationId: String
)
