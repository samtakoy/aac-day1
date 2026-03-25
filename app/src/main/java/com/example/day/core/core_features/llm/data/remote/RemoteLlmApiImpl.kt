package com.example.day.core.core_features.llm.data.remote

import android.util.Log
import com.example.day.core.core_features.llm.data.remote.model.request.ChatRequestDto
import com.example.day.core.core_features.llm.data.remote.model.response.ChatResponseDto
import com.example.day.core.core_features.llm.data.remote.model.response.ChatResultDto
import com.example.day.core.core_features.llm.data.remote.model.response.ErrorResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

internal class RemoteLlmApiImpl @Inject constructor(
    private val client: HttpClient
) : RemoteLlmApi {
    override suspend fun sendRequest(
        request: ChatRequestDto,
        apiKey: String
    ): ChatResultDto {
        Log.e("ktor", "Remote LLM")
        val response = client.post(END_POINT) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("HTTP-Referer", HTTP_REFERER)
            header("X-Title", X_TITLE)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<ChatResponseDto>()
        } else {
            response.body<ErrorResponseDto>()
        }
    }

    companion object {
        private const val END_POINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val HTTP_REFERER = "https://github.com"
        private const val X_TITLE = "My Android App"
    }
}