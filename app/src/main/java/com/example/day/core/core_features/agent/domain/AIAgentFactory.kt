package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import javax.inject.Inject

/**
 * Factory for creating [AIAgent] instances.
 * Resolves AgentConfig, selects the appropriate ContextStrategy, and wires all dependencies.
 */
class AIAgentFactory @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val strategyFactory: StrategyFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase
) {
    suspend fun getOrCreate(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean,
        chatSettings: ChatSettings
    ): AIAgent {
        val config = getOrCreateAgentUseCase(
            systemName = systemName,
            isCommon = isCommonAgent,
            chatId = chatId,
            chatSettings = chatSettings
        )
        val strategy = strategyFactory.create(config.contextStrategyType)
        return AIAgent(config, contextRepository, llmRequestUseCase, strategy)
    }
}
