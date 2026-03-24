package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.config.AiGatewayConfig
import com.example.day.aigateway.llm.LlmRouter
import com.example.day.aigateway.llm.OllamaProvider
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun main() {
    val config = AiGatewayConfig()
    val httpClient = HttpClient(OkHttp) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
        }
    }
    val ollamaProvider = OllamaProvider(httpClient, config.ollamaUrl)
    val router = LlmRouter(ollamaProvider)
    val chatRoutes = ChatRoutes(router)

    embeddedServer(Netty, port = config.port) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        with(chatRoutes) { configureRoutes() }
    }.start(wait = true)
}
