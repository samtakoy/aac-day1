package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

/**
 * Оркестратор tool calling цикла.
 * Реализует полный цикл согласно спецификации OpenRouter.
 *
 * Ответственность:
 * - Управление циклом запросов к LLM с tool calls
 * - Выполнение инструментов через ToolProvider
 * - Сбор полной истории сообщений (включая tool calls)
 * - Отправка событий через onEvent callback
 */
interface ToolCallOrchestrator {
    /**
     * Выполняет цикл tool calling.
     *
     * @param initialHistory   История сообщений из БД (БЕЗ memoryMessages и contextMessages)
     * @param memoryMessages   Сообщения от MemoryProvider.getMemoryContext() — только для LLM, НЕ сохраняются
     * @param contextMessages  Эфемерный контекст от MemoryProvider.appendUserPrompt() — только для LLM, НЕ сохраняются
     * @param prompt           Запрос пользователя — сохраняется в историю
     * @param systemPrompt     Системный промпт (одинаковый для всех итераций)
     * @param modelSettings    Настройки модели для LLM запросов
     * @param tools            Список доступных инструментов
     * @param context          Контекст выполнения инструментов
     * @param onEvent          Callback для событий (ToolCallStarted, ToolCallFinished, etc.)
     * @return Результат с полной историей сообщений для сохранения в контекст
     */
    suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        contextMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult>
}
