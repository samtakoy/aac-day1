package com.example.day.core.core_features.agent.domain.tools

/**
 * Normalized tool execution outcome.
 */
sealed class ToolExecutionResult {
    data class Success(val payload: String) : ToolExecutionResult()
    data class Denied(val reason: String) : ToolExecutionResult()
    data class Failed(val error: String) : ToolExecutionResult()
}
