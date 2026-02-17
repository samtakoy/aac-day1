package com.example.day.app.di

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.day.core.core_features.chat.di.ChatCoreFeatureModule
import com.example.day.core.di.NetworkModule
import com.example.day.core.feature_entries.FeatureEntryProvider
import com.example.day.features.chats.impl.di.ChatsFeatureDeps
import com.example.day.features.chats.impl.di.ChatsFeatureExportModule
import com.example.day.features.console.impl.di.ConsoleFeatureDeps
import com.example.day.features.console.impl.di.ConsoleFeatureExportModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        NetworkModule::class,
        ChatCoreFeatureModule::class,
        ConsoleFeatureExportModule::class,
        ChatsFeatureExportModule::class,
    ]
)
@Immutable
interface AppComponent : FeatureEntryProvider, ConsoleFeatureDeps, ChatsFeatureDeps {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}

val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}

