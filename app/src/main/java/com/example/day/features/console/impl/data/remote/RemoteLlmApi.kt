package com.example.day.features.console.impl.data.remote

import com.example.day.features.console.impl.data.remote.model.request.ChatRequestDto
import com.example.day.features.console.impl.data.remote.model.response.ChatResultDto

internal interface RemoteLlmApi {
    suspend fun sendRequest(
        request: ChatRequestDto,
        apiKey: String
    ): ChatResultDto
}