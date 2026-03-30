package com.example.day.aigateway.llm

import com.example.day.shared.dto.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OllamaCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    @SerialName("num_ctx") val numCtx: Int
)
