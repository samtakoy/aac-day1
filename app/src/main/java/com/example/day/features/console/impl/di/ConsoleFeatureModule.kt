package com.example.day.features.console.impl.di

import com.example.day.features.console.impl.ui.delegates.PlannerTalkDelegate
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
            plannerWorker = deps.plannerWorker,
            chatTools = deps.chatTools
        )
    }
}
