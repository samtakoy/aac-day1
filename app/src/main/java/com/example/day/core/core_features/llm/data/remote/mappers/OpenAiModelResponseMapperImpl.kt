package com.example.day.core.core_features.llm.data.remote.mappers

import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.shared.dto.ChatCompletionResponse
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

internal class OpenAiModelResponseMapperImpl @Inject constructor() {
    fun toDomain(response: ChatCompletionResponse): ModelResult {
        val choice = response.choices.firstOrNull()
            ?: return ModelResult.RuntimeError("No choices in response")
        return ModelResult.Success(
            id = response.id,
            model = response.model ?: "",
            choices = persistentListOf(
                ModelResult.Success.Choice(
                    message = ModelResult.Success.Message(
                        role = choice.message.role,
                        content = choice.message.content,
                        reasoning = null,
                        toolCalls = null
                    ),
                    finishReason = choice.finishReason
                )
            ),
            usage = response.usage?.let { u ->
                ModelResult.Success.Usage(
                    promptTokens = u.promptTokens ?: 0,
                    completionTokens = u.completionTokens ?: 0,
                    totalTokens = u.totalTokens ?: 0,
                    cost = null,
                    costDetails = null
                )
            }
        )
    }
}
