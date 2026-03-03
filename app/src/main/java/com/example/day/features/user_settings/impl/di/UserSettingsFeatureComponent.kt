package com.example.day.features.user_settings.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.user_settings.impl.ui.viewmodel.UserSettingsViewModelImpl
import dagger.Component

@Immutable
@UserSettingsFeatureScope
@Component(
    dependencies = [UserSettingsFeatureDeps::class],
    modules = [UserSettingsFeatureModule::class]
)
internal interface UserSettingsFeatureComponent {

    @Component.Factory
    interface Factory {
        fun create(deps: UserSettingsFeatureDeps): UserSettingsFeatureComponent
    }

    fun getViewModelFactory(): UserSettingsViewModelImpl.Factory
}
