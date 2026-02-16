package com.example.day.core.di

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
interface CoreModule {
    companion object {
        @Provides
        @Singleton // ОБЯЗАТЕЛЬНО: чтобы не плодить клиенты
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
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
                    level = LogLevel.BODY
                }
            }
        }
    }
}