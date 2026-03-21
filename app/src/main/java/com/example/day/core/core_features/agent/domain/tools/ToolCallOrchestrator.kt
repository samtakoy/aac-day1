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
 * - Выполнение инструментов через ToolRegistry
 * - Сбор полной истории сообщений (включая tool calls)
 * - Отправка событий через onEvent callback
 */
interface ToolCallOrchestrator {
    /**
     * Выполняет цикл tool calling с использованием [LlmExecutionRequest].
     *
     * @param request Объект запроса с параметрами выполнения
     * @param onEvent Callback для событий (ToolCallStarted, ToolCallFinished, etc.)
     * @return Результат с полной историей сообщений для сохранения в контекст
     */
    suspend fun execute(
        request: LlmExecutionRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<ToolCallingResult>

    /**
     * Выполняет цикл tool calling.
     *
     * @param initialHistory История сообщений из БД (БЕЗ memoryMessages)
     * @param memoryMessages Сообщения от MemoryProvider (только для LLM запроса, НЕ сохраняются)
     * @param userPrompt Исходный запрос пользователя
     * @param systemPrompt Системный промпт (одинаковый для всех итераций)
     * @param modelSettings Настройки модели для LLM запросов
     * @param tools Список доступных инструментов
     * @param context Контекст выполнения инструментов
     * @param onEvent Callback для событий (ToolCallStarted, ToolCallFinished, etc.)
     * @return Результат с полной историей сообщений для сохранения в контекст
     * @deprecated Use [execute(request: LlmExecutionRequest, onEvent)] instead
     */
    @Deprecated(
        message = "Use execute(request, onEvent) instead",
        replaceWith = ReplaceWith(
            "execute(LlmExecutionRequest(initialHistory, memoryMessages, prompt, systemPrompt, modelSettings, tools, context), onEvent)"
        )
    )
    suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> = execute(
        LlmExecutionRequest(
            initialHistory = initialHistory,
            memoryMessages = memoryMessages,
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelSettings = modelSettings,
            tools = tools,
            context = context
        ),
        onEvent
    )
}
