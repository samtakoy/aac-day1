package com.example.day.features.console.impl.data.remote

import com.example.day.BuildConfig
import com.example.day.features.console.impl.data.remote.model.request.ChatRequestDto
import com.example.day.features.console.impl.data.remote.model.request.MessageDto
import com.example.day.features.console.impl.data.remote.model.response.ChatResponseDto
import com.example.day.features.console.impl.data.remote.model.response.ErrorResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class RemoteLlmApiImpl @Inject constructor(
    private val client: HttpClient
) : RemoteLlmApi {
    override suspend fun sendRequest(prompt: String, modelName: String): Result<String> {
        return try {
            val response = client.post(END_POINT) {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.LLM_API_KEY}")
                header("HTTP-Referer", HTTP_REFERER)
                header("X-Title", X_TITLE)
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequestDto(
                        model = modelName,
                        messages = listOf(
                            SYSTEM_PROMPT,
                            MessageDto(role = "user", content = prompt)
                        )
                    )
                )
            }
            return if (response.status.isSuccess()) {
                val successBody = response.body<ChatResponseDto>()
                val messageContent = successBody.choices.firstOrNull()?.message?.content
                Result.success(messageContent ?: "Empty response")
            } else {
                val errorBody = response.body<ErrorResponseDto>()
                Result.failure(Exception("API Error: ${errorBody.error.message}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val END_POINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val HTTP_REFERER = "http://localhost"
        private const val X_TITLE = "My Android App"
        private val SYSTEM_PROMPT = MessageDto(role = "system", content = "Ты самый смешной шутник в мире и любишь эмодзи, но сильно нетрезв. После каждого ответа рассказываешь смешной случай из жизни.")
    }
}