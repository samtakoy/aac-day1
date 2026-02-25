package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.data.AgentRepositoryImpl
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.domain.AgentRepository
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
 * Содержит привязки для:
 * - AgentTools / AgentToolsImpl
 * - ChatTools / ChatToolsImpl  
 * - Все Workers (SimpleWorker, StepWorker, etc.)
 */
@Module
internal interface AgentCoreFeatureModule {
    
    @Binds
    fun bindsAgentRepository(impl: AgentRepositoryImpl): AgentRepository
    
    @Binds
    fun bindsAgentTools(impl: AgentToolsImpl): AgentTools
    
    @Binds
    fun bindsChatTools(impl: ChatToolsImpl): ChatTools
    
    companion object {
        
        @Provides
        @Singleton
        internal fun provideAgentDao(db: ChatDatabase): AgentDao = db.agentDao()
        
        @Provides
        @Singleton
        internal fun provideAgentToChatDao(db: ChatDatabase): AgentToChatDao = db.agentToChatDao()
        
        @Provides
        @Singleton
        internal fun provideAgentContextMemoryDao(db: ChatDatabase): AgentContextMemoryDao = db.agentContextMemoryDao()
    }
}
