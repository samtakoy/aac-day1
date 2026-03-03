package com.example.day.core.core_features.agent.domain.model

/**
 * Result of AIAgent.process() call.
 *
 * @property responseText LLM response text
 * @property reportMessage optional strategy report (e.g. compression stats), shown as info message
 * @property requestDebugInfo formatted non-history part of the LLM request (system + memory + user prompt)
 */
data class AIAgentResult(
    val responseText: String,
    val reportMessage: String?,
    val requestDebugInfo: String? = null
)
