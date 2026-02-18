package com.example.day.features.console.impl.domain

import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelResult
import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    suspend fun exec(
        promptText: String,
        history: List<ChatMessage>,
        chatSettings: ChatSettings
    ): Result<String> {
        val request = ModelRequest(
            model = DEFAULT_MODEL,
            messages = buildList {
                // Системный промт
                getSystemPromtOrNull(chatSettings)?.let { add(it) }
                // Предыдущая история
                addAll(
                    history.map { chatMessage ->
                        chatMessage.mapToRequestMessage()
                    }
                )
                // Текущее сообщение
                add(
                    ModelRequest.Message(
                        role = ModelRequest.Role.User,
                        content = promptText,
                        thinking = null,
                        cachePrompt = false
                    )
                )
            },
            responseFormat = if (chatSettings.jsonFormat) ModelRequest.ResponseFormat.JsonObject else ModelRequest.ResponseFormat.None,
            maxTokens = chatSettings.maxTokens.takeIf { it > 0 },
            stop = if (chatSettings.stopWord.isNotBlank()) listOf(chatSettings.stopWord) else null
        )
        val result = repository.sendRequest(request)
        return mapResult(result)
    }

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

    private fun getSystemPromtOrNull(chatSettings: ChatSettings): ModelRequest.Message? {
        return if (chatSettings.systemPromt.isNotBlank()) {
             ModelRequest.Message(
                role = ModelRequest.Role.System,
                content = chatSettings.systemPromt,
                thinking = null,
                cachePrompt = true
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
                val messageContent = result.choices.firstOrNull()?.message?.content
                Result.success(messageContent ?: "Empty response")
            }
        }
    }

    companion object {
        // z-ai/glm-4.5-air:free
        // upstage/solar-pro-3:free
        // stepfun/step-3.5-flash:free
        const val DEFAULT_MODEL = "stepfun/step-3.5-flash:free"

    }
}