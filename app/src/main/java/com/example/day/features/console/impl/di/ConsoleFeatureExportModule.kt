package com.example.day.features.console.impl.di

import com.example.day.core.feature_entries.FeatureEntry
import com.example.day.core.feature_entries.FeatureEntryKey
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.impl.ConsoleFeatureEntryIml
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ConsoleFeatureExportModule {
    @Binds
    @IntoMap
    @FeatureEntryKey(ConsoleFeatureEntry::class)
    fun bindFeatureEntry(impl: ConsoleFeatureEntryIml): FeatureEntry
}