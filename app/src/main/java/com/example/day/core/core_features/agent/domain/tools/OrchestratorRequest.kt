package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

data class OrchestratorRequest(
    val messages: List<ModelRequest.Message>,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>
)
