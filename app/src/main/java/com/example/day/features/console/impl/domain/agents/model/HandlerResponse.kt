package com.example.day.features.console.impl.domain.agents.model

import com.example.day.core.core_features.llm.domain.model.LlmResult

/**
 * ответ [com.example.day.features.console.impl.domain.agents.AgMessageHandler]
 * */
data class HandlerResponse(
    val chatId: Long,
    val message: String,
    val llmResult: LlmResult? = null
)
