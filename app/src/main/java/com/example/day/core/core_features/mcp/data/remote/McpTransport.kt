package com.example.day.core.core_features.mcp.data.remote

import com.example.day.core.core_features.mcp.domain.model.McpServerConfig
import com.example.day.core.core_features.mcp.domain.model.McpTool
import com.example.day.core.core_features.mcp.domain.model.TransportType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level MCP transport supporting three modes:
 *
 * **HTTP** — классический JSON-RPC POST на `{url}{urlPath}` (напр. `/message`).
 *
 * **SSE** — устаревший SSE-транспорт (MCP Inspector):
 *   1. GET `{url}{urlPath}` (напр. `/sse`) → сервер отправляет `event: endpoint`.
 *   2. POST JSON-RPC на полученный session URL.
 *   3. Ответы приходят через SSE как `event: message`.
 *
 * **STREAMABLE_HTTP** — новый транспорт по спецификации MCP 2025-03-26:
 *   POST `{url}{urlPath}` (напр. `/mcp`) с заголовком
 *   `Accept: application/json, text/event-stream`.
 *   Сервер отвечает либо `application/json`, либо `text/event-stream` (SSE).
 */
@Singleton
internal class McpTransport @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val requestId = AtomicInteger(0)
    private val sessionIds = ConcurrentHashMap<String, String>()

    /** Dispatches to the correct transport based on [config.transportType]. */
    suspend fun connect(config: McpServerConfig): List<McpTool> = when (config.transportType) {
        TransportType.HTTP -> connectHttp(config)
        TransportType.SSE -> connectSse(config)
        TransportType.STREAMABLE_HTTP -> connectStreamableHttp(config)
        TransportType.STDIO -> connectStdio(config)
    }

    // ── HTTP (legacy) ─────────────────────────────────────────────────────────

    private suspend fun connectHttp(config: McpServerConfig): List<McpTool> {
        postHttpRpc(config, initRequest())   // initialize (result ignored for tool listing)
        val toolsRpc = postHttpRpc(config, JsonRpcRequest(id = nextId(), method = McpMethods.TOOLS_LIST, params = null))
        return parseTools(toolsRpc)
    }

    private suspend fun postHttpRpc(config: McpServerConfig, request: JsonRpcRequest): JsonRpcResponse {
        val url = buildUrl(config.url, config.urlPath)
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            config.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(json.encodeToString(JsonRpcRequest.serializer(), request))
        }
        return decodeOrThrow(response.bodyAsText())
    }

    // ── SSE (legacy) ──────────────────────────────────────────────────────────

    /**
     * 1. Opens SSE connection (GET).
     * 2. Waits for `event: endpoint` → session URL.
     * 3. POSTs initialize + tools/list; receives `event: message` responses.
     */
    private suspend fun connectSse(config: McpServerConfig): List<McpTool> = coroutineScope {
        val sseUrl = buildUrl(config.url, config.urlPath)
        val endpointDeferred = CompletableDeferred<String>()
        val responseChannel = Channel<String>(Channel.UNLIMITED)

        val sseJob = launch {
            httpClient.sse(urlString = sseUrl, request = {
                config.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }) {
                incoming.collect { event ->
                    when (event.event) {
                        "endpoint" -> if (!endpointDeferred.isCompleted) {
                            endpointDeferred.complete(event.data ?: "")
                        }
                        "message" -> event.data?.let { responseChannel.trySend(it) }
                        else -> {}
                    }
                }
            }
        }

        try {
            val sessionPath = withTimeout(15_000) { endpointDeferred.await() }
            val postUrl = resolveUrl(config.url, sessionPath)

            postForSse(config, postUrl, initRequest())
            withTimeout(30_000) { responseChannel.receive() }   // initialize response (ignored)

            postForSse(config, postUrl, JsonRpcRequest(id = nextId(), method = McpMethods.TOOLS_LIST, params = null))
            val toolsData = withTimeout(30_000) { responseChannel.receive() }
            parseTools(decodeOrThrow(toolsData))
        } finally {
            sseJob.cancel()
        }
    }

    private suspend fun postForSse(config: McpServerConfig, postUrl: String, request: JsonRpcRequest) {
        httpClient.post(postUrl) {
            contentType(ContentType.Application.Json)
            config.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(json.encodeToString(JsonRpcRequest.serializer(), request))
        }
    }

    // ── Streamable HTTP (MCP spec 2025-03-26) ─────────────────────────────────

    /**
     * POST с `Accept: application/json, text/event-stream`.
     * Если сервер вернул SSE — парсим тело как поток событий.
     * Если вернул JSON — декодируем напрямую.
     */
    private suspend fun connectStreamableHttp(config: McpServerConfig): List<McpTool> {
        // Start a fresh session for each connect to avoid "already initialized"
        sessionIds.remove(config.id)
        postStreamable(config, initRequest(), includeSessionHeader = false)   // initialize
        val toolsRpc = postStreamable(
            config,
            JsonRpcRequest(id = nextId(), method = McpMethods.TOOLS_LIST, params = null),
            includeSessionHeader = true
        )
        return parseTools(toolsRpc)
    }

    internal suspend fun postStreamable(
        config: McpServerConfig,
        request: JsonRpcRequest,
        includeSessionHeader: Boolean = true
    ): JsonRpcResponse {
        val url = buildUrl(config.url, config.urlPath)
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            if (includeSessionHeader) {
                sessionIds[config.id]?.let { header("mcp-session-id", it) }
            }
            config.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(json.encodeToString(JsonRpcRequest.serializer(), request))
        }
        response.headers["mcp-session-id"]?.let { sessionId ->
            if (sessionId.isNotBlank()) {
                sessionIds[config.id] = sessionId
            }
        }
        val body = response.bodyAsText()
        val ct = response.contentType()
        return if (ct != null && ct.match(ContentType("text", "event-stream"))) {
            parseSseBody(body)
        } else {
            decodeOrThrow(body)
        }
    }

    /**
     * Parses a complete SSE body (not a live stream) and returns the first
     * non-empty `data:` field decoded as [JsonRpcResponse].
     *
     * SSE format:
     * ```
     * event: message
     * data: {"jsonrpc":"2.0","id":1,"result":{...}}
     *
     * ```
     */
    private fun parseSseBody(body: String): JsonRpcResponse {
        for (line in body.lines()) {
            if (line.startsWith("data:")) {
                val data = line.removePrefix("data:").trim()
                if (data.isNotBlank() && data != "[DONE]") {
                    return decodeOrThrow(data)
                }
            }
        }
        throw McpTransportException("Нет данных в SSE-ответе сервера", -1)
    }

    // ── STDIO ─────────────────────────────────────────────────────────────────

    /**
     * Запускает локальный MCP-сервер через [ProcessBuilder] и общается с ним
     * через stdin/stdout.
     *
     * Каждое JSON-RPC сообщение — одна строка (newline-delimited JSON).
     * Требует наличия исполняемого файла на устройстве.
     *
     * Пример команды: `/data/data/com.example/files/mcp-server --stdio`
     */
    private suspend fun connectStdio(config: McpServerConfig): List<McpTool> {
        val command = config.stdioCommand?.trim()?.takeIf { it.isNotBlank() }
            ?: throw McpTransportException("STDIO: команда не указана", -1)

        val cmdParts = command.split(Regex("\\s+"))
        val process = ProcessBuilder(cmdParts)
            .redirectErrorStream(false)
            .start()

        return try {
            val writer = process.outputStream.bufferedWriter()
            val reader = process.inputStream.bufferedReader()

            suspend fun send(req: JsonRpcRequest) = runInterruptible {
                writer.write(json.encodeToString(JsonRpcRequest.serializer(), req))
                writer.newLine()
                writer.flush()
            }

            suspend fun receive(): JsonRpcResponse {
                val line = withTimeout(30_000) {
                    runInterruptible { reader.readLine() }
                } ?: throw McpTransportException("STDIO: процесс завершился неожиданно", -1)
                return decodeOrThrow(line)
            }

            send(initRequest())
            receive()  // initialize response (сохраняем, но инструменты нас не интересуют здесь)

            send(JsonRpcRequest(id = nextId(), method = McpMethods.TOOLS_LIST, params = null))
            parseTools(receive())
        } finally {
            process.destroyForcibly()
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun initRequest() = JsonRpcRequest(
        id = nextId(),
        method = McpMethods.INITIALIZE,
        params = buildJsonObject {
            put("protocolVersion", "2024-11-05")
            put("capabilities", buildJsonObject {})
            put(
                "clientInfo",
                json.encodeToJsonElement(ClientInfo(name = "Day-App", version = "1.0"))
            )
        }
    )

    private fun decodeOrThrow(raw: String): JsonRpcResponse {
        val rpc = json.decodeFromString(JsonRpcResponse.serializer(), raw)
        if (rpc.error != null) throw McpTransportException(rpc.error.message, rpc.error.code)
        return rpc
    }

    private fun parseTools(rpc: JsonRpcResponse): List<McpTool> {
        val result = json.decodeFromJsonElement(ToolsListResult.serializer(), rpc.result!!)
        return result.tools.map { tool ->
            McpTool(
                name = tool.name,
                description = tool.description.orEmpty(),
                inputSchemaJson = tool.inputSchema?.toString() ?: "{}"
            )
        }
    }

    private fun buildUrl(baseUrl: String, path: String): String =
        "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"

    private fun resolveUrl(baseUrl: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"

    private fun nextId() = requestId.incrementAndGet()
}

class McpTransportException(message: String, val code: Int) : Exception(message)
