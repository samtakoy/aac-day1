package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import javax.inject.Inject

class SetupStrategyUseCase @Inject constructor(
    private val agentRepository: AgentRepository,
    private val contextRepository: AgentContextRepository,
    private val migrator: ContextStrategyMigrator
) {
    suspend fun execute(
        agent: AgentConfig,
        targetStrategy: CtxStrategyType,
        params: AContextParams
    ): SetupResult {
        val currentStrategy = agent.contextStrategyType

        // Same strategy - just update params
        if (currentStrategy == targetStrategy) {
            contextRepository.saveContextParams(agent.id, params)
            return SetupResult.Updated(targetStrategy, params)
        }

        // Different strategy - migrate messages
        val messages = migrator.extractMessages(agent.id)
        val updatedAgent = agent.copy(contextStrategyType = targetStrategy)

        agentRepository.updateAgent(updatedAgent)
        contextRepository.saveContextParams(agent.id, params)

        val initialState = migrator.createInitialState(targetStrategy, messages, params)
        contextRepository.saveContextState(agent.id, initialState)

        val messagesMigrated = when (initialState) {
            is com.example.day.core.core_features.agent.domain.model.AContextState.SlidingWindow ->
                initialState.messages.size
            is com.example.day.core.core_features.agent.domain.model.AContextState.StickyFacts ->
                initialState.messages.size
            is com.example.day.core.core_features.agent.domain.model.AContextState.Summary ->
                initialState.messages.size
            else -> messages.size
        }

        return SetupResult.Migrated(
            from = currentStrategy,
            to = targetStrategy,
            messagesMigrated = messagesMigrated,
            totalMessages = messages.size
        )
    }
}
