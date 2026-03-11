package com.example.day.core.core_features.llm.data.remote.model.request

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
class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String,
    @SerialName("thinking")
    val thinking: String? = null,
    @SerialName("cache_prompt")
    val cachePrompt: Boolean? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)

@Serializable
class ToolCallDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
    @SerialName("function")
    val function: FunctionCallDto
)

@Serializable
class FunctionCallDto(
    @SerialName("name")
    val name: String,
    @SerialName("arguments")
    val arguments: String
)
