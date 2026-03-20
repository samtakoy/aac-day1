package com.example.day.core.core_features.agent.domain.tools

/**
 * Context for tool calling operations.
 * Contains agent ID and a map of tool names to their server IDs for routing.
 */
data class ToolCallContext(
    val agentId: Long,
    /**
     * Map of tool names to server IDs.
     * This is populated by ToolProvider.getTools() and used to route
     * tool calls to the correct MCP server.
     * Format: "toolName" -> "serverId" or "serverId:toolName" -> "serverId"
     */
    val toolToServer: Map<String, String> = emptyMap()
) {
    companion object {
        const val NAMESPACE_SEPARATOR = ":"
    }
}
