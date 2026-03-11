package com.example.day.core.core_features.mcp.data.local.inmemory

import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalMcpService @Inject constructor(
    private val setReminderTool: SetReminderTool
) {
    private val tools: List<LocalMcpTool> = listOf(setReminderTool)

    fun listTools(): List<LocalMcpTool> = tools

    suspend fun callTool(
        name: String,
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String> {
        val tool = tools.firstOrNull { it.name == name }
            ?: return Result.failure(IllegalArgumentException("Tool not found: $name"))
        return tool.call(arguments, context)
    }
}
