package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessage
import com.example.day.core.core_features.agent.domain.tools.AssistantToolCall
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolCallSession
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolCallingResult
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.llm.domain.model.getContent
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

/**
 * Реализация ToolCallOrchestrator.
 * Управляет циклом tool calling согласно спецификации OpenRouter.
 */
class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase,
    private val toolProvider: ToolProvider
) : ToolCallOrchestrator {

    private companion object {
        private const val TAG = "ToolCallOrchestrator(ktor)"
        private const val MAX_TOOL_LOOPS = 3
    }

    override suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,  // ← НОВОЕ: только для LLM запроса
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> {
        // initialHistory — это история из БД (БЕЗ memoryMessages)
        // memoryMessages добавляются к initialHistory только для LLM запроса

        // messages — полная история для LLM (memoryMessages + initialHistory + новые)
        val messages = (memoryMessages.map { it.toModelRequestMessage() } + initialHistory).toMutableList()

        // newMessages — сообщения для сохранения в БД (initialHistory + prompt + новые)
        val newMessages = mutableListOf<ModelRequest.Message>()
        
        // Добавляем initialHistory — это старая история из БД, которую нужно сохранить
        newMessages.addAll(initialHistory)
        
        // Добавляем prompt пользователя (сообщение, которое инициировало этот запрос)
        if (prompt.content?.isBlank() == false) {
            messages.add(prompt.toModelRequestMessage())
            newMessages.add(prompt.toModelRequestMessage())
        }

        val sessions = mutableListOf<ToolCallSession>()
        var loopIndex = 0
        var lastLlmResult: ModelResult.Success? = null

        while (loopIndex < MAX_TOOL_LOOPS) {
            // 1. Запрос к LLM
            val llmResult = llmProvider.askLlm(
                model = modelSettings,
                prompt = null,
                systemPrompt = systemPrompt,
                history = messages,
                tools = tools.ifEmpty { null },
                onEvent = onEvent
            ).getOrElse { error ->
                Log.e(TAG, "LLM request failed on iteration $loopIndex", error)
                return Result.failure(error)
            }

            lastLlmResult = llmResult
            val choice = llmResult.choices.firstOrNull()
            val toolCalls = choice?.message?.toolCalls

            // 2. Если нет tool calls — это финальный ответ
            if (toolCalls.isNullOrEmpty()) {
                Log.d(TAG, "Tool loop: нет tool calls, возвращаем ответ (итерация $loopIndex)")
                return Result.success(
                    ToolCallingResult(
                        finalResponseText = llmResult.getContent(),
                        toolCallSessions = sessions,
                        allMessages = newMessages.toAContextMessages()  // ← ТОЛЬКО новые сообщения
                    )
                )
            }

            Log.d(TAG, "Tool loop: получено ${toolCalls.size} tool calls (итерация $loopIndex)")

            // 3. Сохраняем assistant message с tool_calls (спецификация OpenRouter)
            val assistantMessage = ModelRequest.Message(
                role = ModelRequest.Role.Assistant,
                content = choice.message.content,
                toolCalls = toolCalls.map { call ->
                    ModelRequest.ToolCall(
                        id = call.id,
                        type = call.type,
                        function = ModelRequest.FunctionCall(
                            name = call.function.name,
                            arguments = call.function.arguments
                        )
                    )
                }
            )
            messages.add(assistantMessage)
            newMessages.add(assistantMessage)  // ← Синхронно добавляем в оба массива

            // 4. Выполняем tool calls и собираем результаты
            val toolResults = mutableListOf<ToolResult>()
            val toolMessages = mutableListOf<ModelRequest.Message>()
            var hasError = false

            for (call in toolCalls) {
                // 4.1. Уведомляем о начале вызова
                onEvent?.invoke(
                    WorkerEvent.ToolCallStarted(
                        toolCallId = call.id,
                        toolName = call.function.name,
                        arguments = call.function.arguments
                    )
                )

                Log.d(TAG, "tool call ${call.function}, ${call.id}")
                // 4.2. Выполняем инструмент — используем ModelResult.Success.ToolCall
                val toolResult = toolProvider.executeToolCall(call, context)
                val content = toolResult.getOrElse { error ->
                    val errorText = error.message ?: ToolCallingConstants.UNKNOWN_TOOL_ERROR
                    "${ToolCallingConstants.MCP_TOOL_ERROR_PREFIX}: $errorText"
                }

                toolMessages.add(
                    ModelRequest.Message(
                        role = ModelRequest.Role.Tool,
                        content = content,
                        toolCallId = call.id,  // КРИТИЧНО: tool_call_id
                        // TODO нет имени функции
                    )
                )
                toolResults.add(
                    ToolResult(
                        toolCallId = call.id,
                        content = content,
                        isError = toolResult.isFailure
                    )
                )
                // 4.4. Уведомляем о завершении вызова
                onEvent?.invoke(
                    WorkerEvent.ToolCallFinished(
                        toolCallId = call.id,
                        toolName = call.function.name,
                        result = content,
                        isError = toolResult.isFailure
                    )
                )

                Log.d(
                    TAG,
                    "Tool call '${call.function.name}': ${if (toolResult.isFailure) "ERROR" else "OK"}"
                )

                // 4.5. Остановка при ошибке (не прерываем сразу, добавляем в результаты)
                if (toolResult.isFailure) {
                    hasError = true
                }
            }

            // Добавляем tool messages в историю
            messages.addAll(toolMessages)
            newMessages.addAll(toolMessages)  // ← Синхронно добавляем в оба массива

            // Сохраняем сессию
            sessions.add(
                ToolCallSession(
                    assistantMessage = AssistantToolCall(
                        content = choice.message.content,
                        toolCalls = toolCalls
                    ),
                    toolResults = toolResults
                )
            )

            // При ошибке инструмента — НЕ прерываем цикл досрочно.
            // Следующая итерация даёт LLM возможность сформировать финальный ответ с объяснением ошибки.
            if (hasError) {
                Log.w(TAG, "Tool loop: ошибка инструмента на итерации $loopIndex, продолжаем для получения финального ответа")
            }

            loopIndex++
        }

        // Достигнут лимит итераций
        Log.d(TAG, "Tool loop: достигнут лимит итераций ($MAX_TOOL_LOOPS)")
        return Result.success(
            ToolCallingResult(
                finalResponseText = lastLlmResult?.getContent().orEmpty(),
                toolCallSessions = sessions,
                allMessages = newMessages.toAContextMessages()  // ← ТОЛЬКО новые сообщения
            )
        )
    }

    /**
     * Маппинг ModelRequest.Message → AContextMessage
     */
    private fun List<ModelRequest.Message>.toAContextMessages(): List<AContextMessage> {
        return map { msg ->
            AContextMessage(
                role = when (msg.role) {
                    ModelRequest.Role.System -> AContextMessage.Role.SYSTEM
                    ModelRequest.Role.User -> AContextMessage.Role.USER
                    ModelRequest.Role.Assistant -> AContextMessage.Role.ASSISTANT
                    ModelRequest.Role.Tool -> AContextMessage.Role.TOOL
                },
                content = msg.content,
                toolCallId = msg.toolCallId,
                toolCalls = msg.toolCalls?.map { call ->
                    AContextMessage.ToolCallRef(
                        id = call.id,
                        type = call.type,
                        functionName = call.function.name,
                        arguments = call.function.arguments
                    )
                }
            )
        }
    }
}
