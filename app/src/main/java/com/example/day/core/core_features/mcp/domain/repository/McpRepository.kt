package com.example.day.core.core_features.mcp.domain.repository

import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.model.McpTool
import kotlinx.coroutines.flow.Flow

interface McpRepository {

    /** Observe list of configured servers */
    fun getServers(): Flow<List<McpServerConfig>>

    /** Save or update server configuration */
    suspend fun saveServer(config: McpServerConfig)

    /** Delete server configuration */
    suspend fun deleteServer(serverId: String)

    /** Connect to MCP server: initialize + fetch tools */
    suspend fun connect(serverId: String): McpConnectionState

    /** Observe connection state for a specific server */
    fun getConnectionState(serverId: String): Flow<McpConnectionState>

    /** Get count of configured servers */
    suspend fun configuredCount(): Int
}
