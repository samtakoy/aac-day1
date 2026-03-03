package com.example.day.features.group_choice.impl.di

import com.example.day.features.group_choice.impl.ui.viewmodel.GroupChoiceViewModelImpl
import dagger.Module
import dagger.Provides

@Module
internal interface GroupChoiceFeatureModule {
    companion object {
        @Provides
        fun provideViewModelFactory(deps: GroupChoiceFeatureDeps): GroupChoiceViewModelImpl.Factory {
            return GroupChoiceViewModelImpl.Factory(
                deps.getChatGroupsUseCase,
                deps.createChatGroupUseCase,
                deps.updateChatGroupUseCase,
                deps.deleteChatGroupUseCase,
                deps.getChatTypesUseCase,
                deps.createPlannerGroupWithMainChatUseCase
            )
        }
    }
}
