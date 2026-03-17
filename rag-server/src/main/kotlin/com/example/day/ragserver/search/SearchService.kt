package com.example.day.ragserver.search

import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.ScoredChunk
import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.embedding.EmbeddingProvider

class SearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun search(
        query: String,
        strategy: String,
        topK: Int,
        useHybrid: Boolean = true,
    ): List<SearchResult> {
        val queryVector = embeddingProvider.embed(query)
        val allVectors = db.getAllVectors(strategy)

        if (allVectors.isEmpty()) return emptyList()

        return if (useHybrid) {
            allVectors
                .map { (chunk, vector) ->
                    ScoredChunk(
                        chunk = chunk,
                        embeddingScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble(),
                        keywordScore = KeywordScorer.score(query, chunk.content),
                    )
                }
                .sortedByDescending { it.finalScore }
                .take(topK)
                .map { SearchResult(it.chunk, it.finalScore.toFloat()) }
        } else {
            allVectors
                .map { (chunk, vector) ->
                    SearchResult(chunk, VectorMath.cosineSimilarity(queryVector, vector))
                }
                .sortedByDescending { it.score }
                .take(topK)
        }
    }
}
