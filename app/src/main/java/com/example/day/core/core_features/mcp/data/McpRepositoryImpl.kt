package com.example.day.core.core_features.mcp.data

import com.example.day.core.core_features.mcp.data.local.SecretsVault
import com.example.day.core.core_features.mcp.data.local.dao.McpServerDao
import com.example.day.core.core_features.mcp.data.local.entity.McpServerEntity
import com.example.day.core.core_features.mcp.data.remote.McpTransport
import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.model.TransportType
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class McpRepositoryImpl @Inject constructor(
    private val dao: McpServerDao,
    private val transport: McpTransport,
    private val secretsVault: SecretsVault
) : McpRepository {

    private val connectionStates = MutableStateFlow<Map<String, McpConnectionState>>(emptyMap())

    override fun getServers(): Flow<List<McpServerConfig>> =
        dao.getAllServers().map { entities -> entities.map { it.toConfig() } }

    override suspend fun saveServer(config: McpServerConfig) {
        dao.insertServer(
            McpServerEntity(
                id = config.id,
                name = config.name,
                url = config.url,
                authTokenAlias = config.authToken?.let { "token_${config.id}" },
                isEnabled = config.isEnabled,
                transportType = config.transportType.name,
                urlPath = config.urlPath,
                stdioCommand = config.stdioCommand
            )
        )
        config.authToken?.let { secretsVault.saveToken(config.id, it) }
    }

    override suspend fun deleteServer(serverId: String) {
        val entity = dao.getServerById(serverId) ?: return
        dao.deleteServer(entity)
        secretsVault.deleteToken(serverId)
        connectionStates.update { it - serverId }
    }

    override suspend fun connect(serverId: String): McpConnectionState {
        val entity = dao.getServerById(serverId)
            ?: return McpConnectionState.Error("Сервер не найден", serverId)

        val token = secretsVault.getToken(serverId)
        val config = entity.toConfig().copy(authToken = token)

        return try {
            val tools = transport.connect(config)
            McpConnectionState.Connected(serverId, tools).also { state ->
                connectionStates.update { it + (serverId to state) }
            }
        } catch (e: Exception) {
            McpConnectionState.Error(e.message ?: "Неизвестная ошибка", serverId).also { state ->
                connectionStates.update { it + (serverId to state) }
            }
        }
    }

    override fun getConnectionState(serverId: String): Flow<McpConnectionState> =
        connectionStates.map { it[serverId] ?: McpConnectionState.Disconnected }

    override suspend fun configuredCount(): Int = dao.getCount()

    private fun McpServerEntity.toConfig() = McpServerConfig(
        id = id,
        name = name,
        url = url,
        authToken = null,  // token fetched from SecretsVault on demand
        isEnabled = isEnabled,
        transportType = runCatching { TransportType.valueOf(transportType) }.getOrDefault(TransportType.HTTP),
        urlPath = urlPath,
        stdioCommand = stdioCommand
    )
}
