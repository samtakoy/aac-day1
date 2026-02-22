package com.example.day.features.chats.impl.di

import com.example.day.features.chats.api.ChatsFeatureEntry
import com.example.day.features.chats.impl.ChatsFeatureEntryIml
import dagger.Binds
import dagger.Module

@Module
interface ChatsFeatureApiModule {
    @Binds
    fun bindFeatureEntry(impl: ChatsFeatureEntryIml): ChatsFeatureEntry
}