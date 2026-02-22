package com.example.day.features.console.impl.di

import com.example.day.features.console.api.AgentsConsoleFeatureEntry
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.impl.AgentsConsoleFeatureEntryImpl
import com.example.day.features.console.impl.ConsoleFeatureEntryIml
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ConsoleFeatureApiModule {
    @Binds
    fun bindFeatureEntry(impl: ConsoleFeatureEntryIml): ConsoleFeatureEntry
    @Binds
    fun bindAgentsFeatureEntry(impl: AgentsConsoleFeatureEntryImpl): AgentsConsoleFeatureEntry
}