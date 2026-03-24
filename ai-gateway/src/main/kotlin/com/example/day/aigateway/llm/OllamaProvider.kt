package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class OllamaProvider(
    private val client: HttpClient,
    private val ollamaUrl: String
) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        val response = client.post("$ollamaUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error("Ollama error ${response.status}")
        }
        return response.body()
    }
}
