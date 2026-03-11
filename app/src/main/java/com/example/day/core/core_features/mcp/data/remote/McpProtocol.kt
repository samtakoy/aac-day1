package com.example.day.core.core_features.mcp.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** JSON-RPC 2.0 Request */
@Serializable
internal data class JsonRpcRequest(
    @kotlinx.serialization.SerialName("jsonrpc") val jsonrpc: String = "2.0",
    @kotlinx.serialization.SerialName("id") val id: Long,
    @kotlinx.serialization.SerialName("method") val method: String,
    @kotlinx.serialization.SerialName("params") val params: JsonElement? = null
)

/** JSON-RPC 2.0 Response */
@Serializable
internal data class JsonRpcResponse(
    @kotlinx.serialization.SerialName("jsonrpc") val jsonrpc: String = "2.0",
    @kotlinx.serialization.SerialName("id") val id: Long? = null,
    @kotlinx.serialization.SerialName("result") val result: JsonElement? = null,
    @kotlinx.serialization.SerialName("error") val error: JsonRpcError? = null
)

/** JSON-RPC Error */
@Serializable
internal data class JsonRpcError(
    @kotlinx.serialization.SerialName("code") val code: Int,
    @kotlinx.serialization.SerialName("message") val message: String,
    @kotlinx.serialization.SerialName("data") val data: JsonElement? = null
)

/** MCP Initialize Request Params */
@Serializable
internal data class InitializeParams(
    @kotlinx.serialization.SerialName("protocolVersion") val protocolVersion: String = "2024-11-05",
    /** Empty capabilities — JsonObject is properly serializable */
    @kotlinx.serialization.SerialName("capabilities") val capabilities: JsonObject = JsonObject(emptyMap()),
    @kotlinx.serialization.SerialName("clientInfo") val clientInfo: ClientInfo
)

@Serializable
internal data class ClientInfo(
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("version") val version: String
)

/** MCP Initialize Result */
@Serializable
internal data class InitializeResult(
    @kotlinx.serialization.SerialName("protocolVersion") val protocolVersion: String,
    @kotlinx.serialization.SerialName("capabilities") val capabilities: JsonObject = JsonObject(emptyMap()),
    @kotlinx.serialization.SerialName("serverInfo") val serverInfo: ServerInfo
)

@Serializable
internal data class ServerInfo(
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("version") val version: String
)

/** MCP tools/list result */
@Serializable
internal data class ToolsListResult(
    @kotlinx.serialization.SerialName("tools") val tools: List<ToolDefinition>
)

/** Tool definition as returned by MCP server */
@Serializable
internal data class ToolDefinition(
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("description") val description: String? = null,
    @kotlinx.serialization.SerialName("inputSchema") val inputSchema: JsonElement? = null
)

internal object McpMethods {
    const val INITIALIZE = "initialize"
    const val TOOLS_LIST = "tools/list"
    const val TOOLS_CALL = "tools/call"
}
