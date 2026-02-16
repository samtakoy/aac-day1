package com.example.day.features.console.impl.domain

internal interface LlmRepository {
    suspend fun sendRequest(prompt: String): Result<String>
}