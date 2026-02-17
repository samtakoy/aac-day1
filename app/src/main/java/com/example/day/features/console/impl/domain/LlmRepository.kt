package com.example.day.features.console.impl.domain

import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelResult

internal interface LlmRepository {
    suspend fun sendRequest(request: ModelRequest): ModelResult
}