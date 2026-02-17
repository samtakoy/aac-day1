package com.example.day.features.chats.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.features.chats.api.ChatsFeatureEntry
import com.example.day.features.chats.impl.di.ChatsFeatureComponent
import com.example.day.features.chats.impl.di.ChatsFeatureDepsProvider
import com.example.day.features.chats.impl.di.DaggerChatsFeatureComponent
import com.example.day.features.chats.impl.ui.ChatsScreen
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModelImpl
import javax.inject.Inject

class ChatsFeatureEntryIml @Inject constructor(): ChatsFeatureEntry {
    @Composable
    override fun ComposableEntryPoint(modifier: Modifier) {
        val featureComponent: ChatsFeatureComponent = retain {
            DaggerChatsFeatureComponent.factory().create(
                ChatsFeatureDepsProvider.deps
            )
        }

        val viewModel: ChatsViewModelImpl = viewModel(factory = featureComponent.getViewModelFactory())

        ChatsScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}
