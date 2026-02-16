package com.example.day.app.di

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.day.core.di.CoreModule
import com.example.day.core.feature_entries.FeatureEntryProvider
import com.example.day.features.console.impl.di.ConsoleFeatureDeps
import com.example.day.features.console.impl.di.ConsoleFeatureExportModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CoreModule::class, ConsoleFeatureExportModule::class])
@Immutable
interface AppComponent : FeatureEntryProvider, ConsoleFeatureDeps {

}

val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}

