package com.example.day.features.mcp_settings.impl.ui.model

import com.example.day.core.core_features.mcp.domain.model.TransportType

/** UI representation of an MCP server */
data class McpServerUiModel(
    val id: String,
    val name: String,
    val url: String,
    val isConnected: Boolean,
    val toolsCount: Int,
    val errorMessage: String? = null,
    val transportType: TransportType = TransportType.HTTP,
    val urlPath: String = "/message",
    val stdioCommand: String? = null
)

/** UI representation of a single MCP tool */
data class McpToolUiModel(
    val name: String,
    val description: String,
    val inputSchemaJson: String
)

/** Full UI state for the MCP Settings screen */
data class McpSettingsUiState(
    val servers: List<McpServerUiModel> = emptyList(),
    val selectedServerId: String? = null,
    val selectedServerTools: List<McpToolUiModel> = emptyList(),
    val isConnecting: Boolean = false,
    val showAddDialog: Boolean = false,
    /** Non-null = the server currently being edited */
    val editingServer: McpServerUiModel? = null,
    val error: String? = null
)
