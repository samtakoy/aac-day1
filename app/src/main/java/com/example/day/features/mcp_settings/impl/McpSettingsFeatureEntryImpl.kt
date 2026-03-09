package com.example.day.features.mcp_settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.app.di.LocalAppComponent
import com.example.day.features.mcp_settings.api.McpSettingsFeatureEntry
import com.example.day.features.mcp_settings.impl.di.DaggerMcpSettingsFeatureComponent
import com.example.day.features.mcp_settings.impl.di.McpSettingsFeatureComponent
import com.example.day.features.mcp_settings.impl.ui.McpSettingsScreen
import com.example.day.features.mcp_settings.impl.ui.viewmodel.McpSettingsViewModelImpl
import javax.inject.Inject

class McpSettingsFeatureEntryImpl @Inject constructor() : McpSettingsFeatureEntry {

    @Composable
    override fun EntryPoint(modifier: Modifier, onNavigateBack: () -> Unit) {
        val appComponent = LocalAppComponent.current
        val featureComponent: McpSettingsFeatureComponent = retain {
            DaggerMcpSettingsFeatureComponent.factory().create(appComponent)
        }
        val viewModel: McpSettingsViewModelImpl = viewModel(
            factory = featureComponent.getViewModelFactory()
        )
        McpSettingsScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}
