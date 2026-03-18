package com.example.day.ragserver.search

import com.example.day.ragserver.indexing.LlmProvider

/**
 * Оптимизирует поисковый запрос для semantic search по Kotlin кодовой базе.
 *
 * Выполняет полный query rewrite:
 * 1. Делает запрос самодостаточным и конкретным
 * 2. Переводит на английский (если запрос не на английском)
 * 3. Убирает мусорные слова
 * 4. Добавляет технические ключевые слова (class, function, interface, repository, etc.)
 *
 * Оптимизирует запросы на любом языке — английские тоже выигрывают
 * от добавления технических ключевых слов.
 *
 * Активируется через TRANSLATE_QUERIES=true (env) + enable_query_optimize=true (query param).
 * Если TRANSLATE_QUERIES=false — шаг пропускается с логом в консоль.
 */
class QueryOptimizer(private val llmProvider: LlmProvider) {

    suspend fun optimize(query: String): String {
        val response = llmProvider.generate(
            """
            You optimize search queries for semantic search over a Kotlin codebase.

            1. Make the query self-contained and specific
            2. Translate to English if not already in English
            3. Remove filler words
            4. Add relevant technical keywords (class, function, interface, repository, use case, etc.)

            Return ONLY the optimized query, no explanations, no quotes.

            Query: $query
            """.trimIndent()
        )
        val result = response.trim().lines().firstOrNull { it.isNotBlank() } ?: response.trim()
        println("[QueryOptimizer] '$query' → '$result'")
        return result
    }
}
