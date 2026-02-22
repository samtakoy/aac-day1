package com.example.day.core.core_features.llm.data.remote

import com.example.day.core.core_features.llm.data.remote.model.request.ChatRequestDto
import com.example.day.core.core_features.llm.data.remote.model.response.ChatResultDto

internal interface RemoteLlmApi {
    suspend fun sendRequest(
        request: ChatRequestDto,
        apiKey: String
    ): ChatResultDto
}