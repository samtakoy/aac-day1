package com.example.day.ragserver.search.rerank

import com.example.day.ragserver.db.SearchResult

/**
 * Heuristic reranker: добавляет bonus к score за keyword overlap между запросом и чанком.
 *
 * Bonus = (кол-во совпавших слов / кол-во слов запроса) * 0.1
 * Ограничен 0.1 чтобы не перекрыть embedding score.
 */
class HeuristicReranker : Reranker {

    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        if (queryWords.isEmpty()) return results

        return results
            .map { result ->
                val chunkWords = result.chunk.content.lowercase().split(Regex("\\W+")).toSet()
                val overlap = queryWords.intersect(chunkWords).size
                val bonus = (overlap.toDouble() / queryWords.size) * 0.1
                result.copy(score = (result.score + bonus).toFloat())
            }
            .sortedByDescending { it.score }
    }
}
