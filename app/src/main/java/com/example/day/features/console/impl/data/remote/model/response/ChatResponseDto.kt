package com.example.day.features.console.impl.data.remote.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// TODO SerialName
// Модель успешного ответа
@Serializable
data class ChatResponseDto(
    val id: String,
    val model: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

@Serializable
data class ChoiceDto(
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String?
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String,
    val reasoning: String? = null // Для моделей с "мыслями"
)

@Serializable
data class UsageDto(
    @SerialName("total_tokens")
    val totalTokens: Int
)

// Модель ошибки (OpenRouter присылает её в поле "error")
@Serializable
data class ErrorResponseDto(
    val error: ErrorDetails
)

@Serializable
data class ErrorDetails(
    val message: String,
    val code: Int? = null,
    val metadata: String? = null
)
