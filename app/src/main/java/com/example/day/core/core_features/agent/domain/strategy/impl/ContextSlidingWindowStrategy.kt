package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Keeps only the N most recent messages (sliding window).
 * Window size is read from [AContextParams.SlidingWindow.windowSize].
 */
class ContextSlidingWindowStrategy : ContextStrategy {

    companion object {
        private const val DEFAULT_WINDOW = 20
    }

    override suspend fun process(
        agent: AgentConfig,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.SlidingWindow
            ?: AContextState.SlidingWindow(persistentListOf())
        val windowSize = (store.getContextParams(agent.id) as? AContextParams.SlidingWindow)
            ?.windowSize ?: DEFAULT_WINDOW
        val window = state.messages.takeLast(windowSize).toPersistentList()
        // НЕ добавляем userPrompt — это сделает LlmRequestUseCase.exec()
        return ContextSnapshot(messages = window)
    }

    override suspend fun afterResponse(
        agent: AgentConfig,
        response: String,
        store: AgentContextRepository,
        fullContext: ContextSnapshot
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.SlidingWindow
            ?: AContextState.SlidingWindow(persistentListOf())
        val windowSize = (store.getContextParams(agent.id) as? AContextParams.SlidingWindow)
            ?.windowSize ?: DEFAULT_WINDOW

        // fullContext содержит полную историю: initialHistory + prompt + assistant/tool messages
        val messagesToSave = fullContext.messages.toPersistentList()

        val trimmed = if (messagesToSave.size > windowSize) {
            messagesToSave.takeLast(windowSize).toPersistentList()
        } else {
            messagesToSave
        }
        store.saveContextState(agent.id, state.copy(messages = trimmed))
        return ContextStrategyResult(null)
    }

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val params = store.getContextParams(agent.id) as? AContextParams.SlidingWindow
        val state = store.getContextState(agent.id) as? AContextState.SlidingWindow
        val windowSize = params?.windowSize ?: DEFAULT_WINDOW
        return buildString {
            appendLine("Стратегия: sliding window")
            appendLine("Размер окна: $windowSize")
            append("Сообщений в контексте: ${state?.messages?.size ?: 0}")
        }
    }

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.SlidingWindow
        return buildString {
            appendLine("=== Агент: ${agent.systemName} ===")
            appendLine("Системный промпт: ${agent.systemPrompt}")
            if (!state?.messages.isNullOrEmpty()) {
                appendLine()
                appendLine("--- Сообщения в окне (${state!!.messages.size}) ---")
                state.messages.forEach { msg ->
                    appendLine("[${msg.role}] ${msg.content}")
                }
            }
        }
    }

    override suspend fun updateParams(
        agent: AgentConfig,
        params: Map<String, String>,
        store: AgentContextRepository
    ): String {
        val windowSize = params[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()
            ?: DEFAULT_WINDOW
        store.saveContextParams(agent.id, AContextParams.SlidingWindow(windowSize = windowSize))
        return "Размер окна обновлён: $windowSize сообщений"
    }
}
