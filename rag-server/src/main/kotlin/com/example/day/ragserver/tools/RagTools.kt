package com.example.day.ragserver.tools

import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.embedding.EmbeddingProvider
import com.example.day.ragserver.logging.SessionLogger
import com.example.day.ragserver.search.QueryOptimizer
import com.example.day.ragserver.search.SearchService
import com.example.day.ragserver.search.TwoStageSearchService
import com.example.day.ragserver.search.context.ContextFormatter
import com.example.day.ragserver.search.context.ContextPacker
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
    searchService: SearchService,
    db: CodeDatabase,
    topK: Int,
    embeddingProvider: EmbeddingProvider,
    queryOptimizer: QueryOptimizer? = null,
    sessionLogger: SessionLogger? = null,
) {
    val contextPacker = ContextPacker()
    val twoStageSearchService = TwoStageSearchService(db, embeddingProvider)
    //registerSearchCodebase(server, searchService, contextPacker, topK)
    //registerSearchCodebaseFixed(server, searchService, topK)
    registerSearchCodebaseSmart(server, twoStageSearchService, contextPacker, topK, queryOptimizer, sessionLogger)
    //registerGetIndexStatus(server, db)
}

private fun registerSearchCodebase(
    server: Server,
    searchService: SearchService,
    contextPacker: ContextPacker,
    topK: Int,
) {
    server.addTool(
        name = RagToolNames.SEARCH_CODEBASE,
        description = "Используй для поиска по внутренней кодовой базе Android-проекта: " +
            "архитектура, реализация классов, use cases, репозитории, DI-компоненты. " +
            "Возвращает логически завершённые блоки кода (функции, классы), сгруппированные по классам. " +
            "Передавай вопрос на естественном языке или название класса/метода.",
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

        val results = runBlocking { searchService.search(query, "structural", topK * 2) }
        if (results.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    text = "Ничего не найдено в индексе (structural). " +
                        "Проверьте статус индекса через get_index_status."
                ))
            )
        }

        val packed = contextPacker.pack(results)
        val text = ContextFormatter.format(packed)
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerSearchCodebaseFixed(server: Server, searchService: SearchService, topK: Int) {
    server.addTool(
        name = RagToolNames.SEARCH_CODEBASE_FIXED,
        description = "Альтернативный поиск по кодовой базе с нарезкой фиксированного размера. " +
            "Используй если пользователь явно попросил fixed поиск. " +
            "Может возвращать фрагменты без чётких границ функций.",
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

        val results = runBlocking { searchService.search(query, "fixed", topK) }
        if (results.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    text = "Ничего не найдено в индексе (fixed). " +
                        "Проверьте статус индекса через get_index_status."
                ))
            )
        }

        val text = ContextFormatter.formatFlat(results)
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerSearchCodebaseSmart(
    server: Server,
    twoStageSearchService: TwoStageSearchService,
    contextPacker: ContextPacker,
    topK: Int,
    queryOptimizer: QueryOptimizer? = null,
    sessionLogger: SessionLogger? = null,
) {
    server.addTool(
        name = RagToolNames.SEARCH_CODEBASE_SMART,
        description = "Умный двухэтапный поиск по кодовой базе: " +
                "архитектура, реализация классов, use cases, репозитории, DI-компоненты. " +
                "Возвращает логически завершённые блоки кода (функции, классы), сгруппированные по классам. " +
                "Передавай вопрос на естественном языке или название класса/метода.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Концептуальный вопрос или описание фичи"))
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

        println("[Search] >>> RAW QUERY: '${query.take(80)}' | optimizer: ${if (queryOptimizer != null) "enabled" else "disabled"}")
        sessionLogger?.logSearchStart(originalQuery = query, taskStateJson = null, history = null)

        val (results, searchQuery) = runBlocking {
            val sq = if (queryOptimizer != null) {
                println("[Search] Optimize START query='${query.take(80)}'")
                val optimized = queryOptimizer.optimize(query)
                println("[Search] Optimize DONE: '${query.take(80)}' → '${optimized.take(120)}'")
                optimized
            } else {
                println("[Search] Optimize SKIPPED (queryOptimizer=null) — check TRANSLATE_QUERIES env var")
                query
            }
            twoStageSearchService.search(sq, topK * 2) to sq
        }
        if (results.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    text = "Ничего не найдено. Убедитесь что индекс метаданных создан (EXTRACT_METADATA=true)."
                ))
            )
        }

        val packed = contextPacker.pack(results)
        val contextText = ContextFormatter.format(packed)
        sessionLogger?.logSearchFinish(
            finalResults = results.take(topK),
            droppedResults = results.drop(topK),
            contextText = contextText,
        )
        CallToolResult(content = listOf(TextContent(text = contextText)))
    }
}

private fun registerGetIndexStatus(server: Server, db: CodeDatabase) {
    server.addTool(
        name = RagToolNames.GET_INDEX_STATUS,
        description = "Проверь статус индекса кодовой базы перед поиском. " +
            "Возвращает количество проиндексированных чанков по каждой стратегии и дату индексации. " +
            "Если is_ready = false — индекс не готов и поиск не даст результатов.",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        val stats = db.getStats()
        val text = buildString {
            appendLine("Статус индекса кодовой базы:")
            appendLine("- Готов: ${stats.isReady}")
            appendLine("- Всего чанков: ${stats.totalChunks}")
            appendLine("- Структурная стратегия: ${stats.structuralChunks} чанков")
            appendLine("- Фиксированная стратегия: ${stats.fixedChunks} чанков")
            append("- Последняя индексация: ${stats.indexedAt ?: "никогда"}")
        }
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}
