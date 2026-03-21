package com.example.day.core.core_features.agent.data.tools

import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolRegistry
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
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

/**
 * Tool registry that integrates with MCP servers.
 * 
 * Tool naming with multiple servers:
 * When multiple MCP servers provide tools with the same name, this registry uses
 * namespacing to avoid conflicts:
 * - If only one server has "search_codebase", the tool is named "search_codebase"
 * - If multiple servers have "search_codebase", they are named "serverId:search_codebase"
 * 
 * Tool access control:
 * - Per-agent restrictions are managed via AgentMemoryRepository
 * - If no restrictions are set for an agent (allowedTools == null), all tools from
 *   connected MCP servers are available
 */
internal class McpToolRegistry @Inject constructor(
    private val repository: McpRepository,
    private val mcpTools: McpTools,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json
) : ToolRegistry {

    private val toolToServer = ConcurrentHashMap<String, String>()

    override suspend fun getTools(agentId: Long?): List<ModelRequest.Tool> {
        val servers = getEnabledServers()
        if (servers.isEmpty()) return emptyList()

        toolToServer.clear()
        
        // Get per-agent allowed tools (null = all tools allowed)
        val allowedTools = agentId?.let { getAllowedTools(it) }

        // Collect all tools grouped by name (for conflict detection)
        val toolsByName = mutableMapOf<String, MutableList<Pair<McpServerConfig, McpTool>>>()
        
        servers.forEach { server ->
            val tools = getConnectedTools(server.id)
            tools.forEach { tool ->
                // Check: tool in per-agent allowed list (if configured)
                if (allowedTools != null && !allowedTools.contains(tool.name)) return@forEach
                
                toolsByName.getOrPut(tool.name) { mutableListOf() }.add(server to tool)
            }
        }

        // Build final tool list with namespace prefixes
        val collected = mutableListOf<ModelRequest.Tool>()
        
        toolsByName.forEach { (toolName, serverToolPairs) ->
            if (serverToolPairs.size == 1) {
                // Only one server has this tool - use original name
                val (server, tool) = serverToolPairs.first()
                toolToServer[toolName] = server.id
                
                collected.add(toModelRequestTool(toolName, tool))
            } else {
                // Multiple servers have this tool - use namespace prefixes
                serverToolPairs.forEach { (server, tool) ->
                    val namespacedName = "${server.id}${ToolCallContext.NAMESPACE_SEPARATOR}$toolName"
                    toolToServer[namespacedName] = server.id
                    
                    // Also map the original name to the first server for backward compatibility
                    if (!toolToServer.containsKey(toolName)) {
                        toolToServer[toolName] = server.id
                    }
                    
                    collected.add(toModelRequestTool(namespacedName, tool, server.id))
                }
            }
        }
        
        return collected
    }

    private fun toModelRequestTool(
        toolName: String,
        tool: McpTool,
        serverId: String? = null
    ): ModelRequest.Tool {
        val description = if (serverId != null) {
            "[${serverId}] ${tool.description}"
        } else {
            tool.description
        }
        
        return ModelRequest.Tool(
            type = ModelRequest.ToolType.Function,
            function = ModelRequest.Function(
                name = toolName,
                description = description,
                parameters = parseSchema(tool)
            )
        )
    }

    override suspend fun getToolToServerMap(): Map<String, String> {
        return toolToServer.toMap()
    }

    override suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String> {
        val toolName = toolCall.function.name
        
        // Check if the tool name contains namespace separator
        val (serverId, originalToolName) = parseToolName(toolName)
        
        // Verify tool is in allowed list (if restrictions are configured)
        // Check both namespaced and original name
        val allowedTools = context.agentId?.let { getAllowedTools(it) }
        if (allowedTools != null && 
            !allowedTools.contains(toolName) && 
            !allowedTools.contains(originalToolName)) {
            return Result.failure(
                IllegalArgumentException("${ToolCallingConstants.TOOL_NOT_ALLOWED_PREFIX}: $toolName")
            )
        }

        // Use server from context map, or resolve from name
        val resolvedServerId = context.toolToServer[toolName] ?: serverId
            ?: return Result.failure(IllegalStateException(ToolCallingConstants.MCP_NOT_CONFIGURED))

        val arguments = parseArguments(toolCall.function.arguments)
            ?: return Result.failure(IllegalArgumentException(ToolCallingConstants.INVALID_TOOL_ARGUMENTS))

        return mcpTools.callTool(
            serverId = resolvedServerId,
            toolName = originalToolName,  // Use original name for MCP call
            arguments = arguments,
            context = McpToolCallContext(agentId = context.agentId)
        )
    }

    /**
     * Parse tool name to extract server ID and original tool name.
     * Format: "serverId:toolName" -> (serverId, toolName)
     *         "toolName" -> (null, toolName)
     */
    private fun parseToolName(toolName: String): Pair<String?, String> {
        val separator = ToolCallContext.NAMESPACE_SEPARATOR
        return if (toolName.contains(separator)) {
            val parts = toolName.split(separator, limit = 2)
            parts[0] to parts[1]
        } else {
            null to toolName
        }
    }

    /**
     * Get list of tools allowed for specific agent.
     * Returns null if no restrictions are set (all tools from MCP servers are allowed).
     * 
     * Tools are stored in agent memory with:
     * - memoryKey: AgentToolsMemoryProvider.MEMORY_KEY
     * - category: AgentToolsMemoryProvider.CATEGORY
     * - value: JSON array of tool names, e.g. ["search_codebase", "get_file_content"]
     */
    private suspend fun getAllowedTools(agentId: Long): List<String>? {
        val fact = agentMemoryRepository.getFact(
            agentId = agentId,
            memoryKey = AgentToolsMemoryProvider.MEMORY_KEY,
            category = AgentToolsMemoryProvider.CATEGORY
        ) ?: return null  // No restrictions - all tools allowed

        return try {
            val tools = Json.decodeFromString<List<String>>(fact.fact)
            if (tools.isEmpty()) null else tools
        } catch (e: Exception) {
            null  // Invalid format - allow all tools
        }
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
