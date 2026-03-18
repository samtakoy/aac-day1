package com.example.day.ragserver.search.rerank

import com.example.day.ragserver.db.SearchResult

interface Reranker {
    /**
     * Переранжирует результаты поиска относительно запроса.
     * Получает все результаты после threshold-фильтра.
     * TopKStep отсекает финальный список — не здесь.
     */
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult>
}
