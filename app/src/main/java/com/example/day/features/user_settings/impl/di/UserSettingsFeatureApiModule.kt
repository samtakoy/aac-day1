package com.example.day.features.user_settings.impl.di

import com.example.day.features.user_settings.api.UserSettingsFeatureEntry
import com.example.day.features.user_settings.impl.UserSettingsFeatureEntryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class UserSettingsFeatureApiModule {

    @Binds
    @Singleton
    abstract fun bindUserSettingsEntry(impl: UserSettingsFeatureEntryImpl): UserSettingsFeatureEntry
}
