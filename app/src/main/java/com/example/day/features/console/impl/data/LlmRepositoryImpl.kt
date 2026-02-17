package com.example.day.features.console.impl.data

import com.example.day.BuildConfig
import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.data.remote.model.request.ChatRequestDto
import com.example.day.features.console.impl.data.remote.model.request.MessageDto
import com.example.day.features.console.impl.data.remote.model.response.ChatResponseDto
import com.example.day.features.console.impl.data.remote.model.response.ErrorResponseDto
import com.example.day.features.console.impl.domain.LlmRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class LlmRepositoryImpl @Inject constructor(
    private val api: RemoteLlmApi
) : LlmRepository {
    override suspend fun sendRequest(prompt: String): Result<String> {
        return try {
            val result = api.sendRequest(
                request = ChatRequestDto(
                    model = DEFAULT_MODEL,
                    messages = listOf(
                        SYSTEM_PROMPT,
                        MessageDto(role = "user", content = prompt)
                    )
                ),
                apiKey = BuildConfig.LLM_API_KEY
            )
            when (result) {
                is ChatResponseDto -> {
                    val messageContent = result.choices.firstOrNull()?.message?.content
                    Result.success(messageContent ?: "Empty response")
                }
                is ErrorResponseDto -> {
                    Result.failure(Exception("API Error: ${result.error.message}"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "z-ai/glm-4.5-air:free"
        private val SYSTEM_PROMPT = MessageDto(role = "system", content = "Ты самый смешной шутник в мире и любишь эмодзи, но сильно нетрезв. После каждого ответа рассказываешь смешной случай из жизни.")
    }
}