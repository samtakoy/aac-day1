package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

interface LlmProvider {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse
}
