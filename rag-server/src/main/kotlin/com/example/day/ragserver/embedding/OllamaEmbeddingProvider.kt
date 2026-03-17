package com.example.day.ragserver.embedding

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class OllamaEmbeddingProvider(
    private val baseUrl: String,
    private val model: String,
    private val httpClient: HttpClient,
) : EmbeddingProvider {

    override suspend fun embed(text: String): FloatArray {
        val response = httpClient.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(OllamaEmbedRequest(model = model, prompt = text))
        }
        if (!response.status.value.toString().startsWith("2")) {
            error("Ollama embed failed: HTTP ${response.status.value}")
        }
        return response.body<OllamaEmbedResponse>().embedding.toFloatArray()
    }
}
