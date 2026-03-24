package com.example.day.core.core_features.llm.data.remote.mappers

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.Message
import javax.inject.Inject

internal class OpenAiModelRequestMapperImpl @Inject constructor() {
    fun toDto(request: ModelRequest): ChatCompletionRequest = ChatCompletionRequest(
        model = request.model,
        messages = request.messages.map { msg ->
            Message(
                role = when (msg.role) {
                    ModelRequest.Role.System -> "system"
                    ModelRequest.Role.User -> "user"
                    ModelRequest.Role.Assistant -> "assistant"
                    ModelRequest.Role.Tool -> "tool"
                },
                content = msg.content
            )
        },
        temperature = request.temperature,
        maxTokens = request.maxTokens ?: request.maxCompletionTokens,
        stream = false
    )
}
