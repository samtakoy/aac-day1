package com.example.day.ragserver.search.rerank

import com.example.day.ragserver.db.SearchResult

/** Null Object: реранкер-заглушка для RerankStrategy.NONE — возвращает результаты без изменений. */
class NoopReranker : Reranker {
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> = results
}
