package com.example.day.core.core_features.llm.data.remote.mappers

import com.example.day.core.core_features.llm.data.remote.model.request.ChatRequestDto
import com.example.day.core.core_features.llm.data.remote.model.request.MessageDto
import com.example.day.core.core_features.llm.data.remote.model.request.Reasoning
import com.example.day.core.core_features.llm.data.remote.model.request.ResponseFormatDto
import com.example.day.core.core_features.llm.domain.model.ModelRequest
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
                    cachePrompt = message.cachePrompt
                )
            },
            responseFormat = modelRequest.responseFormat.toDto(),
            stream = modelRequest.stream ?: false,
            maxCompletionTokens = modelRequest.maxCompletionTokens,
            maxTokens = modelRequest.maxTokens,
            temperature = modelRequest.temperature,
            topP = modelRequest.topP,
            topK = modelRequest.topK,
            stop = modelRequest.stopSequence?.toList(),
            presencePenalty = modelRequest.presencePenalty,
            frequencyPenalty = modelRequest.frequencyPenalty,
            reasoning = mapReasoning(modelRequest.reasoningEffort),
            seed = modelRequest.seed,
            logProbs = modelRequest.logProbs,
            topLogProbs = modelRequest.topLogProbs
        )
    }

    private fun mapReasoning(reasoning: ModelRequest.Reasoning?): Reasoning? {
        reasoning ?: return null
        return Reasoning(
            effort = when (reasoning) {
                ModelRequest.Reasoning.XHIGH -> "xhigh"
                ModelRequest.Reasoning.HIGH -> "high"
                ModelRequest.Reasoning.MEDIUM -> "medium"
                ModelRequest.Reasoning.LOW -> "low"
                ModelRequest.Reasoning.MINIMAL -> "minimal"
                ModelRequest.Reasoning.NONE -> "none"
            }
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
