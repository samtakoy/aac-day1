package com.example.day.features.console.impl.data.remote.mappers

import com.example.day.features.console.impl.data.remote.model.response.ChatResponseDto
import com.example.day.features.console.impl.data.remote.model.response.ChatResultDto
import com.example.day.features.console.impl.data.remote.model.response.ErrorResponseDto
import com.example.day.features.console.impl.domain.model.ModelResult
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal interface ModelResponseMapper {
    fun toDomain(chatResultDto: ChatResultDto): ModelResult
}

internal class ModelResponseMapperImpl @Inject constructor() : ModelResponseMapper {

    override fun toDomain(chatResultDto: ChatResultDto): ModelResult {
        return when (chatResultDto) {
            is ChatResponseDto -> responseToDomain(chatResultDto)
            is ErrorResponseDto -> errorToDomain(chatResultDto)
        }
    }

    private fun responseToDomain(chatResponseDto: ChatResponseDto): ModelResult.Success {
        return ModelResult.Success(
            id = chatResponseDto.id,
            model = chatResponseDto.model,
            choices = chatResponseDto.choices.map { choiceDto ->
                ModelResult.Success.Choice(
                    message = ModelResult.Success.Message(
                        role = choiceDto.message.role,
                        content = choiceDto.message.content,
                        reasoning = choiceDto.message.reasoning
                    ),
                    finishReason = choiceDto.finishReason
                )
            }.toImmutableList()
        )
    }

    private fun errorToDomain(errorResponseDto: ErrorResponseDto): ModelResult.Error {
        return ModelResult.Error(
            message = errorResponseDto.error.message,
            code = errorResponseDto.error.code,
            // metadata = errorResponseDto.error.metadata,
            param = errorResponseDto.error.param,
            type = errorResponseDto.error.type
        )
    }
}
