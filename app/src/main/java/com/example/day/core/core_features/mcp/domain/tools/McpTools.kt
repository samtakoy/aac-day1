package com.example.day.core.core_features.mcp.domain.tools

import kotlinx.serialization.json.JsonObject

interface McpTools {
    suspend fun callTool(
        serverId: String,
        toolName: String,
        arguments: JsonObject
    ): Result<String>

    suspend fun listTools(serverId: String): Result<List<String>>
}
