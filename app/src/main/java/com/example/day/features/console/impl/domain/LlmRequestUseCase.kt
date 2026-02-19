package com.example.day.features.console.impl.domain

import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelRequest.ResponseFormat
import com.example.day.features.console.impl.domain.model.ModelResult
import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {

    suspend fun exec(
        model: String,
        systemPrompt: String?,
        messages: List<ModelRequest.Message>,
        promptText: String,
        responseFormat: ResponseFormat = ResponseFormat.None,
        maxTokens: Int? = null,
        stopSequence: List<String>? = null
    ): Result<String> {
        val request = ModelRequest(
            model = model,
            messages = buildList {
                // Системный промт
                systemPrompt.toSystemPromptOrNull()?.let { systemMessage ->
                    add(systemMessage)
                }
                // Предыдущая история
                addAll(messages)
                // Текущее сообщение
                add(promptText.toUserMessage())
            },
            responseFormat = responseFormat,
            maxTokens = maxTokens,
            stop = stopSequence
        )
        val result = repository.sendRequest(request)
        return mapResult(result)
    }

    suspend fun exec(
        promptText: String,
        history: List<ChatMessage>,
        chatSettings: ChatSettings
    ): Result<String> {
        return exec(
            model = ModelConst.DEFAULT_MODEL,
            systemPrompt = chatSettings.systemPromt,
            messages = history.map { chatMessage ->
                chatMessage.mapToRequestMessage()
            },
            promptText = promptText,
            if (chatSettings.jsonFormat) ModelRequest.ResponseFormat.JsonObject else ModelRequest.ResponseFormat.None,
            maxTokens = chatSettings.maxTokens.takeIf { it > 0 },
            stopSequence = if (chatSettings.stopWord.isNotBlank()) listOf(chatSettings.stopWord) else null
        )
    }

    private fun String.toUserMessage() = ModelRequest.Message(
        role = ModelRequest.Role.User,
        content = this,
        thinking = null,
        cachePrompt = false
    )

    private fun ChatMessage.mapToRequestMessage(): ModelRequest.Message = ModelRequest.Message(
        role = user.type.mapToRole(),
        content = text,
        thinking = null,
        cachePrompt = false
    )

    private fun UserType.mapToRole(): ModelRequest.Role =
        if (this == UserType.User) {
            ModelRequest.Role.User
        } else {
            ModelRequest.Role.Assistant
        }

    private fun String?.toSystemPromptOrNull(): ModelRequest.Message? {
        return if (this != null && isNotBlank()) {
            ModelRequest.Message(
                role = ModelRequest.Role.System,
                content = this,
                thinking = null,
                // TODO разобраться с параметром - как он влияет точно
                cachePrompt = false
            )
        } else {
            null
        }
    }

    private fun mapResult(result: ModelResult): Result<String> {
        return when (result) {
            is ModelResult.Error -> {
                Result.failure(Exception("API Error: ${result.message}"))
            }
            is ModelResult.RuntimeError -> {
                Result.failure(Exception("API Error: ${result.message}"))
            }
            is ModelResult.Success -> {
                val message = result.choices.firstOrNull()?.message
                if (message?.content?.isNotBlank() == true) {
                    Result.success(message.content)
                } else {
                    // Если content пуст, берем из размышлений
                    Result.success(message?.reasoning ?: "Empty response")
                }

            }
        }
    }

}