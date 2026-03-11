package com.example.day.core.core_features.agent.data.tools

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
    private val json: Json
) : ToolProvider {

    private val toolToServer = ConcurrentHashMap<String, String>()

    override suspend fun getTools(): List<ModelRequest.Tool> {
        val servers = getEnabledServers()
        if (servers.isEmpty()) return emptyList()

        toolToServer.clear()
        // toolToServer map is rebuilt for each tool listing

        val collected = mutableListOf<ModelRequest.Tool>()
        servers.forEach { server ->
            val tools = getConnectedTools(server.id)
            tools.forEach { tool ->
                if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(tool.name)) return@forEach
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
        return collected
    }

    override suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String> {
        val toolName = toolCall.function.name
        if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(toolName)) {
            return Result.failure(
                IllegalArgumentException("${ToolCallingConstants.TOOL_NOT_ALLOWED_PREFIX}: $toolName")
            )
        }

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

    private suspend fun getEnabledServers(): List<McpServerConfig> {
        val servers = repository.getServers().first()
        val enabled = servers.filter { it.isEnabled }
        return if (enabled.isNotEmpty()) enabled else servers
    }

    private suspend fun resolveServerIdForTool(toolName: String): String? {
        toolToServer[toolName]?.let { return it }
        val servers = getEnabledServers()
        for (server in servers) {
            val tools = getConnectedTools(server.id)
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
