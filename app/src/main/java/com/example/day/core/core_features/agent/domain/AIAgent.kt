package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
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
    private val memoryProvider: MemoryProvider    // Долговременная + Рабочая
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
        chat: ChatSettings,
        userPrompt: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        // 1. Получаем "знания" (LTM + Working Memory) в виде промптов
        val memoryMessages = memoryProvider.getMemoryContext()
        val snapshot = strategy.process(chat, config, userPrompt, contextRepository)
        // 3. Собираем итоговый пирог для LLM
        // Порядок: System Prompt -> Memory (LTM/Working) -> History -> User Prompt
        val history = (memoryMessages + snapshot.messages).toModelRequestMessages()

        val requestDebugInfo = buildRequestDebugInfo(config.systemPrompt, memoryMessages, userPrompt)

        return llmProvider.askLlm(
            // Модель теперь берется из агента, а не из чата. В чате - это только прототип для копирования.
            model = config.modelSettings,
            userPrompt = userPrompt,
            systemPrompt = config.systemPrompt,
            history = history,
            onEvent = onEvent
        ).map { result ->
            val responseText = result.getContent()
            val strategyResult = strategy.afterResponse(chat, config, userPrompt, responseText, contextRepository)
            AIAgentResult(responseText, strategyResult.reportMessage, requestDebugInfo)
        }
    }

    private fun buildRequestDebugInfo(
        systemPrompt: String?,
        memoryMessages: List<AContextMessage>,
        userPrompt: String
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
        appendLine("[USER]")
        append(userPrompt.trimEnd())
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
}

