package com.example.day.features.console.impl.di

import com.example.day.features.console.impl.ui.delegates.AssistantTalkDelegate
import com.example.day.features.console.impl.ui.delegates.PlannerTalkDelegate
import com.example.day.features.console.impl.ui.delegates.RagTalkDelegate
import dagger.Module
import dagger.Provides

/**
 * DI модуль для console фичи.
 */
@Module
internal class ConsoleFeatureModule {

    @Provides
    fun providePlannerTalkDelegate(deps: ConsoleFeatureDeps): PlannerTalkDelegate {
        return PlannerTalkDelegate(
            addChatMessageUseCase = deps.addChatMessageUseCase,
            taskWorker = deps.taskWorker,
            chatTools = deps.chatTools,
            json = deps.json
        )
    }

    @Provides
    fun provideRagTalkDelegate(deps: ConsoleFeatureDeps): RagTalkDelegate {
        return RagTalkDelegate(
            addChatMessageUseCase = deps.addChatMessageUseCase,
            ragWorker = deps.ragWorker,
            chatTools = deps.chatTools,
        )
    }

    @Provides
    fun provideAssistantTalkDelegate(deps: ConsoleFeatureDeps): AssistantTalkDelegate {
        return AssistantTalkDelegate(
            addChatMessageUseCase = deps.addChatMessageUseCase,
            assistantWorker = deps.assistantWorker,
            chatTools = deps.chatTools,
            consumptionCalculator = deps.consuption,
        )
    }
}
