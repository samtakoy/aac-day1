package com.example.day.features.group_choice.impl.di

import com.example.day.features.group_choice.api.GroupChoiceFeatureEntry
import com.example.day.features.group_choice.impl.GroupChoiceFeatureEntryImpl
import dagger.Binds
import dagger.Module

@Module
interface GroupChoiceFeatureApiModule {
    @Binds
    fun bindFeatureEntry(impl: GroupChoiceFeatureEntryImpl): GroupChoiceFeatureEntry
}
