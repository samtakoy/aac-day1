package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import javax.inject.Inject

/**
 * Factory for creating [AIAgent] instances.
 * Resolves AgentConfig, selects the appropriate ContextStrategy, and wires all dependencies.
 */
class AIAgentFactory @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val strategyFactory: StrategyFactory,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase
) {
    suspend fun getOrCreate(
        systemName: String,
        isCommonAgent: Boolean,
        chat: Chat
    ): AIAgent {
        val config = getOrCreateAgentUseCase(
            systemName = systemName,
            isCommon = isCommonAgent,
            chatSettings = chat.settings
        )
        val strategy = strategyFactory.create(config.contextStrategyType)
        val memoryProvider = memoryProviderFactory.create(
            memoryTypes = config.memoryTypes,
            chatId = chat.id,
            chatGroupId = chat.chatGroup.id
        )
        return AIAgent(
            config,
            contextRepository,
            llmRequestUseCase,
            strategy,
            memoryProvider
        )
    }
}
