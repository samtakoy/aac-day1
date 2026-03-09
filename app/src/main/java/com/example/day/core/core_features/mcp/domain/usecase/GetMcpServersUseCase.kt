package com.example.day.core.core_features.mcp.domain.usecase

import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMcpServersUseCase @Inject constructor(
    private val repository: McpRepository
) {
    operator fun invoke(): Flow<List<McpServerConfig>> = repository.getServers()
}
