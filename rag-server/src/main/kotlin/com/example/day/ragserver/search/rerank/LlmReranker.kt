package com.example.day.ragserver.search.rerank

import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.indexing.LlmProvider

/**
 * LLM-based reranker: локальная модель оценивает релевантность каждого чанка запросу.
 *
 * Промпт просит вернуть строки "Chunk N: X.XX" — простой формат устойчивый к лишнему тексту.
 * Парсинг через regex — не ломается если LLM добавляет пояснения.
 *
 * Чанки обрезаются до MAX_CHUNK_CHARS чтобы не превысить контекст модели.
 */
class LlmReranker(private val llmProvider: LlmProvider) : Reranker {

    companion object {
        private const val MAX_CHUNK_CHARS = 400
        private val SCORE_REGEX = Regex("Chunk (\\d+): ([\\d.]+)")
    }

    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) return results

        val prompt = buildPrompt(query, results)
        val response = llmProvider.generate(prompt)

        val scores = parseScores(response, results.size)
        if (scores.isEmpty()) {
            println("[LlmReranker] Failed to parse scores, returning original order")
            return results
        }

        return results
            .mapIndexed { index, result ->
                val llmScore = scores[index + 1]  // chunks are 1-indexed in prompt
                if (llmScore != null) result.copy(score = llmScore.toFloat()) else result
            }
            .sortedByDescending { it.score }
    }

    private fun buildPrompt(query: String, results: List<SearchResult>): String = buildString {
        appendLine("Rate the relevance of each code chunk to the search query.")
        appendLine("For each chunk output ONLY a line in format: \"Chunk N: X.XX\" (score 0.00 to 1.00)")
        appendLine()
        appendLine("Query: $query")
        appendLine()
        results.forEachIndexed { index, result ->
            appendLine("Chunk ${index + 1}:")
            appendLine(result.chunk.content.take(MAX_CHUNK_CHARS).trimEnd())
            appendLine()
        }
        appendLine("Scores:")
    }

    /** Парсит строки "Chunk N: X.XX" → Map<chunkIndex, score>. */
    private fun parseScores(response: String, count: Int): Map<Int, Double> {
        val scores = mutableMapOf<Int, Double>()
        SCORE_REGEX.findAll(response).forEach { match ->
            val index = match.groupValues[1].toIntOrNull() ?: return@forEach
            val score = match.groupValues[2].toDoubleOrNull() ?: return@forEach
            if (index in 1..count) scores[index] = score.coerceIn(0.0, 1.0)
        }
        return scores
    }
}
