package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.workers.concrete.TaskWorker
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_store.StateStoreImpl
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.StateMemoryProviderFactory
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import com.example.day.core.core_features.state_machine.domain.StateStore
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object SupportStateMachineModule {

    @Provides
    @Named("support")
    @Singleton
    internal fun provideSupportStateStore(
        agentMemoryRepository: AgentMemoryRepository,
        agentContextRepository: AgentContextRepository,
        stateConfig: SupportStateConfig
    ): StateStore = StateStoreImpl(
        agentMemoryRepository = agentMemoryRepository,
        agentContextRepository = agentContextRepository,
        stateConfig = stateConfig.config
    )

    @Provides
    @Named("support")
    internal fun provideSupportWorker(
        aiAgentFactory: AIAgentFactory,
        chatTools: ChatTools,
        memoryProviderFactory: MemoryProviderFactory,
        stateMemoryProviderFactory: StateMemoryProviderFactory,
        contextRepository: AgentContextRepository,
        llmRequestUseCase: LlmRequestUseCase,
        strategyFactory: StrategyFactory,
        @Named("support") stateStore: StateStore,
        toolProvider: ToolProvider,
        toolCallOrchestrator: ToolCallOrchestrator
    ): TaskWorker = TaskWorker(
        aiAgentFactory = aiAgentFactory,
        chatTools = chatTools,
        memoryProviderFactory = memoryProviderFactory,
        stateMemoryProviderFactory = stateMemoryProviderFactory,
        contextRepository = contextRepository,
        llmRequestUseCase = llmRequestUseCase,
        strategyFactory = strategyFactory,
        stateStore = stateStore,
        toolProvider = toolProvider,
        toolCallOrchestrator = toolCallOrchestrator,
        agentName = "support_agent"
    )
}
