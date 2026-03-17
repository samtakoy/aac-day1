package com.example.day.ragserver

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.embedding.createEmbeddingProvider
import com.example.day.ragserver.indexing.FileScanner
import com.example.day.ragserver.indexing.IndexingService
import com.example.day.ragserver.search.SearchService
import com.example.day.ragserver.tools.registerRagTools
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() {
    val config = RagConfig.from()
    println("=== RAG MCP Server ===")
    println("Code path:  ${config.codePath}")
    println("DB path:    ${config.dbPath}")
    println("Embedding:  ${config.embeddingProvider} / ${config.embeddingModel}")
    println("Port:       ${config.serverPort}")
    println("Force reindex: ${config.forceReindex}")

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) { requestTimeoutMillis = 120_000 }
    }

    val db = CodeDatabase(config.dbPath)
    db.connect()

    val embeddingProvider = createEmbeddingProvider(config, httpClient)
    val indexingService = IndexingService(db, embeddingProvider)

    println("\n--- Starting indexing ---")
    runBlocking {
        indexingService.indexAll(FileScanner, config)
    }
    println("--- Indexing complete ---\n")

    val mcpServer = Server(
        serverInfo = Implementation(name = "codebase-rag", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    val searchService = SearchService(db, embeddingProvider)
    registerRagTools(mcpServer, searchService, db, config.searchTopK)

    println("RAG MCP Server started on port ${config.serverPort}")

    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") {
        install(ServerContentNegotiation) { json(json) }
        mcpStreamableHttp { mcpServer }
        routing {
            post("/message") {
                call.respondRedirect("/mcp", permanent = false)
            }
        }
    }.start(wait = true)
}
