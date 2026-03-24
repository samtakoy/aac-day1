package com.example.day.app.di

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.day.app.di.AppSettingsModule
import com.example.day.core.core_features.agent.di.AgentCoreFeatureModule
import com.example.day.core.core_features.chat.di.ChatCoreFeatureModule
import com.example.day.core.core_features.llm.di.LlmCoreFeatureModule
import com.example.day.core.core_features.mcp.di.McpCoreFeatureModule
import com.example.day.core.core_features.memory.di.MemoryCoreFeatureModule
import com.example.day.core.core_features.reminder.di.ReminderCoreFeatureModule
import com.example.day.core.core_features.reminder.domain.usecase.ExecuteReminderUseCase
import com.example.day.core.di.NetworkModule
import com.example.day.core.feature_entries.FeatureEntryProvider
import com.example.day.features.chats.impl.di.ChatsFeatureApiModule
import com.example.day.features.chats.impl.di.ChatsFeatureDeps
import com.example.day.features.console.impl.di.ConsoleFeatureApiModule
import com.example.day.features.console.impl.di.ConsoleFeatureDeps
import com.example.day.features.group_choice.impl.di.GroupChoiceFeatureApiModule
import com.example.day.features.group_choice.impl.di.GroupChoiceFeatureDeps
import com.example.day.features.mcp_settings.impl.di.McpSettingsFeatureApiModule
import com.example.day.features.mcp_settings.impl.di.McpSettingsFeatureDeps
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
        ReminderCoreFeatureModule::class,
        ConsoleFeatureApiModule::class,
        ChatsFeatureApiModule::class,
        GroupChoiceFeatureApiModule::class,
        UserSettingsFeatureApiModule::class,
        LlmCoreFeatureModule::class,
        McpCoreFeatureModule::class,
        McpSettingsFeatureApiModule::class,
        AppSettingsModule::class
    ]
)
@Immutable
interface AppComponent : FeatureEntryProvider, ConsoleFeatureDeps, ChatsFeatureDeps,
    GroupChoiceFeatureDeps, UserSettingsFeatureDeps, McpSettingsFeatureDeps {
    // TODO ого какая хрень
    fun executeReminderUseCase(): ExecuteReminderUseCase

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}

val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}
