package com.example.day.features.chats.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.app.di.LocalAppComponent
import com.example.day.features.chats.api.ChatsFeatureEntry
import com.example.day.features.chats.impl.di.ChatsFeatureComponent
import com.example.day.features.chats.impl.di.DaggerChatsFeatureComponent
import com.example.day.features.chats.impl.ui.ChatsScreen
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModelImpl
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import javax.inject.Inject

class ChatsFeatureEntryIml @Inject constructor(): ChatsFeatureEntry {
    @Composable
    override fun EntryPoint(groupId: Long, modifier: Modifier, onNavigateBack: (() -> Unit)?) {
        val appComponent = LocalAppComponent.current
        
        val featureComponent: ChatsFeatureComponent = retain {
            DaggerChatsFeatureComponent.factory().create(appComponent)
        }

        val extras = remember(groupId) {
            MutableCreationExtras().apply {
                set(ChatsViewModelImpl.GROUP_ID_KEY, groupId)
            }
        }

        val viewModel: ChatsViewModelImpl = viewModel(
            key = "${ChatsViewModelImpl::class.qualifiedName}_$groupId",
            factory = featureComponent.getViewModelFactory(),
            extras = extras
        )

        ChatsScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}
