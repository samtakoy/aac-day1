package com.example.day.features.chats.impl.di

import com.example.day.core.feature_entries.FeatureEntry
import com.example.day.core.feature_entries.FeatureEntryKey
import com.example.day.features.chats.api.ChatsFeatureEntry
import com.example.day.features.chats.impl.ChatsFeatureEntryIml
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ChatsFeatureExportModule {
    @Binds
    @IntoMap
    @FeatureEntryKey(ChatsFeatureEntry::class)
    fun bindFeatureEntry(impl: ChatsFeatureEntryIml): FeatureEntry
}