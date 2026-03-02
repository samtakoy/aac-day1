package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import kotlinx.collections.immutable.persistentListOf

/** Keeps the full message history without any limits or compression. */
class ContextFullStrategy : ContextStrategy {

    override suspend fun process(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.Full
            ?: AContextState.Full(persistentListOf())
        return ContextSnapshot(messages = state.messages.addUserMessage(userPrompt))
    }

    override suspend fun afterResponse(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        response: String,
        store: AgentContextRepository
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.Full
            ?: AContextState.Full(persistentListOf())
        store.saveContextState(
            agent.id,
            state.copy(
                messages = state.messages
                    .addUserMessage(userPrompt)
                    .addAssistantMessage(response)
            )
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
