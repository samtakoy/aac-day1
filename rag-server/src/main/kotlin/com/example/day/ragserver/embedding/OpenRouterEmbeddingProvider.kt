package com.example.day.ragserver.embedding

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class OpenRouterEmbeddingProvider(
    private val apiKey: String,
    private val model: String,
    private val httpClient: HttpClient,
) : EmbeddingProvider {

    override suspend fun embed(text: String): FloatArray {
        val response = httpClient.post("https://openrouter.ai/api/v1/embeddings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(OpenRouterEmbedRequest(model = model, input = text))
        }
        if (!response.status.value.toString().startsWith("2")) {
            error("OpenRouter embed failed: HTTP ${response.status.value}")
        }
        return response.body<OpenRouterEmbedResponse>().data.first().embedding.toFloatArray()
    }
}
