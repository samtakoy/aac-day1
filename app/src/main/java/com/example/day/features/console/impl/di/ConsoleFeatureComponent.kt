package com.example.day.features.console.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.console.impl.ui.delegates.AgentsTalkDelegate
import com.example.day.features.console.impl.ui.delegates.LlmTalkDelegate
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import dagger.Component

@Immutable
@ConsoleFeatureScope
@Component(dependencies = [ConsoleFeatureDeps::class], modules = [ConsoleFeatureModule::class])
internal interface ConsoleFeatureComponent {
    @Component.Factory
    interface Factory {
        fun create(deps: ConsoleFeatureDeps): ConsoleFeatureComponent
    }

    fun getViewModelFactory(): ConsoleViewModelImpl.Factory
    fun getAgentsViewModelFactory(): ConsoleViewModelImpl.AgentFactory
    fun getLlmTalkDelegate(): LlmTalkDelegate
    fun getAgentsTalkDelegate(): AgentsTalkDelegate
}