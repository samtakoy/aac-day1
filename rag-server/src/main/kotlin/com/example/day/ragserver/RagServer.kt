package com.example.day.ragserver

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.embedding.createEmbeddingProvider
import com.example.day.ragserver.indexing.FileScanner
import com.example.day.ragserver.indexing.IndexingService
import com.example.day.ragserver.indexing.OllamaLlmProvider
import com.example.day.ragserver.evaluation.EvaluationService
import com.example.day.ragserver.pipeline.PipelineConfig
import com.example.day.ragserver.pipeline.PipelineExecutor
import com.example.day.ragserver.pipeline.PipelinePreset
import com.example.day.ragserver.pipeline.RerankStrategy
import com.example.day.ragserver.pipeline.RetrievalStrategy
import com.example.day.ragserver.pipeline.steps.ContextPackingStep
import com.example.day.ragserver.pipeline.steps.QueryOptimizeStep
import com.example.day.ragserver.pipeline.steps.RerankStep
import com.example.day.ragserver.pipeline.steps.RetrievalStep
import com.example.day.ragserver.pipeline.steps.ThresholdFilterStep
import com.example.day.ragserver.pipeline.steps.TopKStep
import com.example.day.ragserver.search.QueryOptimizer
import com.example.day.ragserver.search.SearchService
import com.example.day.ragserver.search.TwoStageSearchService
import com.example.day.ragserver.search.context.ContextFormatter
import com.example.day.ragserver.search.context.ContextPacker
import com.example.day.ragserver.search.rerank.HeuristicReranker
import com.example.day.ragserver.search.rerank.LlmReranker
import com.example.day.ragserver.search.rerank.NoopReranker
import com.example.day.ragserver.search.rerank.Reranker
import com.example.day.ragserver.agent_context.TaskStateUpdateRequest
import com.example.day.ragserver.agent_context.TaskStateUpdateResponse
import com.example.day.ragserver.agent_context.TaskStateUpdaterService
import com.example.day.ragserver.logging.SessionLogger
import com.example.day.ragserver.tools.registerRagTools
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import com.example.day.ragserver.comparison.ComparisonService
import kotlinx.coroutines.launch
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
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 120_000
        }
    }

    val db = CodeDatabase(config.dbPath)
    db.connect()

    val embeddingProvider = createEmbeddingProvider(config, httpClient)
    val llmProvider = if (config.extractMetadata) {
        OllamaLlmProvider(baseUrl = config.ollamaBaseUrl, model = config.llmModel, httpClient = httpClient)
    } else null
    val indexingService = IndexingService(db, embeddingProvider, llmProvider)

    val sessionLogger = SessionLogger()
    println("Session logging: ./logs/session_<date>.md")

    // QueryOptimizer (query rewrite + translate) — создаётся только если TRANSLATE_QUERIES=true.
    // Если env=false и enable_query_optimize=true в запросе — шаг пропускается с логом.
    val queryOptimizer = if (config.translateQueries) {
        val optimizerLlm = OllamaLlmProvider(
            baseUrl = config.ollamaBaseUrl,
            model = config.translateLlmModel,
            httpClient = httpClient,
        )
        QueryOptimizer(optimizerLlm, sessionLogger = sessionLogger).also {
            println("Query optimization: enabled (model: ${config.translateLlmModel})")
        }
    } else {
        println("Query optimization: disabled (set TRANSLATE_QUERIES=true to enable)")
        null
    }

    // LLM для реранкинга — создаётся всегда, независимо от других настроек.
    // Cheap операция: просто конфигурация HTTP клиента, сетевые вызовы только при реранке.
    val rerankerLlmProvider = OllamaLlmProvider(
        baseUrl = config.ollamaBaseUrl,
        model = config.rerankerLlmModel,
        httpClient = httpClient,
    )
    println("Reranker LLM:    ${config.rerankerLlmModel}")

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
    registerRagTools(mcpServer, searchService, db, config.searchTopK, embeddingProvider, queryOptimizer, sessionLogger)

    // --- Pipeline helpers ---

    fun buildReranker(strategy: RerankStrategy): Reranker = when (strategy) {
        RerankStrategy.HEURISTIC -> HeuristicReranker()
        RerankStrategy.LLM       -> LlmReranker(rerankerLlmProvider, sessionLogger = sessionLogger)
        RerankStrategy.NONE      -> NoopReranker()
    }

    fun buildPipeline(
        pipelineConfig: PipelineConfig,
        taskState: String? = null,
        history: String? = null,
    ): PipelineExecutor = PipelineExecutor(buildList {
        if (pipelineConfig.enableQueryOptimize) {
            if (queryOptimizer != null) {
                add(QueryOptimizeStep(queryOptimizer, taskState = taskState, history = history))
            } else {
                println("[Pipeline] enable_query_optimize=true but TRANSLATE_QUERIES=false — skipping QueryOptimizeStep")
            }
        }
        add(RetrievalStep(twoStageSearchService, searchService, pipelineConfig))
        if (pipelineConfig.threshold > 0.0)
            add(ThresholdFilterStep(pipelineConfig.threshold))
        add(RerankStep(buildReranker(pipelineConfig.rerankStrategy)))
        if (pipelineConfig.postRerankThreshold > 0.0)
            add(ThresholdFilterStep(pipelineConfig.postRerankThreshold))
        add(TopKStep(pipelineConfig.finalTopK))
        add(ContextPackingStep(ContextPacker(db = db)))
    })

    fun parsePipelineConfig(params: io.ktor.server.request.ApplicationRequest): PipelineConfig {
        val p = params.queryParameters
        val base = p["preset"]?.let { PipelinePreset.fromString(it).config } ?: PipelineConfig()
        return base.copy(
            enableQueryOptimize = p["enable_query_optimize"]?.toBooleanStrictOrNull() ?: base.enableQueryOptimize,
            retrievalStrategy   = p["retrieval_strategy"]?.let { s ->
                RetrievalStrategy.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
            } ?: base.retrievalStrategy,
            chunkingStrategy    = p["chunking_strategy"] ?: base.chunkingStrategy,
            retrievalTopK       = p["retrieval_topK"]?.toIntOrNull() ?: base.retrievalTopK,
            threshold           = p["threshold"]?.toDoubleOrNull() ?: base.threshold,
            rerankStrategy      = p["rerank_strategy"]?.let { s ->
                RerankStrategy.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
            } ?: base.rerankStrategy,
            finalTopK           = p["final_topK"]?.toIntOrNull() ?: base.finalTopK,
            postRerankThreshold = p["post_rerank_threshold"]?.toDoubleOrNull() ?: base.postRerankThreshold,
        )
    }

    val evaluationService = EvaluationService(buildPipeline = { buildPipeline(it) })

    val comparisonService = ComparisonService(
        ollamaProvider = rerankerLlmProvider,
        reportsDir = "./reports",
        answersPath = config.referenceAnswersPath,
    )
    println("Comparison LLM:  ${config.rerankerLlmModel} | answers: ${config.referenceAnswersPath}")

    // TODO: Кандидат на переезд в отдельный AgentServer.
    // Вся логика изолирована в agent_context/ пакете — в RagServer.kt только регистрация маршрута.
    val taskStateUpdaterService = TaskStateUpdaterService(
        llmProvider = OllamaLlmProvider(
            baseUrl = config.ollamaBaseUrl,
            model = System.getenv("TASK_STATE_LLM_MODEL") ?: config.translateLlmModel,
            httpClient = httpClient,
        ),
        sessionLogger = sessionLogger,
    )
    println("TaskState LLM:   ${System.getenv("TASK_STATE_LLM_MODEL") ?: config.translateLlmModel}")

    println("RAG MCP Server started on port ${config.serverPort}")

    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") {
        install(ServerContentNegotiation) { json(json) }
        mcpStreamableHttp { mcpServer }
        routing {
            post("/message") {
                call.respondRedirect("/mcp", permanent = false)
            }

            // TODO: Кандидат на переезд в отдельный AgentServer.
            // Логика — в agent_context/TaskStateUpdaterService.kt (изолирована от rag-server).
            post("/task-state/update") {
                val request = runCatching { call.receive<TaskStateUpdateRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body: ${it.message}")
                }
                val response = taskStateUpdaterService.update(request)
                call.respond(response)
            }

            post("/session/log-response") {
                val request = runCatching { call.receive<LogResponseRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }
                sessionLogger.logLlmResponse(
                    userMessage = request.userMessage,
                    assistantResponse = request.assistantResponse,
                    taskStateJson = request.taskStateJson,
                )
                call.respond(HttpStatusCode.OK)
            }

            get("/search") {
                val query = call.request.queryParameters["query"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'query' parameter")

                val taskState = call.request.queryParameters["task_state"]
                val history = call.request.queryParameters["history"]

                println("[rag][ktor][/search] query='${query.take(80)}', hasTaskState=${!taskState.isNullOrBlank()}, hasHistory=${!history.isNullOrBlank()}")
                sessionLogger.logSearchStart(query, taskState, history)

                val pipelineConfig = parsePipelineConfig(call.request)
                val pipeline = buildPipeline(pipelineConfig, taskState = taskState, history = history)
                var ctx = pipeline.execute(query)

                // Fallback: if post-rerank threshold filtered everything out,
                // retry without context-aware optimization (avoids wrong focus injection).
                if (ctx.results.isEmpty() && pipelineConfig.postRerankThreshold > 0.0) {
                    println("[search] Post-rerank threshold wiped results, fallback to base query: '${ctx.originalQuery}'")
                    val fallbackPipeline = buildPipeline(
                        pipelineConfig.copy(enableQueryOptimize = false),
                        taskState = null,
                        history = null,
                    )
                    val fallbackCtx = fallbackPipeline.execute(ctx.originalQuery)
                    if (fallbackCtx.results.isNotEmpty()) {
                        println("[search] Fallback succeeded: ${fallbackCtx.results.size} results")
                        ctx = fallbackCtx
                    }
                }

                if (ctx.packed == null || ctx.packed.groups.isEmpty()) {
                    call.respondText(
                        "НЕДОСТАТОЧНО_КОНТЕКСТА: по запросу \"$query\" ничего не найдено. " +
                        "Уточните запрос или проверьте что индекс содержит нужный код."
                    )
                    return@get
                }

                // Определяем KEPT/DROPPED: resultsAfterRerank — полный список после реранка до TopK
                val droppedResults = if (ctx.resultsAfterRerank.isNotEmpty()) {
                    ctx.resultsAfterRerank.drop(ctx.results.size)
                } else emptyList()
                val contextText = ContextFormatter.format(ctx.packed)
                sessionLogger.logSearchFinish(ctx.results, droppedResults, contextText)

                val debugHeader = buildDebugHeader(ctx, pipelineConfig)
                call.respondText(debugHeader + contextText)
            }

            post("/runtest/save") {
                val request = runCatching { call.receive<RuntestSaveRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
                val modelLabel = if (request.isLocalLlm) "LOCAL" else "CLOUD"
                val reportPath = "./reports/runtest_RAG_WORKER_${modelLabel}_$timestamp.md"
                File("./reports").mkdirs()
                File(reportPath).writeText(buildRuntestReport(request.preset, request.items, request.isLocalLlm, request.executionTimeMs))
                call.respond(RuntestSaveResponse(savedReport = reportPath))
                // Запускаем сравнение асинхронно после сохранения отчёта
                call.application.launch { comparisonService.tryRunComparison() }
            }

            post("/evaluate") {
                val request = runCatching { call.receive<EvaluateRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                val result = if (request.customConfig != null) {
                    val config = buildCustomPipelineConfig(request.customConfig)
                    evaluationService.evaluateCustom("CUSTOM", config, request.questions)
                } else {
                    val presets = if (request.presets.singleOrNull()?.equals("all", ignoreCase = true) == true) {
                        PipelinePreset.all
                    } else {
                        request.presets.map { PipelinePreset.fromString(it) }
                    }
                    evaluationService.evaluate(request.questions, presets)
                }

                call.respond(EvaluateResponse(
                    savedReports = result.savedReports,
                    summary = result.summary,
                    items = result.items.mapValues { (_, items) ->
                        items.map { EvalItemDto(it.question, it.optimizedQuery, it.ragContext) }
                    },
                ))
            }
        }
    }.start(wait = true)
}

@Serializable
private data class LogResponseRequest(
    val userMessage: String,
    val assistantResponse: String,
    val taskStateJson: String? = null,
)

@Serializable
private data class RuntestItemDto(
    val question: String,
    val llmAnswer: String,
)

@Serializable
private data class RuntestSaveRequest(
    val preset: String,
    val items: List<RuntestItemDto>,
    val isLocalLlm: Boolean = false,
    val executionTimeMs: Long = 0,
)

@Serializable
private data class RuntestSaveResponse(
    val savedReport: String,
)

private fun buildRuntestReport(
    preset: String,
    items: List<RuntestItemDto>,
    isLocalLlm: Boolean = false,
    executionTimeMs: Long = 0,
): String = buildString {
    val modelLabel = if (isLocalLlm) "Локальная (Ollama)" else "Облачная"
    appendLine("# Runtest: ${preset.uppercase()} | ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
    appendLine("**Модель:** $modelLabel")
    appendLine("**Время выполнения:** ${executionTimeMs} мс")
    appendLine()
    items.forEachIndexed { i, item ->
        appendLine("## Q${i + 1}: \"${item.question}\"")
        appendLine(item.llmAnswer)
        appendLine()
    }
}

@Serializable
private data class EvaluateRequest(
    val questions: List<String>,
    val presets: List<String> = listOf("all"),
    val customConfig: CustomConfigDto? = null,
)

@Serializable
private data class CustomConfigDto(
    val retrievalTopK: Int? = null,
    val threshold: Double? = null,
    val rerankStrategy: String? = null,
    val finalTopK: Int? = null,
    val enableQueryOptimize: Boolean? = null,
    val retrievalStrategy: String? = null,
    val chunkingStrategy: String? = null,
)

private fun buildCustomPipelineConfig(dto: CustomConfigDto): PipelineConfig {
    val base = PipelinePreset.BASELINE.config
    return base.copy(
        retrievalTopK = dto.retrievalTopK ?: base.retrievalTopK,
        threshold = dto.threshold ?: base.threshold,
        rerankStrategy = dto.rerankStrategy?.let { s ->
            RerankStrategy.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
        } ?: base.rerankStrategy,
        finalTopK = dto.finalTopK ?: base.finalTopK,
        enableQueryOptimize = dto.enableQueryOptimize ?: base.enableQueryOptimize,
        retrievalStrategy = dto.retrievalStrategy?.let { s ->
            RetrievalStrategy.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
        } ?: base.retrievalStrategy,
        chunkingStrategy = dto.chunkingStrategy ?: base.chunkingStrategy,
    )
}

@Serializable
private data class EvalItemDto(
    val question: String,
    val optimizedQuery: String?,
    val ragContext: String,
)

@Serializable
private data class EvaluateResponse(
    val savedReports: List<String>,
    val summary: String,
    val items: Map<String, List<EvalItemDto>>,
)

private fun buildDebugHeader(
    ctx: com.example.day.ragserver.pipeline.PipelineContext,
    config: PipelineConfig,
): String {
    val m = ctx.metrics
    val counts = buildString {
        append("Retrieved: ${m.countAfterRetrieval}")
        if (config.threshold > 0.0) append(" → Filtered: ${m.countAfterFilter}")
        append(" → Final: ${ctx.results.size}")
    }
    val timings = m.timings.entries.joinToString(", ") { (k, v) -> "$k=${v}ms" }
    val queryLine = m.optimizedQuery
        ?.takeIf { it != ctx.originalQuery }
        ?.let { "\nQuery: \"${ctx.originalQuery}\" → \"$it\"" }
        ?: ""
    return "Pipeline: ${config.rerankStrategy} | $counts$queryLine\nTimings: $timings\n\n"
}
