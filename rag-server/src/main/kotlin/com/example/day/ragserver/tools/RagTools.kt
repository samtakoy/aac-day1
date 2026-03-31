package com.example.day.ragserver.tools

import com.example.day.ragserver.logging.SessionLogger
import com.example.day.ragserver.pipeline.PipelineConfig
import com.example.day.ragserver.pipeline.PipelineExecutor
import com.example.day.ragserver.search.context.ContextFormatter
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RagTools")

fun registerRagTools(
    server: Server,
    buildPipeline: (PipelineConfig) -> PipelineExecutor,
    sessionLogger: SessionLogger? = null,
) {
    registerSearchCodebase(server, buildPipeline, sessionLogger)
}

private fun registerSearchCodebase(
    server: Server,
    buildPipeline: (PipelineConfig) -> PipelineExecutor,
    sessionLogger: SessionLogger? = null,
) {
    server.addTool(
        name = RagToolNames.SEARCH_CODEBASE,
        description = "Поиск по кодовой базе Android-проекта. Возвращает релевантные фрагменты кода, сгруппированные по классам. Используй для вопросов об архитектуре, реализации классов, use cases, DI-компонентах.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Поисковый запрос: вопрос, имя класса или метода"))
                })
            },
            required = listOf("query")
        )
    ) { request ->
        val query = request.arguments?.get("query")?.let {
            (it as? JsonPrimitive)?.content
        } ?: return@addTool CallToolResult(
            content = listOf(TextContent(text = "Параметр query обязателен")),
            isError = true
        )

        val pipelineConfig = PipelineConfig()
        sessionLogger?.logSearchStart(query, null, null)

        val pipeline = buildPipeline(pipelineConfig)
        var ctx = runBlocking { pipeline.execute(query) }

        // Fallback: identical to /search endpoint
        if (ctx.results.isEmpty() && pipelineConfig.postRerankThreshold > 0.0) {
            val fallbackPipeline = buildPipeline(pipelineConfig.copy(enableQueryOptimize = false))
            val fallbackCtx = runBlocking { fallbackPipeline.execute(ctx.originalQuery) }
            if (fallbackCtx.results.isNotEmpty()) {
                ctx = fallbackCtx
            }
        }

        if (ctx.packed == null || ctx.packed.groups.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    text = "НЕДОСТАТОЧНО_КОНТЕКСТА: по запросу \"$query\" ничего не найдено. " +
                        "Уточните запрос или проверьте что индекс содержит нужный код."
                ))
            )
        }

        val contextText = ContextFormatter.format(ctx.packed)
        val droppedResults = if (ctx.resultsAfterRerank.isNotEmpty()) {
            ctx.resultsAfterRerank.drop(ctx.results.size)
        } else emptyList()
        sessionLogger?.logSearchFinish(ctx.results, droppedResults, contextText)

        CallToolResult(content = listOf(TextContent(text = contextText)))
    }
}

//private fun registerSearchCodebaseFixed(server: Server, searchService: SearchService, topK: Int) {
//    server.addTool(
//        name = RagToolNames.SEARCH_CODEBASE_FIXED,
//        description = "Альтернативный поиск по кодовой базе с нарезкой фиксированного размера. " +
//            "Используй если пользователь явно попросил fixed поиск. " +
//            "Может возвращать фрагменты без чётких границ функций.",
//        inputSchema = ToolSchema(
//            properties = buildJsonObject {
//                put("query", buildJsonObject {
//                    put("type", JsonPrimitive("string"))
//                    put("description", JsonPrimitive("Поисковый запрос: вопрос, имя класса или метода"))
//                })
//            },
//            required = listOf("query")
//        )
//    ) { request ->
//        val query = request.arguments?.get("query")?.let {
//            (it as? JsonPrimitive)?.content
//        } ?: return@addTool CallToolResult(
//            content = listOf(TextContent(text = "Параметр query обязателен")),
//            isError = true
//        )
//
//        val results = runBlocking { searchService.search(query, "fixed", topK) }
//        if (results.isEmpty()) {
//            return@addTool CallToolResult(
//                content = listOf(TextContent(
//                    text = "Ничего не найдено в индексе (fixed). " +
//                        "Проверьте статус индекса через get_index_status."
//                ))
//            )
//        }
//
//        val text = ContextFormatter.formatFlat(results)
//        CallToolResult(content = listOf(TextContent(text = text)))
//    }
//}

//private fun registerSearchCodebaseSmart(
//    server: Server,
//    twoStageSearchService: TwoStageSearchService,
//    contextPacker: ContextPacker,
//    topK: Int,
//    queryOptimizer: QueryOptimizer? = null,
//    sessionLogger: SessionLogger? = null,
//) {
//    server.addTool(
//        name = RagToolNames.SEARCH_CODEBASE_SMART,
//        description = "Умный двухэтапный поиск по кодовой базе: " +
//                "архитектура, реализация классов, use cases, репозитории, DI-компоненты. " +
//                "Возвращает логически завершённые блоки кода (функции, классы), сгруппированные по классам. " +
//                "Передавай вопрос на естественном языке или название класса/метода.",
//        inputSchema = ToolSchema(
//            properties = buildJsonObject {
//                put("query", buildJsonObject {
//                    put("type", JsonPrimitive("string"))
//                    put("description", JsonPrimitive("Концептуальный вопрос или описание фичи"))
//                })
//            },
//            required = listOf("query")
//        )
//    ) { request ->
//        val query = request.arguments?.get("query")?.let {
//            (it as? JsonPrimitive)?.content
//        } ?: return@addTool CallToolResult(
//            content = listOf(TextContent(text = "Параметр query обязателен")),
//            isError = true
//        )
//
//        println("[Search] >>> RAW QUERY: '${query.take(80)}' | optimizer: ${if (queryOptimizer != null) "enabled" else "disabled"}")
//        sessionLogger?.logSearchStart(originalQuery = query, taskStateJson = null, history = null)
//
//        val (results, searchQuery) = runBlocking {
//            val sq = if (queryOptimizer != null) {
//                println("[Search] Optimize START query='${query.take(80)}'")
//                val optimized = queryOptimizer.optimize(query)
//                println("[Search] Optimize DONE: '${query.take(80)}' → '${optimized.take(120)}'")
//                optimized
//            } else {
//                println("[Search] Optimize SKIPPED (queryOptimizer=null) — check TRANSLATE_QUERIES env var")
//                query
//            }
//            twoStageSearchService.search(sq, topK * 2) to sq
//        }
//        if (results.isEmpty()) {
//            return@addTool CallToolResult(
//                content = listOf(TextContent(
//                    text = "Ничего не найдено. Убедитесь что индекс метаданных создан (EXTRACT_METADATA=true)."
//                ))
//            )
//        }
//
//        val packed = contextPacker.pack(results)
//        val contextText = ContextFormatter.format(packed)
//        sessionLogger?.logSearchFinish(
//            finalResults = results.take(topK),
//            droppedResults = results.drop(topK),
//            contextText = contextText,
//        )
//        CallToolResult(content = listOf(TextContent(text = contextText)))
//    }
//}

//private fun registerGetIndexStatus(server: Server, db: CodeDatabase) {
//    server.addTool(
//        name = RagToolNames.GET_INDEX_STATUS,
//        description = "Проверь статус индекса кодовой базы перед поиском. " +
//            "Возвращает количество проиндексированных чанков по каждой стратегии и дату индексации. " +
//            "Если is_ready = false — индекс не готов и поиск не даст результатов.",
//        inputSchema = ToolSchema(properties = buildJsonObject {})
//    ) { _ ->
//        val stats = db.getStats()
//        val text = buildString {
//            appendLine("Статус индекса кодовой базы:")
//            appendLine("- Готов: ${stats.isReady}")
//            appendLine("- Всего чанков: ${stats.totalChunks}")
//            appendLine("- Структурная стратегия: ${stats.structuralChunks} чанков")
//            appendLine("- Фиксированная стратегия: ${stats.fixedChunks} чанков")
//            append("- Последняя индексация: ${stats.indexedAt ?: "никогда"}")
//        }
//        CallToolResult(content = listOf(TextContent(text = text)))
//    }
//}
