package com.example.day.features.console.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.impl.di.ConsoleFeatureComponent
import com.example.day.features.console.impl.di.ConsoleFeatureDepsProvider
import com.example.day.features.console.impl.di.DaggerConsoleFeatureComponent
import com.example.day.features.console.impl.ui.ConsoleScreen
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import javax.inject.Inject

class ConsoleFeatureEntryIml @Inject constructor(): ConsoleFeatureEntry {
    @Composable
    override fun ComposableEntryPoint(modifier: Modifier) {
        val featureComponent: ConsoleFeatureComponent = retain {
            DaggerConsoleFeatureComponent.factory().create(
                ConsoleFeatureDepsProvider.deps
            )
        }
        val viewModel: ConsoleViewModelImpl = viewModel(factory = featureComponent.getViewModelFactory())

        ConsoleScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}