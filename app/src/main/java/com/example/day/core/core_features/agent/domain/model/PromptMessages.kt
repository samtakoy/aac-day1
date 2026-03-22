package com.example.day.core.core_features.agent.domain.model

/**
 * Результат [com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider.appendUserPrompt].
 *
 * @property context Эфемерные сообщения — отправляются LLM, но НЕ сохраняются в историю.
 *                   Используются для передачи RAG-контекста, инструкций и т.п.
 * @property prompt  Исходный (или модифицированный) запрос пользователя — сохраняется в историю.
 */
data class PromptMessages(
    val context: List<AContextMessage> = emptyList(),
    val prompt: AContextMessage,
)
