package com.example.day.features.user_settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.app.di.LocalAppComponent
import com.example.day.features.user_settings.api.UserSettingsFeatureEntry
import com.example.day.features.user_settings.impl.di.DaggerUserSettingsFeatureComponent
import com.example.day.features.user_settings.impl.di.UserSettingsFeatureComponent
import com.example.day.features.user_settings.impl.ui.UserSettingsScreen
import com.example.day.features.user_settings.impl.ui.viewmodel.UserSettingsViewModelImpl
import javax.inject.Inject

class UserSettingsFeatureEntryImpl @Inject constructor() : UserSettingsFeatureEntry {

    @Composable
    override fun EntryPoint(modifier: Modifier, onDismiss: () -> Unit) {
        val appComponent = LocalAppComponent.current
        val featureComponent: UserSettingsFeatureComponent = retain {
            DaggerUserSettingsFeatureComponent.factory().create(appComponent)
        }
        val viewModel: UserSettingsViewModelImpl = viewModel(
            factory = featureComponent.getViewModelFactory()
        )
        UserSettingsScreen(viewModel = viewModel, onDismiss = onDismiss, modifier = modifier)
    }
}
