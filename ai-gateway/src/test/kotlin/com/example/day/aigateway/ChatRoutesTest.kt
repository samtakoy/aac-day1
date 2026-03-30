package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.llm.LlmProvider
import com.example.day.aigateway.middleware.ConcurrencyLimiter
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
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.testing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ChatRoutesTest {

    private val fakeProvider = object : LlmProvider {
        override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse =
            ChatCompletionResponse(
                id = "test-id",
                model = request.model,
                choices = listOf(Choice(message = Message("assistant", "Hello!"), finishReason = "stop"))
            )

        override suspend fun models(): List<String> = listOf("llama3", "mistral", "qwen2.5:7b")
    }

    private fun ApplicationTestBuilder.setupApp(concurrencyLimit: Int = 10) {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(RateLimit) {
                register(RateLimitName("api")) {
                    rateLimiter(limit = 1000, refillPeriod = 60.seconds)
                }
            }
            ChatRoutes(fakeProvider, ConcurrencyLimiter(concurrencyLimit)).apply { configureRoutes() }
        }
    }

    @Test
    fun `POST chat completions returns 200 with assistant message`() = testApplication {
        setupApp()
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"llama3","messages":[{"role":"user","content":"Hi"}]}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Hello!"))
    }

    @Test
    fun `GET models returns list`() = testApplication {
        setupApp()
        val response = client.get("/v1/models")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("llama3"))
    }

    @Test
    fun `POST chat completions returns 429 when concurrency limit exceeded`() = testApplication {
        val slowProvider = object : LlmProvider {
            override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
                delay(500)
                return ChatCompletionResponse(
                    id = "test-id",
                    model = request.model,
                    choices = listOf(Choice(message = Message("assistant", "ok")))
                )
            }
            override suspend fun models() = emptyList<String>()
        }
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(RateLimit) {
                register(RateLimitName("api")) {
                    rateLimiter(limit = 1000, refillPeriod = 60.seconds)
                }
            }
            ChatRoutes(slowProvider, ConcurrencyLimiter(1)).apply { configureRoutes() }
        }
        val results = coroutineScope {
            (1..2).map {
                async {
                    client.post("/v1/chat/completions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"model":"llama3","messages":[{"role":"user","content":"Hi"}]}""")
                    }.status
                }
            }.awaitAll()
        }
        assertTrue(results.contains(HttpStatusCode.TooManyRequests))
    }
}
