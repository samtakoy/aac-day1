package com.example.day.core.core_features.llm.data.remote

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject

internal class LocalLlmApiImpl @Inject constructor(
    private val client: HttpClient
) : LocalLlmApi {
    override suspend fun sendRequest(request: ChatCompletionRequest, serverUrl: String): ChatCompletionResponse {
        val response = client.post("$serverUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error("ai-gateway error ${response.status}: ${response.bodyAsText()}")
        }
        return response.body()
    }
}
