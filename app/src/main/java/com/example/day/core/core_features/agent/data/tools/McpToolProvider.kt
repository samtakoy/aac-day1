package com.example.day.core.core_features.agent.data.tools

import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.model.McpTool
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import com.example.day.core.core_features.memory.domain.provider.AgentToolsMemoryProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class McpToolProvider @Inject constructor(
    private val repository: McpRepository,
    private val mcpTools: McpTools,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json
) : ToolProvider {

    private companion object {
        private const val TAG = "McpToolProvider"
    }

    private val toolToServer = ConcurrentHashMap<String, String>()

    override suspend fun getTools(agentId: Long?): List<ModelRequest.Tool> {
        android.util.Log.d(TAG, "getTools: start agentId=$agentId")
        val servers = getEnabledServers()
        if (servers.isEmpty()) {
            android.util.Log.w(TAG, "getTools: no servers → returning empty")
            return emptyList()
        }
        android.util.Log.d(TAG, "getTools: servers=${servers.map { "${it.id}(enabled=${it.isEnabled})" }}")

        toolToServer.clear()

        val allowedTools = agentId?.let { getAllowedTools(it) }
        android.util.Log.d(TAG, "getTools: agentAllowedTools=$allowedTools")

        val collected = mutableListOf<ModelRequest.Tool>()
        servers.forEach { server ->
            val tools = getConnectedTools(server.id)
            android.util.Log.d(TAG, "getTools: server=${server.id} connectedTools=${tools.map { it.name }}")
            tools.forEach { tool ->
                // Проверка 1: tool в глобальном списке разрешенных
                if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(tool.name)) {
                    android.util.Log.v(TAG, "getTools: skip '${tool.name}' — not in ALLOWED_TOOL_NAMES")
                    return@forEach
                }
                // Проверка 2: tool в списке разрешенных для агента (если задан)
                if (allowedTools != null && !allowedTools.contains(tool.name)) {
                    android.util.Log.v(TAG, "getTools: skip '${tool.name}' — not in agent allowedTools")
                    return@forEach
                }

                if (toolToServer.containsKey(tool.name)) return@forEach
                toolToServer[tool.name] = server.id
                collected.add(
                    ModelRequest.Tool(
                        type = ModelRequest.ToolType.Function,
                        function = ModelRequest.Function(
                            name = tool.name,
                            description = tool.description,
                            parameters = parseSchema(tool)
                        )
                    )
                )
            }
        }
        android.util.Log.d(TAG, "getTools: returning ${collected.size} tools: ${collected.map { it.function.name }}")
        return collected
    }

    override suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String> {
        // Некоторые модели (gpt-oss-20b/DeepInfra) добавляют артефакты в имя функции:
        // "search_codebase<|channel|>commentary", "search_codebasecommentaryjson" и т.п.
        // Обрезаем всё после первого невалидного символа.
        val toolName = toolCall.function.name
            .replace(Regex("[^a-zA-Z0-9_\\-].*"), "")
            .trim()

        val serverId = resolveServerIdForTool(toolName)
            ?: return Result.failure(IllegalStateException(ToolCallingConstants.MCP_NOT_CONFIGURED))

        val arguments = parseArguments(toolCall.function.arguments)
            ?: return Result.failure(IllegalArgumentException(ToolCallingConstants.INVALID_TOOL_ARGUMENTS))

        return mcpTools.callTool(
            serverId = serverId,
            toolName = toolName,
            arguments = arguments,
            context = McpToolCallContext(agentId = context.agentId)
        )
    }

    /**
     * Get list of tools allowed for specific agent.
     * Returns null if no restrictions are set (all tools allowed).
     */
    private suspend fun getAllowedTools(agentId: Long): List<String>? {
        val fact = agentMemoryRepository.getFact(
            agentId = agentId,
            memoryKey = AgentToolsMemoryProvider.MEMORY_KEY,
            category = AgentToolsMemoryProvider.CATEGORY
        ) ?: return null  // Нет ограничений - все инструменты разрешены

        return try {
            val tools = Json.decodeFromString<List<String>>(fact.fact)
            if (tools.isEmpty()) null else tools
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getEnabledServers(): List<McpServerConfig> {
        val servers = repository.getServers().first()
        val enabled = servers.filter { it.isEnabled }
        return if (enabled.isNotEmpty()) enabled else servers
    }

    private suspend fun resolveServerIdForTool(toolName: String): String? {
        toolToServer[toolName]?.let { return it }
        android.util.Log.w(TAG, "resolveServer: '$toolName' not in toolToServer map (size=${toolToServer.size}, keys=${toolToServer.keys})")
        val servers = getEnabledServers()
        for (server in servers) {
            val tools = getConnectedTools(server.id)
            android.util.Log.d(TAG, "resolveServer fallback: server=${server.id} tools=${tools.map { it.name }}")
            if (tools.any { it.name == toolName }) {
                toolToServer[toolName] = server.id
                return server.id
            }
        }
        return null
    }

    private suspend fun getConnectedTools(serverId: String): List<McpTool> {
        return when (val state = repository.getConnectionState(serverId).first()) {
            is McpConnectionState.Connected -> state.tools
            is McpConnectionState.Error -> emptyList()
            else -> {
                when (val connected = repository.connect(serverId)) {
                    is McpConnectionState.Connected -> connected.tools
                    else -> emptyList()
                }
            }
        }
    }

    private fun parseSchema(tool: McpTool): JsonObject {
        val parsed = runCatching {
            json.parseToJsonElement(tool.inputSchemaJson)
        }.getOrNull()
        return (parsed as? JsonObject) ?: buildJsonObject { }
    }

    private fun parseArguments(rawArguments: String): JsonObject? {
        if (rawArguments.isBlank()) return buildJsonObject { }
        val parsed: JsonElement = runCatching {
            json.parseToJsonElement(rawArguments)
        }.getOrNull() ?: return null
        return parsed as? JsonObject
    }
}
