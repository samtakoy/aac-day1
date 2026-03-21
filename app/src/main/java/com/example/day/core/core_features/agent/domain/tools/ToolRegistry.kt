package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Registry of available tools for agents.
 * 
 * Provides access to tools (e.g., from MCP servers) and handles tool execution.
 * When multiple MCP servers provide tools with the same name, they are namespaced
 * using the format "serverId:toolName" to avoid conflicts.
 */
interface ToolRegistry {
    /**
     * Get available tools for the agent.
     * 
     * @param agentId The agent ID (for per-agent tool restrictions)
     * @return List of available tools with namespaced names
     */
    suspend fun getTools(agentId: Long? = null): List<ModelRequest.Tool>
    
    /**
     * Get the mapping of tool names to server IDs.
     * This map contains entries for all tools returned by getTools().
     * 
     * @return Map of "serverId:toolName" or "toolName" -> "serverId"
     */
    suspend fun getToolToServerMap(): Map<String, String>
    
    /**
     * Execute a tool call.
     * 
     * @param toolCall The tool call to execute
     * @param context The context for tool execution (includes agent ID and server routing)
     * @return Result containing the tool execution result or error
     */
    suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String>
}
