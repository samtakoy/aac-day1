package com.example.day.core.di

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
interface NetworkModule {
    companion object Companion {
        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            prettyPrint = true
        }

        @Provides
        @Singleton
        fun providesHttpClient(json: Json): HttpClient {
            return HttpClient(Android) {
                install(ContentNegotiation) {
                    json(json)
                }
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
                install(SSE)
                install(HttpTimeout) {
                    requestTimeoutMillis = 300_000  // 5 мин — для медленных локальных LLM
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 300_000
                }
            }
        }
    }
}
