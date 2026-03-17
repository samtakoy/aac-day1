package com.example.day.ragserver.search

import com.example.day.ragserver.indexing.LlmProvider

/**
 * Переводит поисковый запрос на английский если он написан на другом языке.
 * Используется перед поиском чтобы улучшить совпадение с английским кодом и метаданными.
 */
class QueryTranslator(private val llmProvider: LlmProvider) {

    // Эвристика определения языка: если более 30% букв — кириллица,
    // считаем запрос русским и отправляем на перевод.
    fun isNonEnglish(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        val cyrillicCount = letters.count { it in '\u0400'..'\u04FF' }
        return cyrillicCount.toDouble() / letters.length > 0.3
    }

    // Переводит запрос если он не на английском. Если английский — возвращает без изменений.
    suspend fun translateIfNeeded(query: String): String {
        if (!isNonEnglish(query)) {
            println("[Translate] Query is English — no translation: '$query'")
            return query
        }

        val translated = llmProvider.generate(
            "Translate the following search query to English. " +
                "Return ONLY the translated query, no explanations, no quotes.\n\nQuery: $query"
        )

        // Берём первую непустую строку ответа — LLM иногда добавляет пустые строки
        val result = translated.trim().lines().firstOrNull { it.isNotBlank() } ?: translated.trim()
        println("[Translate] '$query' → '$result'")
        return result
    }
}
