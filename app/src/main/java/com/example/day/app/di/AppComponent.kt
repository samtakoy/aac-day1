package com.example.day.app.di

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.day.core.core_features.agent.di.AgentCoreFeatureModule
import com.example.day.core.core_features.chat.di.ChatCoreFeatureModule
import com.example.day.core.core_features.llm.di.LlmCoreFeatureModule
import com.example.day.core.core_features.memory.di.MemoryCoreFeatureModule
import com.example.day.core.di.NetworkModule
import com.example.day.core.feature_entries.FeatureEntryProvider
import com.example.day.features.chats.impl.di.ChatsFeatureDeps
import com.example.day.features.chats.impl.di.ChatsFeatureApiModule
import com.example.day.features.console.impl.di.ConsoleFeatureDeps
import com.example.day.features.console.impl.di.ConsoleFeatureApiModule
import com.example.day.features.group_choice.impl.di.GroupChoiceFeatureApiModule
import com.example.day.features.group_choice.impl.di.GroupChoiceFeatureDeps
import com.example.day.features.user_settings.impl.di.UserSettingsFeatureApiModule
import com.example.day.features.user_settings.impl.di.UserSettingsFeatureDeps
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        NetworkModule::class,
        ChatCoreFeatureModule::class,
        MemoryCoreFeatureModule::class,
        AgentCoreFeatureModule::class,
        ConsoleFeatureApiModule::class,
        ChatsFeatureApiModule::class,
        GroupChoiceFeatureApiModule::class,
        UserSettingsFeatureApiModule::class,
        LlmCoreFeatureModule::class
    ]
)
@Immutable
interface AppComponent : FeatureEntryProvider, ConsoleFeatureDeps, ChatsFeatureDeps, GroupChoiceFeatureDeps, UserSettingsFeatureDeps {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}

val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}

