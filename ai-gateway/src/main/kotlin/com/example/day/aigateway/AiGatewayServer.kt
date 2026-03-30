package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.config.AiGatewayConfig
import com.example.day.aigateway.llm.LlmRouter
import com.example.day.aigateway.llm.OllamaModelInfoCache
import com.example.day.aigateway.llm.OllamaProvider
import com.example.day.aigateway.middleware.ConcurrencyLimiter
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.ratelimit.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = AiGatewayConfig()
    val httpClient = HttpClient(OkHttp) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
            connectTimeoutMillis = 10_000
        }
    }
    val modelInfoCache = OllamaModelInfoCache(httpClient, config.ollamaUrl)
    val ollamaProvider = OllamaProvider(httpClient, config.ollamaUrl, modelInfoCache, config)
    val router = LlmRouter(ollamaProvider)
    val concurrencyLimiter = ConcurrencyLimiter(config.concurrencyLimit)
    val chatRoutes = ChatRoutes(router, concurrencyLimiter)

    embeddedServer(Netty, port = config.port) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(XForwardedHeaders)
        install(RateLimit) {
            register(RateLimitName("api")) {
                rateLimiter(limit = config.rateLimitRpm, refillPeriod = 60.seconds)
            }
        }
        with(chatRoutes) { configureRoutes() }
    }.start(wait = true)
}
