package com.example.day.core.core_features.mcp.domain.model

import kotlinx.serialization.Serializable

/** How the client communicates with the MCP server */
enum class TransportType {
    /** Classic JSON-RPC POST to {url}{urlPath} (MCP Inspector legacy) */
    HTTP,
    /** SSE stream: GET {url}{urlPath} → receive endpoint → POST JSON-RPC (legacy SSE transport) */
    SSE,
    /**
     * Streamable HTTP (MCP spec 2025-03-26).
     * POST {url}{urlPath} with Accept: application/json, text/event-stream.
     * Server replies with either a plain JSON response or an SSE stream.
     */
    STREAMABLE_HTTP,
    /**
     * STDIO — запускает локальный процесс, общение через stdin/stdout.
     * Требует наличия исполняемого файла на устройстве.
     * Пример: `/data/data/com.example/files/mcp-server --stdio`
     */
    STDIO,
    /** Local in-memory MCP implementation (no network). */
    LOCAL
}

/** Domain model for MCP Server configuration */
@Serializable
data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val authToken: String? = null,
    val isEnabled: Boolean = true,
    val transportType: TransportType = TransportType.HTTP,
    /** Path appended to [url]: `/message` for HTTP, `/sse` for SSE, `/mcp` for Streamable HTTP */
    val urlPath: String = "/message",
    /** Shell command for STDIO transport (e.g. `/data/data/.../mcp-server --stdio`) */
    val stdioCommand: String? = null
)

/** Domain model for MCP Tool */
@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String = "{}"  // raw JSON string for display
)

/** Optional context passed to MCP tool calls */
data class McpToolCallContext(
    val agentId: Long? = null,
    val chatId: Long? = null
)

/** Connection lifecycle */
sealed class McpConnectionState {
    data object Disconnected : McpConnectionState()
    data object Connecting : McpConnectionState()
    data class Connected(val serverId: String, val tools: List<McpTool>) : McpConnectionState()
    data class Error(val message: String, val serverId: String? = null) : McpConnectionState()
}
