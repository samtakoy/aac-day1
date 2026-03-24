package com.example.day.core.core_features.llm.data.remote

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

internal interface LocalLlmApi {
    suspend fun sendRequest(request: ChatCompletionRequest, serverUrl: String): ChatCompletionResponse
}
