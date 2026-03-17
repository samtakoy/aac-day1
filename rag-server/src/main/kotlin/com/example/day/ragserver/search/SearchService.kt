package com.example.day.ragserver.search

import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.embedding.EmbeddingProvider

class SearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun search(query: String, strategy: String, topK: Int): List<SearchResult> {
        val queryVector = embeddingProvider.embed(query)
        val allVectors = db.getAllVectors(strategy)

        if (allVectors.isEmpty()) return emptyList()

        return allVectors
            .map { (chunk, vector) ->
                SearchResult(chunk, VectorMath.cosineSimilarity(queryVector, vector))
            }
            .sortedByDescending { it.score }
            .take(topK)
    }
}
