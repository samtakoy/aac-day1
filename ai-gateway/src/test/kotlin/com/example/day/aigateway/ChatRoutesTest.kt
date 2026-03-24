package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.llm.LlmProvider
import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import com.example.day.shared.dto.Choice
import com.example.day.shared.dto.Message
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatRoutesTest {

    private val fakeProvider = object : LlmProvider {
        override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse =
            ChatCompletionResponse(
                id = "test-id",
                model = request.model,
                choices = listOf(Choice(message = Message("assistant", "Hello!"), finishReason = "stop"))
            )
    }

    @Test
    fun `POST chat completions returns 200 with assistant message`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            ChatRoutes(fakeProvider).apply { configureRoutes() }
        }
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"llama3","messages":[{"role":"user","content":"Hi"}]}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Hello!"))
    }

    @Test
    fun `GET models returns list`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            ChatRoutes(fakeProvider).apply { configureRoutes() }
        }
        val response = client.get("/v1/models")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("llama3"))
    }
}
