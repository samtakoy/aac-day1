package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

/**
 * Request data class for LLM execution with tool calling.
 * Consolidates multiple parameters into a single object for cleaner API.
 *
 * @param initialHistory История сообщений из БД (БЕЗ memoryMessages)
 * @param memoryMessages Сообщения от MemoryProvider (только для LLM запроса, НЕ сохраняются)
 * @param prompt Исходный запрос пользователя
 * @param systemPrompt Системный промпт (одинаковый для всех итераций)
 * @param modelSettings Настройки модели для LLM запросов
 * @param tools Список доступных инструментов
 * @param context Контекст выполнения инструментов
 */
data class LlmExecutionRequest(
    val initialHistory: List<ModelRequest.Message>,
    val memoryMessages: List<AContextMessage>,
    val prompt: AContextMessage,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>,
    val context: ToolCallContext
) {
    companion object {
        const val DEFAULT_MAX_TOOL_LOOPS = 10
    }
}
