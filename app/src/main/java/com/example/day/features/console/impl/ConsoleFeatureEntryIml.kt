package com.example.day.features.console.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.app.di.LocalAppComponent
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.impl.di.ConsoleFeatureComponent
import com.example.day.features.console.impl.di.DaggerConsoleFeatureComponent
import com.example.day.features.console.impl.ui.ConsoleScreen
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import javax.inject.Inject

class ConsoleFeatureEntryIml @Inject constructor(): ConsoleFeatureEntry {
    @Composable
    override fun EntryPoint(chatId: Long, modifier: Modifier) {
        val appComponent = LocalAppComponent.current
        
        val featureComponent: ConsoleFeatureComponent = retain {
            DaggerConsoleFeatureComponent.factory().create(appComponent)
        }

        val extras = remember(chatId) {
            MutableCreationExtras().apply {
                set(ConsoleViewModelImpl.CHAT_ID_KEY, chatId)
            }
        }

        val viewModel: ConsoleViewModelImpl = viewModel(
            key = chatId.toString(),
            factory = featureComponent.getViewModelFactory(),
            extras = extras
        )

        ConsoleScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}