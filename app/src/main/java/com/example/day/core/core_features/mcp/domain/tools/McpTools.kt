package com.example.day.core.core_features.mcp.domain.tools

import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.serialization.json.JsonObject

interface McpTools {
    suspend fun callTool(
        serverId: String,
        toolName: String,
        arguments: JsonObject,
        context: McpToolCallContext? = null
    ): Result<String>

    suspend fun listTools(serverId: String): Result<List<String>>
}
