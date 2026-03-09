package com.example.day.features.mcp_settings.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.mcp_settings.impl.ui.viewmodel.McpSettingsViewModelImpl
import dagger.Component

@Immutable
@McpSettingsFeatureScope
@Component(
    dependencies = [McpSettingsFeatureDeps::class],
    modules = [McpSettingsFeatureModule::class]
)
internal interface McpSettingsFeatureComponent {

    @Component.Factory
    interface Factory {
        fun create(deps: McpSettingsFeatureDeps): McpSettingsFeatureComponent
    }

    fun getViewModelFactory(): McpSettingsViewModelImpl.Factory
}
