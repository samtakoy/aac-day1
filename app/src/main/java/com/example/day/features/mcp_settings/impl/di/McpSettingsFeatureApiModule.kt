package com.example.day.features.mcp_settings.impl.di

import com.example.day.features.mcp_settings.api.McpSettingsFeatureEntry
import com.example.day.features.mcp_settings.impl.McpSettingsFeatureEntryImpl
import dagger.Binds
import dagger.Module

@Module
interface McpSettingsFeatureApiModule {
    @Binds
    fun bindFeatureEntry(impl: McpSettingsFeatureEntryImpl): McpSettingsFeatureEntry
}
