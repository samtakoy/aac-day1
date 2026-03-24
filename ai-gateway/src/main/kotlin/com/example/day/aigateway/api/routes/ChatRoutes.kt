package com.example.day.aigateway.api.routes

import com.example.day.aigateway.llm.LlmProvider
import com.example.day.shared.dto.ChatCompletionRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ChatRoutes(private val llmProvider: LlmProvider) {
    fun Application.configureRoutes() {
        routing {
            post("/v1/chat/completions") {
                try {
                    val request = call.receive<ChatCompletionRequest>()
                    val response = llmProvider.chat(request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "unknown")))
                }
            }
            get("/v1/models") {
                call.respond(mapOf("data" to listOf("llama3", "mistral", "qwen2.5:7b")))
            }
        }
    }
}
