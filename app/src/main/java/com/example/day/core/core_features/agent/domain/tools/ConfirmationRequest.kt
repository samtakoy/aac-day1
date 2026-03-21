package com.example.day.core.core_features.agent.domain.tools

/**
 * UI-facing confirmation payload for dangerous tool calls.
 */
data class ConfirmationRequest(
    val confirmationId: String,
    val title: String,
    val message: String,
    val actionLabel: String
)
