package com.example.day.features.console.impl.data.remote.mappers

import com.example.day.features.console.impl.data.remote.model.request.ChatRequestDto
import com.example.day.features.console.impl.data.remote.model.request.MessageDto
import com.example.day.features.console.impl.data.remote.model.request.ResponseFormatDto
import com.example.day.features.console.impl.domain.model.ModelRequest
import javax.inject.Inject

internal interface ModelRequestMapper {
    fun toDto(modelRequest: ModelRequest): ChatRequestDto
}

internal class ModelRequestMapperImpl @Inject constructor(): ModelRequestMapper {

    override fun toDto(modelRequest: ModelRequest): ChatRequestDto {
        return ChatRequestDto(
            model = modelRequest.model,
            messages = modelRequest.messages.map { message ->
                MessageDto(
                    role = message.role.toDtoString(),
                    content = message.content,
                    thinking = message.thinking,
                    cachePrompt = if (message.cachePrompt) true else null
                )
            },
            responseFormat = modelRequest.responseFormat.toDto(),
            stream = modelRequest.stream ?: false,
            maxTokens = modelRequest.maxTokens,
            temperature = modelRequest.temperature,
            topP = modelRequest.topP,
            topK = modelRequest.topK,
            stop = modelRequest.stop?.toList(),
            presencePenalty = modelRequest.presencePenalty,
            frequencyPenalty = modelRequest.frequencyPenalty,
            seed = modelRequest.seed,
            logProbs = modelRequest.logProbs,
            topLogProbs = modelRequest.topLogProbs
        )
    }

    private fun ModelRequest.Role.toDtoString(): String {
        return when (this) {
            ModelRequest.Role.System -> "system"
            ModelRequest.Role.Assistant -> "assistant"
            ModelRequest.Role.User -> "user"
        }
    }

    private fun ModelRequest.ResponseFormat.toDto(): ResponseFormatDto? {
        return when (this) {
            ModelRequest.ResponseFormat.None -> null
            ModelRequest.ResponseFormat.JsonObject -> ResponseFormatDto(
                type = "json_object"
            )
            ModelRequest.ResponseFormat.JsonSchema -> ResponseFormatDto(
                type = "json_schema"
            )
        }
    }
}
