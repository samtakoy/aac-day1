package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.data.AgentContextRepositoryImpl
import com.example.day.core.core_features.agent.data.AgentRepositoryImpl
import com.example.day.core.core_features.agent.data.repository.AgentMemoryRepositoryImpl
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.state_machine.domain.StateStore
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskStateStoreImpl
import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.agent.domain.workers.tools.AgentToolsImpl
import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.tools.ChatToolsImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module for Agent feature dependency injection.
 *
 */
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
    fun bindsChatTools(impl: ChatToolsImpl): ChatTools

    /**
     * TODO это будет работать только пока у нас 1 агент работающий с этим стором
     *  Если их станет больше, то они полезут в память друг к другу, т.к. [TaskStateStoreImpl] кеширует значения в памяти
     *  Должно быть один агент - один [StateStore]
     * */
    @Binds
    @Singleton
    fun bindsTaskStateStore(impl: TaskStateStoreImpl): StateStore

    companion object {

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
