package com.example.day.aigateway.api.routes

import com.example.day.aigateway.middleware.ConcurrencyLimiter
import com.example.day.aigateway.llm.LlmProvider
import com.example.day.shared.dto.ChatCompletionRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ChatRoutes(
    private val llmProvider: LlmProvider,
    private val concurrencyLimiter: ConcurrencyLimiter
) {
    fun Application.configureRoutes() {
        routing {
            rateLimit(RateLimitName("api")) {
                post("/v1/chat/completions") {
                    try {
                        val request = call.receive<ChatCompletionRequest>()
                        val response = concurrencyLimiter.withLimit { llmProvider.chat(request) }
                            ?: return@post call.respond(
                                HttpStatusCode.TooManyRequests,
                                mapOf("error" to "Too many concurrent requests")
                            )
                        call.respond(response)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "unknown")))
                    }
                }
                get("/v1/models") {
                    try {
                        val models = concurrencyLimiter.withLimit { llmProvider.models() }
                            ?: return@get call.respond(
                                HttpStatusCode.TooManyRequests,
                                mapOf("error" to "Too many concurrent requests")
                            )
                        call.respond(mapOf("data" to models))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "unknown")))
                    }
                }
            }
        }
    }
}
