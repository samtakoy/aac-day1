package com.example.day.core.core_features.mcp.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** JSON-RPC 2.0 Request */
@Serializable
internal data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: JsonElement? = null
)

/** JSON-RPC 2.0 Response */
@Serializable
internal data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

/** JSON-RPC Error */
@Serializable
internal data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

/** MCP Initialize Request Params */
@Serializable
internal data class InitializeParams(
    val protocolVersion: String = "2024-11-05",
    /** Empty capabilities — JsonObject is properly serializable */
    val capabilities: JsonObject = JsonObject(emptyMap()),
    val clientInfo: ClientInfo
)

@Serializable
internal data class ClientInfo(
    val name: String,
    val version: String
)

/** MCP Initialize Result */
@Serializable
internal data class InitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject = JsonObject(emptyMap()),
    val serverInfo: ServerInfo
)

@Serializable
internal data class ServerInfo(
    val name: String,
    val version: String
)

/** MCP tools/list result */
@Serializable
internal data class ToolsListResult(
    val tools: List<ToolDefinition>
)

/** Tool definition as returned by MCP server */
@Serializable
internal data class ToolDefinition(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
)

internal object McpMethods {
    const val INITIALIZE = "initialize"
    const val TOOLS_LIST = "tools/list"
}
