package com.example.day.features.console.impl.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщение в запросе
 *
 * @property role Роль: "system", "user", "assistant", или "tool"
 * @property content Содержимое сообщения
 * @property thinking Содержимое для моделей с extended thinking (например, o1, o1-mini, o3-mini)
 * @property cachePrompt Включить это сообщение в кэш промптов
 */
@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String,
    @SerialName("thinking")
    val thinking: String? = null,
    @SerialName("cache_prompt")
    val cachePrompt: Boolean? = null
)