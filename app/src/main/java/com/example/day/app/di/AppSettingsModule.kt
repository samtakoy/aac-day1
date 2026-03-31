package com.example.day.app.di

import android.content.Context
import com.example.day.core.app_settings.AppSettings
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
object AppSettingsModule {
    @Provides
    @Singleton
    fun provideAppSettings(context: Context, json: Json): AppSettings = AppSettings(context, json)
}
