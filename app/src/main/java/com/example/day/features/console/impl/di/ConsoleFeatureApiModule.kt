package com.example.day.features.console.impl.di

import com.example.day.features.console.api.AgentsConsoleFeatureEntry
import com.example.day.features.console.api.AssistantConsoleFeatureEntry
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.api.PlannerConsoleFeatureEntry
import com.example.day.features.console.api.RagConsoleFeatureEntry
import com.example.day.features.console.api.SupportConsoleFeatureEntry
import com.example.day.features.console.impl.AgentsConsoleFeatureEntryImpl
import com.example.day.features.console.impl.AssistantConsoleFeatureEntryImpl
import com.example.day.features.console.impl.ConsoleFeatureEntryIml
import com.example.day.features.console.impl.PlannerConsoleFeatureEntryImpl
import com.example.day.features.console.impl.RagConsoleFeatureEntryImpl
import com.example.day.features.console.impl.SupportConsoleFeatureEntryImpl
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ConsoleFeatureApiModule {
    @Binds
    fun bindFeatureEntry(impl: ConsoleFeatureEntryIml): ConsoleFeatureEntry
    @Binds
    fun bindAgentsFeatureEntry(impl: AgentsConsoleFeatureEntryImpl): AgentsConsoleFeatureEntry
    @Binds
    fun bindPlannerFeatureEntry(impl: PlannerConsoleFeatureEntryImpl): PlannerConsoleFeatureEntry
    @Binds
    fun bindRagFeatureEntry(impl: RagConsoleFeatureEntryImpl): RagConsoleFeatureEntry
    @Binds
    fun bindAssistantFeatureEntry(impl: AssistantConsoleFeatureEntryImpl): AssistantConsoleFeatureEntry
    @Binds
    fun bindSupportFeatureEntry(impl: SupportConsoleFeatureEntryImpl): SupportConsoleFeatureEntry
}