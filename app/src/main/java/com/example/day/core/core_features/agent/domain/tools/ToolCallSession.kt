package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Сессия tool calling — хранит пару request/response для каждого вызова.
 * Соответствует спецификации OpenRouter.
 *
 * @property assistantMessage Сообщение ассистента с tool_calls
 * @property toolResults Результаты выполнения инструментов
 */
data class ToolCallSession(
    val assistantMessage: AssistantToolCall,
    val toolResults: List<ToolResult>
)

/**
 * Сообщение assistant с tool_calls.
 *
 * @property content Содержание сообщения (может быть null если только tool_calls)
 * @property toolCalls Список вызовов инструментов
 */
data class AssistantToolCall(
    val content: String?,
    val toolCalls: List<ModelResult.Success.ToolCall>
)

/**
 * Результат выполнения инструмента.
 *
 * @property toolCallId Идентификатор вызова (соответствует tool_call_id в спецификации OpenRouter)
 * @property content Результат выполнения (строка)
 * @property isError Флаг ошибки выполнения
 */
data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean
)
