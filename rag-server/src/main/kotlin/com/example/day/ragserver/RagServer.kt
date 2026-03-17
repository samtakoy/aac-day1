package com.example.day.ragserver

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.formatHeader
import com.example.day.ragserver.embedding.createEmbeddingProvider
import com.example.day.ragserver.indexing.FileScanner
import com.example.day.ragserver.indexing.IndexingService
import com.example.day.ragserver.indexing.OllamaLlmProvider
import com.example.day.ragserver.search.QueryTranslator
import com.example.day.ragserver.search.SearchService
import com.example.day.ragserver.search.TwoStageSearchService
import com.example.day.ragserver.search.context.ContextFormatter
import com.example.day.ragserver.search.context.ContextPacker
import com.example.day.ragserver.tools.registerRagTools
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
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
    // Redirect stdout → stderr so println output is unbuffered and
    // appears interleaved with SLF4J/Logback logs in the correct order.
    System.setOut(System.err)

    val config = RagConfig.from()
    println("=== RAG MCP Server ===")
    println("Code path:       ${config.codePath}")
    println("DB path:         ${config.dbPath}")
    println("Embedding:       ${config.embeddingProvider} / ${config.embeddingModel}")
    println("Port:            ${config.serverPort}")
    println("Force reindex:   ${config.forceReindex}")
    println("Extract metadata:${config.extractMetadata}" + if (config.extractMetadata) " (LLM: ${config.llmModel})" else "")

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
    val llmProvider = if (config.extractMetadata) {
        OllamaLlmProvider(baseUrl = config.ollamaBaseUrl, model = config.llmModel, httpClient = httpClient)
    } else null
    val indexingService = IndexingService(db, embeddingProvider, llmProvider)

    // QueryTranslator создаётся только если TRANSLATE_QUERIES=true.
    // Может использовать отдельную модель (TRANSLATE_LLM_MODEL) — например, быструю,
    // специализированную на переводе, отличную от модели для извлечения метаданных.
    val queryTranslator = if (config.translateQueries) {
        val translateLlm = OllamaLlmProvider(
            baseUrl = config.ollamaBaseUrl,
            model = config.translateLlmModel,
            httpClient = httpClient,
        )
        QueryTranslator(translateLlm).also {
            println("Query translation: enabled (model: ${config.translateLlmModel})")
        }
    } else {
        println("Query translation: disabled (set TRANSLATE_QUERIES=true to enable)")
        null
    }

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
    val twoStageSearchService = TwoStageSearchService(db, embeddingProvider)
    registerRagTools(mcpServer, searchService, db, config.searchTopK, embeddingProvider, queryTranslator)

    println("RAG MCP Server started on port ${config.serverPort}")

    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") {
        install(ServerContentNegotiation) { json(json) }
        mcpStreamableHttp { mcpServer }
        routing {
            post("/message") {
                call.respondRedirect("/mcp", permanent = false)
            }
            get("/search") {
                val query = call.request.queryParameters["query"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'query' parameter")
                val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
                val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
                if (results.isEmpty()) {
                    call.respondText("Ничего не найдено по запросу: $query")
                    return@get
                }
                val packed = ContextPacker().pack(results)
                call.respondText(ContextFormatter.format(packed))
            }
        }
    }.start(wait = true)
}
