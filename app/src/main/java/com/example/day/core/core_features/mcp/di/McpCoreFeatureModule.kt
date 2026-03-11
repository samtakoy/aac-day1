package com.example.day.core.core_features.mcp.di

import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.mcp.data.McpRepositoryImpl
import com.example.day.core.core_features.mcp.data.McpToolsImpl
import com.example.day.core.core_features.mcp.data.local.dao.McpServerDao
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module for the MCP core feature.
 * Uses the existing HttpClient and Json from NetworkModule — no duplicate binding.
 */
@Module
internal interface McpCoreFeatureModule {

    @Binds
    @Singleton
    fun bindsMcpRepository(impl: McpRepositoryImpl): McpRepository

    @Binds
    @Singleton
    fun bindsMcpTools(impl: McpToolsImpl): McpTools

    companion object {
        @Provides
        internal fun provideMcpServerDao(db: ChatDatabase): McpServerDao = db.mcpServerDao()
    }
}
