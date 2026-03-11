package com.example.day.core.core_features.mcp.data.local.inmemory

import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.serialization.json.JsonObject

internal interface LocalMcpTool {
    val name: String
    val description: String
    val inputSchema: JsonObject

    suspend fun call(arguments: JsonObject, context: McpToolCallContext?): Result<String>
}
