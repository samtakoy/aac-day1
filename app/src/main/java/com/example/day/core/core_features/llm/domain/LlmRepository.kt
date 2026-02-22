package com.example.day.core.core_features.llm.domain

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

internal interface LlmRepository {
    suspend fun sendRequest(request: ModelRequest): ModelResult
}