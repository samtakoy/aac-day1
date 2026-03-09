package com.example.day.features.mcp_settings.impl.di

import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.usecase.ConnectToMcpServerUseCase
import com.example.day.core.core_features.mcp.domain.usecase.GetMcpServersUseCase
import com.example.day.core.core_features.mcp.domain.usecase.GetMcpToolsUseCase
import kotlinx.serialization.json.Json

interface McpSettingsFeatureDeps {
    val getMcpServersUseCase: GetMcpServersUseCase
    val connectToMcpServerUseCase: ConnectToMcpServerUseCase
    val getMcpToolsUseCase: GetMcpToolsUseCase
    val mcpRepository: McpRepository
    val json: Json
}
