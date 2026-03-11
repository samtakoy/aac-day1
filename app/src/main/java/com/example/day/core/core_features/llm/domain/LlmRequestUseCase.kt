package com.example.day.core.core_features.llm.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.ModelSettings

interface LlmRequestUseCase {
    suspend fun exec(
        modelSettings: ModelSettings,
        systemPrompt: String?,
        messages: List<ModelRequest.Message>,
        prompt: AContextMessage?,
        tools: List<ModelRequest.Tool>? = null
    ): Result<ModelResult.Success>
}
