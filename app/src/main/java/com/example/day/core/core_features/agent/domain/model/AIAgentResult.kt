package com.example.day.core.core_features.agent.domain.model

/**
 * Result of AIAgent.process() call.
 *
 * @property responseText LLM response text
 * @property reportMessage optional strategy report (e.g. compression stats), shown as info message
 */
data class AIAgentResult(
    val responseText: String,
    val reportMessage: String?
)
