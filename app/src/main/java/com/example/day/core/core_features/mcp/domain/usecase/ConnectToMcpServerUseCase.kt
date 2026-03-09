package com.example.day.core.core_features.mcp.domain.usecase

import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import javax.inject.Inject

class ConnectToMcpServerUseCase @Inject constructor(
    private val repository: McpRepository
) {
    suspend operator fun invoke(serverId: String): McpConnectionState =
        repository.connect(serverId)
}
