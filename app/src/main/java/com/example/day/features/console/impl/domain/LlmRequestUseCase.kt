package com.example.day.features.console.impl.domain

import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.features.console.impl.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelRequest.ResponseFormat
import com.example.day.features.console.impl.domain.model.ModelResult
import com.example.day.features.console.impl.domain.model.ModelSettings
import kotlinx.collections.immutable.ImmutableList
import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {

    suspend fun exec(
        modelSettings: ModelSettings,
        systemPrompt: String?,
        messages: List<ModelRequest.Message>,
        promptText: String,
    ): Result<String> {
        val request = ModelRequest(
            model = modelSettings.name,
            messages = buildList {
                // Системный промт
                systemPrompt.reqSystemPromptOrNull()?.let { systemMessage ->
                    add(systemMessage)
                }
                // Предыдущая история
                addAll(messages)
                // Текущее сообщение
                add(promptText.reqUserMessage())
            },
            responseFormat = modelSettings.reqResponseFormat(),
            maxTokens = modelSettings.reqMaxTokensOrNull(),
            temperature = modelSettings.reqTemperatureOrNull(),
            stopSequence = modelSettings.reqStopSequenceOrNull(),
            reasoningEffort = modelSettings.reqReasoning()
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
            modelSettings = chatSettings.model,
            systemPrompt = chatSettings.systemPromt,
            messages = history.map { chatMessage ->
                chatMessage.reqMessage()
            },
            promptText = promptText,
        )
    }

    private fun String.reqUserMessage() = ModelRequest.Message(
        role = ModelRequest.Role.User,
        content = this,
        thinking = null,
        cachePrompt = false
    )

    private fun ChatMessage.reqMessage(): ModelRequest.Message = ModelRequest.Message(
        role = user.type.reqRole(),
        content = text,
        thinking = null,
        cachePrompt = false
    )

    private fun UserType.reqRole(): ModelRequest.Role =
        if (this == UserType.User) {
            ModelRequest.Role.User
        } else {
            ModelRequest.Role.Assistant
        }

    private fun String?.reqSystemPromptOrNull(): ModelRequest.Message? {
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

    private fun ModelSettings.reqStopSequenceOrNull(): ImmutableList<String>? =
        stopSequence.ifEmpty { null }

    private fun ModelSettings.reqMaxTokensOrNull(): Int? =
        maxTokens.takeIf { it > 0 }

    private fun ModelSettings.reqResponseFormat(): ResponseFormat =
        if (jsonFormat) ResponseFormat.JsonObject else ResponseFormat.None

    private fun ModelSettings.reqTemperatureOrNull(): Double? = temperature

    private fun ModelSettings.reqReasoning(): ModelRequest.Reasoning? {
        reasoningEffort ?: return null
        val reasoningEffort = reasoningEffort.trim()
        ModelRequest.Reasoning.entries.forEach {
            if (it.title.equals(reasoningEffort, ignoreCase = true)) return it
        }
        return null
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