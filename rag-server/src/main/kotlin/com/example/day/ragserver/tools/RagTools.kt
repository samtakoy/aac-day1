package com.example.day.ragserver.tools

import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.formatHeader
import com.example.day.ragserver.search.SearchService
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun registerRagTools(server: Server, searchService: SearchService, db: CodeDatabase, topK: Int) {
    registerSearchCodebase(server, searchService, topK)
    registerSearchCodebaseFixed(server, searchService, topK)
    registerGetIndexStatus(server, db)
}

private fun registerSearchCodebase(server: Server, searchService: SearchService, topK: Int) {
    server.addTool(
        name = RagToolNames.SEARCH_CODEBASE,
        description = "Используй для поиска по внутренней кодовой базе Android-проекта: " +
            "архитектура, реализация классов, use cases, репозитории, DI-компоненты. " +
            "Возвращает логически завершённые блоки кода (функции, классы). " +
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

        val results = runBlocking { searchService.search(query, "structural", topK) }
        if (results.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    text = "Ничего не найдено в индексе (structural). " +
                        "Проверьте статус индекса через get_index_status."
                ))
            )
        }

        val separator = "\n${"=".repeat(60)}\n"
        val text = results.mapIndexed { i, r ->
            "[${i + 1}/${results.size}] ${r.chunk.formatHeader()} | score: ${"%.3f".format(r.score)}\n\n${r.chunk.content}"
        }.joinToString(separator)

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

        val separator = "\n${"=".repeat(60)}\n"
        val text = results.mapIndexed { i, r ->
            "[${i + 1}/${results.size}] ${r.chunk.formatHeader()} | score: ${"%.3f".format(r.score)}\n\n${r.chunk.content}"
        }.joinToString(separator)

        CallToolResult(content = listOf(TextContent(text = text)))
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
