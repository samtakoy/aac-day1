package com.example.day.core.core_features.mcp.data

import android.util.Log
import com.example.day.core.core_features.mcp.data.remote.JsonRpcRequest
import com.example.day.core.core_features.mcp.data.remote.McpMethods
import com.example.day.core.core_features.mcp.data.remote.McpTransport
import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class McpToolsImpl @Inject constructor(
    private val repository: McpRepository,
    private val transport: McpTransport,
    private val json: Json
) : McpTools {
    private companion object {
        const val TAG = "McpTools"
    }

    override suspend fun callTool(
        serverId: String,
        toolName: String,
        arguments: JsonObject
    ): Result<String> = runCatching {
        val serverConfig = repository.getServers().first().find { it.id == serverId }
            ?: error("Server not found")

        val state = repository.connect(serverId)
        if (state is McpConnectionState.Error) {
            error(state.message)
        }

        val request = JsonRpcRequest(
            id = System.currentTimeMillis().toInt(),
            method = McpMethods.TOOLS_CALL,
            params = buildJsonObject {
                put("name", JsonPrimitive(toolName))
                put("arguments", arguments)
            }
        )

        Log.d(TAG, "POST tools/call to ${serverConfig.url}${serverConfig.urlPath} tool=$toolName")
        val response = transport.postStreamable(serverConfig, request)
        val result = response.result ?: error("Empty tool response")
        val decoded = json.decodeFromJsonElement(CallToolResult.serializer(), result)
        val text = decoded.content.joinToString("\n") { it.text }
        if (decoded.isError) error(text)
        text
    }

    override suspend fun listTools(serverId: String): Result<List<String>> = runCatching {
        val serverConfig = repository.getServers().first().find { it.id == serverId }
            ?: error("Server not found")
        val state = repository.connect(serverId)
        when (state) {
            is McpConnectionState.Connected -> state.tools.map { it.name }
            is McpConnectionState.Error -> error(state.message)
            else -> emptyList()
        }
    }
}

@Serializable
internal data class CallToolResult(
    @SerialName("content") val content: List<ContentItem>,
    @SerialName("isError") val isError: Boolean = false
)

@Serializable
internal data class ContentItem(
    @SerialName("type") val type: String,
    @SerialName("text") val text: String
)
