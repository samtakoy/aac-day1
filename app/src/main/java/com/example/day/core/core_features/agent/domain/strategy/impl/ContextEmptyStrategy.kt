package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings

/** Stateless strategy — no history is kept or sent to LLM. */
class ContextEmptyStrategy : ContextStrategy {

    override suspend fun process(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        store: AgentContextRepository
    ): ContextSnapshot = ContextSnapshot(messages = emptyList())

    override suspend fun afterResponse(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        response: String,
        store: AgentContextRepository
    ): ContextStrategyResult = ContextStrategyResult(null)

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String = "Стратегия: empty (без контекста)"

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String = "Стратегия empty: контекст не сохраняется"

    override suspend fun updateParams(
        agent: AgentConfig,
        params: Map<String, String>,
        store: AgentContextRepository
    ): String = "Empty стратегия не имеет параметров"
}