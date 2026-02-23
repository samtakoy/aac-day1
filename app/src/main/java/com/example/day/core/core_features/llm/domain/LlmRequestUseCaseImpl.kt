package com.example.day.core.core_features.llm.domain

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.collections.immutable.ImmutableList
import javax.inject.Inject

internal class LlmRequestUseCaseImpl @Inject constructor(
    private val repository: LlmRepository
) : LlmRequestUseCase {

    override suspend fun exec(
        modelSettings: ModelSettings,
        systemPrompt: String?,
        messages: List<ModelRequest.Message>,
        promptText: String,
    ): Result<ModelResult.Success> {
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

    private fun String.reqUserMessage() = ModelRequest.Message(
        role = ModelRequest.Role.User,
        content = this,
        thinking = null,
        cachePrompt = false
    )

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

    private fun ModelSettings.reqResponseFormat(): ModelRequest.ResponseFormat =
        if (jsonFormat) ModelRequest.ResponseFormat.JsonObject else ModelRequest.ResponseFormat.None

    private fun ModelSettings.reqTemperatureOrNull(): Double? = temperature

    private fun ModelSettings.reqReasoning(): ModelRequest.Reasoning? {
        reasoningEffort ?: return null
        val reasoningEffort = reasoningEffort.trim()
        ModelRequest.Reasoning.entries.forEach {
            if (it.title.equals(reasoningEffort, ignoreCase = true)) return it
        }
        return null
    }

    private fun mapResult(result: ModelResult): Result<ModelResult.Success> {
        return when (result) {
            is ModelResult.Error -> {
                Result.failure(Exception("API Error: ${result.message}"))
            }
            is ModelResult.RuntimeError -> {
                Result.failure(Exception("API Error: ${result.message}"))
            }
            is ModelResult.Success -> {
                Result.success(result)
            }
        }
    }
}