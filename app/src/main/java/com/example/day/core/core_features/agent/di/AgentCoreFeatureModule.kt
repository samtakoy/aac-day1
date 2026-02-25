package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.data.AgentRepositoryImpl
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.data.local.mapper.AgentContextMapper
import com.example.day.core.core_features.agent.data.local.mapper.AgentMapper
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.chat.domain.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module for Agent feature dependency injection.
 */
@Module
internal interface AgentCoreFeatureModule {
    
    @Binds
    fun bindsAgentRepository(impl: AgentRepositoryImpl): AgentRepository
    
    companion object {
        
        @Provides
        @Singleton
        internal fun provideAgentDao(db: com.example.day.core.core_features.chat.data.local.ChatDatabase): AgentDao = db.agentDao()
        
        @Provides
        @Singleton
        internal fun provideAgentToChatDao(db: com.example.day.core.core_features.chat.data.local.ChatDatabase): AgentToChatDao = db.agentToChatDao()
        
        @Provides
        @Singleton
        internal fun provideAgentContextMemoryDao(db: com.example.day.core.core_features.chat.data.local.ChatDatabase): AgentContextMemoryDao = db.agentContextMemoryDao()
        
        @Provides
        internal fun provideAgentMapper(): AgentMapper = AgentMapper()
        
        @Provides
        internal fun provideAgentContextMapper(): AgentContextMapper = AgentContextMapper()
    }
}
