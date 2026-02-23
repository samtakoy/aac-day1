package com.example.day.core.core_features.llm.domain

import com.example.day.core.core_features.llm.domain.model.LlmResult
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

interface LlmRequestUseCase {
    suspend fun exec(
        modelSettings: ModelSettings,
        systemPrompt: String?,
        messages: List<ModelRequest.Message>,
        promptText: String,
    ): Result<LlmResult>
}