package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject

class RagSearchRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient
) : RagSearchRepository {

    override suspend fun search(query: String, serverUrl: String): Result<String> = runCatching {
        val response = httpClient.get("${serverUrl.trimEnd('/')}/search") {
            parameter("query", query)
        }
        response.bodyAsText()
    }
}
