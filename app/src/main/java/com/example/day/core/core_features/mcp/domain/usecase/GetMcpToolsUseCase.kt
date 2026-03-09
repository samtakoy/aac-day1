package com.example.day.core.core_features.mcp.domain.usecase

import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpTool
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import javax.inject.Inject

/** Connects to a server and returns its tools, or empty list on error */
class GetMcpToolsUseCase @Inject constructor(
    private val repository: McpRepository
) {
    suspend operator fun invoke(serverId: String): Result<List<McpTool>> {
        return try {
            val state = repository.connect(serverId)
            when (state) {
                is McpConnectionState.Connected -> Result.success(state.tools)
                is McpConnectionState.Error -> Result.failure(Exception(state.message))
                else -> Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
