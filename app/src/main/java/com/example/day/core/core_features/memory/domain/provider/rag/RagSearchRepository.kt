package com.example.day.core.core_features.memory.domain.provider.rag

interface RagSearchRepository {
    /**
     * Search codebase via rag-server REST endpoint.
     * @param query user query text
     * @param serverUrl base URL, e.g. "http://10.0.2.2:3001"
     * @return formatted search results or failure
     */
    suspend fun search(query: String, serverUrl: String): Result<String>
}
