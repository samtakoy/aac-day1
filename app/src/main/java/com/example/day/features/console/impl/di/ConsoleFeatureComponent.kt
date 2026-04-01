package com.example.day.features.console.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.console.impl.ui.delegates.AgentsTalkDelegate
import com.example.day.features.console.impl.ui.delegates.AssistantTalkDelegate
import com.example.day.features.console.impl.ui.delegates.LlmTalkDelegate
import com.example.day.features.console.impl.ui.delegates.PlannerTalkDelegate
import com.example.day.features.console.impl.ui.delegates.RagTalkDelegate
import com.example.day.features.console.impl.ui.delegates.SupportTalkDelegate
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
    fun getPlannerViewModelFactory(): ConsoleViewModelImpl.PlannerFactory
    fun getRagViewModelFactory(): ConsoleViewModelImpl.RagFactory
    fun getAssistantViewModelFactory(): ConsoleViewModelImpl.AssistantFactory
    fun getLlmTalkDelegate(): LlmTalkDelegate
    fun getAgentsTalkDelegate(): AgentsTalkDelegate
    fun getPlannerTalkDelegate(): PlannerTalkDelegate
    fun getRagTalkDelegate(): RagTalkDelegate
    fun getAssistantTalkDelegate(): AssistantTalkDelegate
    fun getSupportViewModelFactory(): ConsoleViewModelImpl.SupportFactory
    fun getSupportTalkDelegate(): SupportTalkDelegate
}