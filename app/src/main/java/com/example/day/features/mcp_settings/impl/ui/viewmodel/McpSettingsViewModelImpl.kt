package com.example.day.features.mcp_settings.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.model.TransportType
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.usecase.ConnectToMcpServerUseCase
import com.example.day.core.core_features.mcp.domain.usecase.GetMcpServersUseCase
import com.example.day.features.mcp_settings.impl.ui.model.McpServerUiModel
import com.example.day.features.mcp_settings.impl.ui.model.McpSettingsUiState
import com.example.day.features.mcp_settings.impl.ui.model.McpToolUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

internal class McpSettingsViewModelImpl(
    private val getServersUseCase: GetMcpServersUseCase,
    private val connectUseCase: ConnectToMcpServerUseCase,
    private val repository: McpRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(McpSettingsUiState())
    val state: StateFlow<McpSettingsUiState> = _state

    init {
        observeServers()
    }

    private fun observeServers() {
        viewModelScope.launch {
            getServersUseCase().collect { servers ->
                _state.update { current ->
                    val uiModels = servers.map { config ->
                        McpServerUiModel(
                            id = config.id,
                            name = config.name,
                            url = config.url,
                            isConnected = false,
                            toolsCount = 0,
                            transportType = config.transportType,
                            urlPath = config.urlPath,
                            stdioCommand = config.stdioCommand
                        )
                    }
                    current.copy(servers = uiModels)
                }
            }
        }
    }

    fun connect(serverId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            val result = connectUseCase(serverId)
            when (result) {
                is McpConnectionState.Connected -> {
                    val tools = result.tools.map { tool ->
                        McpToolUiModel(
                            name = tool.name,
                            description = tool.description,
                            inputSchemaJson = tool.inputSchemaJson
                        )
                    }
                    _state.update { current ->
                        val updatedServers = current.servers.map { server ->
                            if (server.id == serverId) {
                                server.copy(isConnected = true, toolsCount = tools.size, errorMessage = null)
                            } else server
                        }
                        current.copy(
                            servers = updatedServers,
                            selectedServerId = serverId,
                            selectedServerTools = tools,
                            isConnecting = false
                        )
                    }
                }

                is McpConnectionState.Error -> {
                    _state.update { current ->
                        val updatedServers = current.servers.map { server ->
                            if (server.id == serverId) server.copy(isConnected = false, errorMessage = result.message)
                            else server
                        }
                        current.copy(
                            servers = updatedServers,
                            isConnecting = false,
                            error = result.message
                        )
                    }
                }

                else -> _state.update { it.copy(isConnecting = false) }
            }
        }
    }

    fun selectServer(serverId: String) {
        _state.update { it.copy(selectedServerId = serverId) }
        connect(serverId)
    }

    fun addServer(
        name: String,
        url: String,
        token: String,
        transportType: TransportType,
        urlPath: String,
        stdioCommand: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.saveServer(
                    McpServerConfig(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        url = url,
                        authToken = token.trim().ifBlank { null },
                        transportType = transportType,
                        urlPath = urlPath.trim().ifBlank { defaultPath(transportType) },
                        stdioCommand = stdioCommand?.trim()?.ifBlank { null }
                    )
                )
                _state.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun showEditDialog(server: McpServerUiModel) =
        _state.update { it.copy(editingServer = server) }

    fun hideEditDialog() =
        _state.update { it.copy(editingServer = null) }

    /**
     * Saves updated server settings.
     * If [newToken] is blank, the existing token in SecretsVault is preserved.
     */
    fun editServer(
        serverId: String,
        name: String,
        url: String,
        newToken: String,
        transportType: TransportType,
        urlPath: String,
        stdioCommand: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.saveServer(
                    McpServerConfig(
                        id = serverId,
                        name = name,
                        url = url,
                        authToken = newToken.trim().ifBlank { null },
                        transportType = transportType,
                        urlPath = urlPath.trim().ifBlank { defaultPath(transportType) },
                        stdioCommand = stdioCommand?.trim()?.ifBlank { null }
                    )
                )
                _state.update { it.copy(editingServer = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            try {
                repository.deleteServer(serverId)
                _state.update { current ->
                    val updated = current.copy(
                        servers = current.servers.filter { it.id != serverId }
                    )
                    if (current.selectedServerId == serverId) {
                        updated.copy(selectedServerId = null, selectedServerTools = emptyList())
                    } else updated
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog() = _state.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() = _state.update { it.copy(showAddDialog = false) }
    fun clearError() = _state.update { it.copy(error = null) }
    fun refresh(serverId: String) = connect(serverId)

    private fun defaultPath(type: TransportType) = when (type) {
        TransportType.HTTP -> "/message"
        TransportType.SSE -> "/sse"
        TransportType.STREAMABLE_HTTP -> "/mcp"
        TransportType.STDIO -> ""
        TransportType.LOCAL -> ""
    }

    class Factory @Inject constructor(
        private val getServersUseCase: GetMcpServersUseCase,
        private val connectUseCase: ConnectToMcpServerUseCase,
        private val repository: McpRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            McpSettingsViewModelImpl(getServersUseCase, connectUseCase, repository) as T
    }
}
