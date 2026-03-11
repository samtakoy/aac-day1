package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage

/**
 * Результат выполнения tool calling цикла.
 * Содержит полную историю для сохранения в контекст.
 *
 * @property finalResponseText Финальный текстовый ответ от LLM
 * @property toolCallSessions История сессий tool calling (для отладки/анализа)
 * @property allMessages Все сообщения (включая tool calls) готовые для сохранения в AContext
 */
data class ToolCallingResult(
    val finalResponseText: String,
    val toolCallSessions: List<ToolCallSession>,
    val allMessages: List<AContextMessage>
)
