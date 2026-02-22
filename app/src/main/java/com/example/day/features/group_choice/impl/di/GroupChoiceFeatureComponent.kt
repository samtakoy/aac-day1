package com.example.day.features.group_choice.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.group_choice.impl.ui.viewmodel.GroupChoiceViewModelImpl
import dagger.Component

@Immutable
@GroupChoiceFeatureScope
@Component(dependencies = [GroupChoiceFeatureDeps::class], modules = [GroupChoiceFeatureModule::class])
internal interface GroupChoiceFeatureComponent {
    @Component.Factory
    interface Factory {
        fun create(deps: GroupChoiceFeatureDeps): GroupChoiceFeatureComponent
    }

    fun getViewModelFactory(): GroupChoiceViewModelImpl.Factory
}
