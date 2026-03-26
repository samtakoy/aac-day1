package com.example.day.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<Message>,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("temperature")
    val temperature: Double? = null
)

@Serializable
data class ChatCompletionResponse(
    @SerialName("id")
    val id: String,
    @SerialName("model")
    val model: String = "",
    @SerialName("choices")
    val choices: List<Choice>,
    @SerialName("usage")
    val usage: Usage? = null
)

@Serializable
data class Message(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class Choice(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("message")
    val message: Message,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)
