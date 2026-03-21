package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.data.AgentContextRepositoryImpl
import com.example.day.core.core_features.agent.data.AgentRepositoryImpl
import com.example.day.core.core_features.agent.data.ExecutionSessionManagerImpl
import com.example.day.core.core_features.agent.data.ExecutionSessionRepositoryImpl
import com.example.day.core.core_features.agent.data.repository.AgentMemoryRepositoryImpl
import com.example.day.core.core_features.agent.data.tools.DefaultConfirmationHandler
import com.example.day.core.core_features.agent.data.tools.McpToolRegistry
import com.example.day.core.core_features.agent.data.tools.ToolCallManagerImpl
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.tools.ConfirmationHandler
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionManager
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionRepository
import com.example.day.core.core_features.agent.domain.tools.ToolCallManager
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolRegistry
import com.example.day.core.core_features.agent.domain.tools.impl.ToolCallOrchestratorImpl
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskStateStoreImpl
import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.agent.domain.workers.tools.AgentToolsImpl
import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.tools.ChatToolsImpl
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.state_machine.domain.StateStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    includes = [
        CommandHandlerModule::class,
        BranchingStrategyModule::class,
        TaskStateMachineModule::class
    ]
)
internal interface AgentCoreFeatureModule {

    @Binds
    fun bindsAgentRepository(impl: AgentRepositoryImpl): AgentRepository

    @Binds
    fun bindsAgentContextRepository(impl: AgentContextRepositoryImpl): AgentContextRepository

    @Binds
    fun bindsAgentMemoryRepository(impl: AgentMemoryRepositoryImpl): AgentMemoryRepository

    @Binds
    fun bindsAgentTools(impl: AgentToolsImpl): AgentTools

    @Binds
    fun bindsToolRegistry(impl: McpToolRegistry): ToolRegistry

    @Binds
    fun bindsChatTools(impl: ChatToolsImpl): ChatTools

    @Binds
    fun bindsConfirmationHandler(impl: DefaultConfirmationHandler): ConfirmationHandler

    @Binds
    fun bindsToolCallManager(impl: ToolCallManagerImpl): ToolCallManager

    @Binds
    fun bindsExecutionSessionRepository(impl: ExecutionSessionRepositoryImpl): ExecutionSessionRepository

    @Binds
    fun bindsExecutionSessionManager(impl: ExecutionSessionManagerImpl): ExecutionSessionManager

    @Binds
    @Singleton
    fun bindsTaskStateStore(impl: TaskStateStoreImpl): StateStore

    companion object {

        @Provides
        @Singleton
        internal fun provideToolCallOrchestrator(
            llmRequestUseCase: LlmRequestUseCase,
            executionSessionManager: ExecutionSessionManager,
            toolCallManager: ToolCallManager
        ): ToolCallOrchestrator = ToolCallOrchestratorImpl(
            llmProvider = llmRequestUseCase,
            executionSessionManager = executionSessionManager,
            toolCallManager = toolCallManager
        )

        @Provides
        @Singleton
        internal fun provideAgentDao(db: ChatDatabase): AgentDao = db.agentDao()

        @Provides
        @Singleton
        internal fun provideAgentMemoryDao(db: ChatDatabase): AgentMemoryDao = db.agentMemoryDao()

        @Provides
        @Singleton
        internal fun provideAgentToChatDao(db: ChatDatabase): AgentToChatDao = db.agentToChatDao()

        @Provides
        @Singleton
        internal fun provideAgentContextMemoryDao(db: ChatDatabase): AgentContextMemoryDao = db.agentContextMemoryDao()
    }
}
