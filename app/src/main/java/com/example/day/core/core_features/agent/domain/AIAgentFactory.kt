package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.impl.AutoToolExecutor
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManager
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlToolExecutor
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.llm.domain.model.ModelSettings
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
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator,
    private val autoToolExecutor: AutoToolExecutor,
    private val hitlToolExecutor: HitlToolExecutor,
    private val hitlSessionManager: HitlSessionManager
) {
    suspend fun getOrCreate(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: (suspend (Long) -> Unit)? = null
    ): AIAgent {
        val config = getOrCreateAgentUseCase(
            systemName = systemName,
            chatId = chatId,
            systemPrompt = systemPrompt,
            defaultModel = defaultModel,
            defaultContext = defaultContext,
            onCreateCallback = onCreateCallback
        )
        val strategy = strategyFactory.create(config.contextStrategyType)
        val memoryProvider = memoryProviderFactory.create(
            memoryTypes = config.memoryTypes,
            agentId = config.id
        )
        val toolExecutor: ToolExecutor =
            if (config.hitlEnabled) hitlToolExecutor else autoToolExecutor
        return AIAgent(
            config = config,
            contextRepository = contextRepository,
            strategy = strategy,
            memoryProvider = memoryProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator,
            toolExecutor = toolExecutor,
            hitlSessionManager = hitlSessionManager
        )
    }
}
