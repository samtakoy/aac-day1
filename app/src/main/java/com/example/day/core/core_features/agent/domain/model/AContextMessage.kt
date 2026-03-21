package com.example.day.core.core_features.agent.domain.model

/**
 * Сообщение контекста агента.
 * Соответствует спецификации OpenRouter API.
 *
 * @property role Роль сообщения (SYSTEM, USER, ASSISTANT, TOOL)
 * @property content Содержание сообщения
 * @property toolCallId Идентификатор tool call (для role=TOOL). Соответствует tool_call_id в спецификации OpenRouter.
 * @property toolCalls Список вызовов инструментов (для role=ASSISTANT). Соответствует tool_calls в спецификации OpenRouter.
 */
data class AContextMessage(
    val role: Role,
    val content: String?,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCallRef>? = null
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    /**
     * Ссылка на вызов инструмента в сообщении assistant.
     * Соответствует tool_calls в спецификации OpenRouter.
     *
     * @property id Уникальный идентификатор вызова (tool_call_id)
     * @property type Тип инструмента (обычно "function")
     * @property functionName Имя функции
     * @property arguments JSON аргументы функции
     */
    data class ToolCallRef(
        val id: String,
        val type: String,
        val functionName: String,
        val arguments: String
    )
}