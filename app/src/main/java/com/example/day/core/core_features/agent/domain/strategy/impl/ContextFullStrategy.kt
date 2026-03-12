package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** Keeps the full message history without any limits or compression. */
class ContextFullStrategy : ContextStrategy {

    override suspend fun process(
        agent: AgentConfig,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.Full
            ?: AContextState.Full(persistentListOf())
        // НЕ добавляем userPrompt — это сделает LlmRequestUseCase.exec()
        return ContextSnapshot(messages = state.messages)
    }

    override suspend fun afterResponse(
        agent: AgentConfig,
        response: String,
        store: AgentContextRepository,
        fullContext: ContextSnapshot
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.Full
            ?: AContextState.Full(persistentListOf())

        // fullContext содержит полную историю: initialHistory + prompt + assistant/tool messages
        val messagesToSave = fullContext.messages.toPersistentList()

        store.saveContextState(
            agent.id,
            state.copy(messages = messagesToSave)
        )
        return ContextStrategyResult(null)
    }

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.Full
        return "Стратегия: full context (без ограничений)\nСообщений в контексте: ${state?.messages?.size ?: 0}"
    }

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.Full
        return buildString {
            appendLine("=== Агент: ${agent.systemName} ===")
            appendLine("Системный промпт: ${agent.systemPrompt}")
            if (!state?.messages.isNullOrEmpty()) {
                appendLine()
                appendLine("--- Сообщения (${state!!.messages.size}) ---")
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
    ): String = "Full context стратегия не использует лимиты"
}
