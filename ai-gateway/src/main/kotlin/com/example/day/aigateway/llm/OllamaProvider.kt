package com.example.day.aigateway.llm

import com.example.day.aigateway.config.AiGatewayConfig
import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OllamaProvider(
    private val client: HttpClient,
    private val ollamaUrl: String,
    private val modelInfoCache: OllamaModelInfoCache,
    private val config: AiGatewayConfig
) : LlmProvider {

    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        val numCtx = minOf(config.ollamaMaxContext, modelInfoCache.numCtxFor(request.model))
        val ollamaRequest = OllamaCompletionRequest(
            model = request.model,
            messages = request.messages,
            stream = request.stream,
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            numCtx = numCtx
        )
        val response = client.post("$ollamaUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ollamaRequest)
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Ollama error ${response.status}: $body")
        }
        return response.body()
    }

    override suspend fun models(): List<String> {
        val response: JsonObject = client.get("$ollamaUrl/api/tags").body()
        return response["models"]?.jsonArray
            ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
            ?: emptyList()
    }
}
