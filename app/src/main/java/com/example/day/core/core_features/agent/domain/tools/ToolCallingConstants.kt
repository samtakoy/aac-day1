package com.example.day.core.core_features.agent.domain.tools

object ToolCallingConstants {
    const val MAX_TOOL_LOOPS = 5
    const val MCP_NOT_CONFIGURED = "MCP server is not configured"
    const val MCP_TOOL_ERROR_PREFIX = "MCP tool error"
    const val UNKNOWN_TOOL_ERROR = "Unknown error"
    const val TOOL_NOT_ALLOWED_PREFIX = "Tool not allowed"
    const val INVALID_TOOL_ARGUMENTS = "Invalid tool arguments"
    const val TOOL_EVENT_START_PREFIX = "MCP tool"
    const val TOOL_EVENT_RESULT_PREFIX = "MCP result"
    const val CONFIRMATION_TITLE = "Confirm tool execution"
    const val CONFIRMATION_MESSAGE_PREFIX = "Execute tool"
    const val WAITING_CONFIRMATION_MESSAGE = "Waiting for user confirmation before continuing."
    const val CONFIRMATION_REQUIRED_PREFIX = "User confirmation required for run"

    val DANGEROUS_TOOL_KEYWORDS = listOf(
        "delete",
        "remove",
        "rm",
        "drop",
        "truncate",
        "write",
        "overwrite",
        "exec",
        "shell"
    )
}
