package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.mcp.domain.McpConstants
import com.example.day.core.core_features.mcp.domain.McpFormatting
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class McpWorker @Inject constructor(
    private val mcpRepository: McpRepository,
    private val mcpTools: McpTools,
    private val chatTools: ChatTools,
    private val json: Json
) : AWorker {
    private companion object {
        const val TAG = "McpWorker(ktor)"
    }

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val parsed = parseCommand(userPrompt)
        if (parsed == null) {
            Log.w(TAG, "Invalid MCP command: $userPrompt")
            chatTools.addBotMessage(chat.id, "Формат: ${McpConstants.COMMAND_PREFIX} <tool> key=value ...")
            return
        }

        val serverId = resolveServerId()
        if (serverId == null) {
            Log.w(TAG, "No MCP server configured")
            chatTools.addBotMessage(chat.id, "MCP сервер не настроен")
            return
        }

        Log.d(TAG, "Calling MCP tool ${parsed.toolName} on server $serverId")
        chatTools.addInfoMessage(chat.id, "🔧 MCP: ${parsed.toolName}")
        val result = mcpTools.callTool(serverId, parsed.toolName, parsed.arguments)
        result.onSuccess { text ->
            val formatted = McpFormatting.formatResult(text, json)
            chatTools.addInfoMessage(chat.id, "MCP Result:\n$formatted")
        }.onFailure { error ->
            Log.e(TAG, "MCP tool failed: ${error.message}", error)
            chatTools.addBotMessage(chat.id, "Ошибка MCP: ${error.message}")
        }
    }

    private suspend fun resolveServerId(): String? {
        val servers = mcpRepository.getServers().first()
        val enabled = servers.firstOrNull { it.isEnabled }
        return enabled?.id ?: servers.firstOrNull()?.id
    }

    private fun parseCommand(raw: String): ParsedCommand? {
        val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        val toolName = parts.first()
        val args = parts.drop(1).mapNotNull { token ->
            val idx = token.indexOf('=')
            if (idx <= 0 || idx == token.length - 1) return@mapNotNull null
            val key = token.substring(0, idx)
            val value = token.substring(idx + 1)
            key to inferValue(value)
        }
        val arguments = buildJsonObject {
            args.forEach { (k, v) -> put(k, v) }
        }
        return ParsedCommand(toolName, arguments)
    }

    private fun inferValue(raw: String): JsonPrimitive {
        return when {
            raw.equals("true", ignoreCase = true) -> JsonPrimitive(true)
            raw.equals("false", ignoreCase = true) -> JsonPrimitive(false)
            raw.toIntOrNull() != null -> JsonPrimitive(raw.toInt())
            raw.toLongOrNull() != null -> JsonPrimitive(raw.toLong())
            else -> JsonPrimitive(raw)
        }
    }

    private data class ParsedCommand(
        val toolName: String,
        val arguments: JsonObject
    )
}
