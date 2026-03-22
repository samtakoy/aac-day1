package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.model.PromptMessages
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider

/**
 * Main AI Agent orchestrator.
 * Coordinates strategy, LLM, and context repository to process user messages.
 *
 * "Pure brain" — doesn't know about database or UI.
 * All persistence is delegated to ContextStrategy.
 */
class AIAgent(
    val config: AgentConfig,
    // TODO больше тут не актуально - нужно вынести в AgentContextMemoryProvider или AgentMessageHistoryProvider
    private val contextRepository: AgentContextRepository,
    private val llmProvider: LlmRequestUseCase,
    // TODO больше тут не актуально - нужно вынести в AgentContextMemoryProvider или AgentMessageHistoryProvider
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,    // Долговременная + Рабочая
    private val toolProvider: ToolProvider,
    private val orchestrator: ToolCallOrchestrator
) {

    /**
     * Process user message:
     * 1. strategy.process() — build context snapshot for LLM
     * 2. LLM call
     * 3. strategy.afterResponse() — persist messages, optional compression
     *
     * @return Result with response text and optional report message (e.g. compression stats)
     */
    suspend fun process(
        prompt: AContextMessage,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        // 1. Получаем "знания" (LTM + Working Memory) в виде промптов
        val memoryMessages = memoryProvider.getMemoryContext()
        val promptMessages = memoryProvider.appendUserPrompt(prompt)
        val snapshot = strategy.process(config, contextRepository)

        val result = orchestrator.execute(
            initialHistory = snapshot.messages.toModelRequestMessages(),
            memoryMessages = memoryMessages,
            contextMessages = promptMessages.context,
            prompt = promptMessages.prompt,
            systemPrompt = config.systemPrompt,
            modelSettings = config.modelSettings,
            tools = toolProvider.getTools(agentId = config.id),
            context = ToolCallContext(agentId = config.id),
            onEvent = onEvent
        )

        return result.map { toolCallingResult ->
            // 4. Сохраняем ПОЛНУЮ историю от LLM (включая tool calls, НО БЕЗ memoryMessages)
            val extendedSnapshot = snapshot.copy(messages = toolCallingResult.allMessages)

            // 5. Передаём расширенный контекст в strategy
            val strategyResult = strategy.afterResponse(
                agent = config,
                response = toolCallingResult.finalResponseText,
                store = contextRepository,
                fullContext = extendedSnapshot
            )

            val requestDebugInfo = buildRequestDebugInfo(config.systemPrompt, memoryMessages, promptMessages)
            AIAgentResult(toolCallingResult.finalResponseText, strategyResult.reportMessage, requestDebugInfo)
        }
    }

    /** Returns formatted info about current context strategy/params state. */
    suspend fun getInfo(): String = strategy.getInfoReport(config, contextRepository)

    /** Returns full context contents formatted for display. */
    suspend fun getFullContext(): String = strategy.getFullContextReport(config, contextRepository)

    /**
     * Update strategy parameters using a map of parameter names to values.
     * @return confirmation message
     */
    suspend fun setupParams(params: Map<String, String>): String =
        strategy.updateParams(config, params, contextRepository)

    private fun buildRequestDebugInfo(
        systemPrompt: String?,
        memoryMessages: List<AContextMessage>,
        promptMessages: PromptMessages,
    ): String = buildString {
        appendLine("=== LLM запрос (без истории) ===")
        if (!systemPrompt.isNullOrBlank()) {
            appendLine("[SYSTEM]")
            appendLine(systemPrompt.trimEnd())
            appendLine()
        }
        memoryMessages.forEach { msg ->
            appendLine("[MEMORY:${msg.role.name}]")
            appendLine(msg.content.trimEnd())
            appendLine()
        }
        promptMessages.context.forEach { msg ->
            appendLine("[${msg.role.name}:context]")
            appendLine(msg.content.trimEnd())
            appendLine()
        }
        appendLine("[${promptMessages.prompt.role.name}]")
        append(promptMessages.prompt.content.trimEnd())
    }
}
