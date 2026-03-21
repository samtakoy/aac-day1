package com.example.day.core.core_features.agent.domain.model

/**
 * Result of AIAgent.process() call.
 */
data class AIAgentResult(
    val responseText: String,
    val reportMessage: String?,
    val requestDebugInfo: String? = null,
    val toolLoop: Boolean = false,
    val isPaused: Boolean = false,
    val pendingConfirmationId: String? = null,
    val runId: String? = null
)
