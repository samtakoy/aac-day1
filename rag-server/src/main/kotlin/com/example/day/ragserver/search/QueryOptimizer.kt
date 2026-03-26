package com.example.day.ragserver.search

import com.example.day.ragserver.indexing.LlmProvider
import com.example.day.ragserver.logging.SessionLogger

/**
 * Оптимизирует поисковый запрос для semantic search по Kotlin кодовой базе.
 *
 * [optimize] — базовая оптимизация без контекста.
 * [optimizeWithContext] — умный rewrite с учётом TaskState и Short History:
 *   - Если запрос абстрактный ("как это работает?") → использует current_focus из TaskState
 *   - Если context_switched=true → не использует старый фокус
 *   - Всегда переводит на английский и добавляет технические ключевые слова
 *
 * Активируется через TRANSLATE_QUERIES=true (env) + enable_query_optimize=true (query param).
 */
class QueryOptimizer(
    private val llmProvider: LlmProvider,
    private val sessionLogger: SessionLogger? = null,
) {

    /** Базовая оптимизация без контекста (обратная совместимость). */
    suspend fun optimize(query: String): String =
        optimizeWithContext(query, history = null)

    /**
     * Оптимизация с учётом контекста задачи.
     * @param taskState JSON строка TaskState (может быть null или "{}")
     * @param history краткая история диалога (пары USER/ASSISTANT, может быть null)
     */
    suspend fun optimizeWithContext(
        query: String,
        history: String?,
    ): String {
        val hasContext = !history.isNullOrBlank()

        val prompt = if (hasContext) {
            buildContextAwarePrompt(query, history)
        } else {
            buildBasePrompt(query)
        }

        val response = llmProvider.generate(prompt)
        var result = response.trim()

        // Validate output is English — some LLMs (Qwen etc.) occasionally respond in Chinese/other languages.
        // Non-ASCII > 10% of output → fallback to base prompt without context.
        if (!isEnglish(result)) {
            println("[QueryOptimizer] Non-English output detected ('${result.take(40)}'), falling back to base prompt")
            val fallback = llmProvider.generate(buildBasePrompt(query))
            result = fallback.trim().lines().firstOrNull { it.isNotBlank() } ?: query
        }

        println("[QueryOptimizer] '$query' → '$result' (context: $hasContext, model: ${llmProvider.modelName})")

        sessionLogger?.logQueryOptimize(
            originalQuery = query,
            prompt = prompt,
            rawResponse = response,
            optimizedQuery = result,
            model = llmProvider.modelName,
        )

        return result
    }

    private fun isEnglish(text: String): Boolean {
        val nonAscii = text.count { it.code > 127 }
        return nonAscii.toDouble() / text.length.coerceAtLeast(1) < 0.1
    }

    private fun buildBasePrompt(query: String): String = """
        Your goal: rewrite the user's query for semantic search over a Kotlin codebase.

        RULES:
        1. Translate to English
        2. Add technical keywords (class, function, interface, repository, use case, etc.)
        3. Make the query self-contained and specific
        4. Remove filler words

        CRITICAL: Return ONLY the rewritten query. No explanation, no quotes, no notes.

        Query: $query
    """.trimIndent()

    private fun buildContextAwarePrompt(
        query: String,
        history: String?,
    ): String = """
        Your goal: rewrite the user's query for semantic search over a Kotlin codebase,
        using conversation history for better precision.

        PRIORITY RULES:
        1. If the query TEXT LITERALLY CONTAINS a compound word that looks like a class/method name (e.g. "wholehistory", "toolregistry", "aiagentbuilder") → translate to English, preserve that name, do NOT inject focus context. DO NOT invent class names not present in the query text.
        2. If the query is abstract ("how does it work?", "show me") AND its topic clearly overlaps with current_focus → enrich with current_focus.
        3. If the query introduces a NEW concept/topic not related to current_focus → IGNORE focus, treat as a fresh query.
        4. If context_switched=true → DO NOT use the old focus.
        5. Always translate to English.
        6. Always add technical keywords (class, function, interface, etc.).

        CRITICAL: NEVER invent class names, file names, or identifiers not explicitly present in the original query.
        CRITICAL: Return ONLY the rewritten query. No explanation, no quotes, no parenthetical notes. One line.

        Examples:
        - "wholehistory example" → "wholehistory" is in query → Rule 1 → output: WholeHistory class implementation Kotlin
        - "как агент работает с историей сообщений" → no class name → output: AI agent message history management Kotlin
        - "как конфигурируется агент" → no class name → output: AI agent configuration Kotlin class

        ---
        RECENT HISTORY:
        ${history?.takeIf { it.isNotBlank() } ?: "(no history yet)"}

        ---
        USER QUERY: $query
    """.trimIndent()
}
