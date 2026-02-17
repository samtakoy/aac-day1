package com.example.day.features.console.impl.domain

import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelResult
import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    suspend fun exec(promptText: String): Result<String> {
        val request = ModelRequest(
            model = DEFAULT_MODEL,
            messages = listOf(
                SYSTEM_PROMPT,
                ModelRequest.Message(
                    role = ModelRequest.Role.User,
                    content = promptText,
                    thinking = null,
                    cachePrompt = false
                )
            ),
            responseFormat = ModelRequest.ResponseFormat.None
        )
        val result = repository.sendRequest(request)
        return mapResult(result)
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
        const val DEFAULT_MODEL = "z-ai/glm-4.5-air:free"
        private val SYSTEM_PROMPT = ModelRequest.Message(
            role = ModelRequest.Role.System,
            content = "Ты самый смешной шутник в мире и любишь эмодзи, но сильно нетрезв. После каждого ответа рассказываешь смешной случай из жизни.",
            thinking = null,
            cachePrompt = true
        )
    }
}