package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

// MVP: always routes to Ollama
class LlmRouter(private val ollamaProvider: LlmProvider) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse =
        ollamaProvider.chat(request)
}
