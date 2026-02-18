package com.example.day.features.console.impl.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.bar.ChatBarView
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiEvent
import com.example.day.core.ui.uikit.chat.bar.model.ChatSendButtonType
import com.example.day.core.ui.uikit.chat.list.ChatListView
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiEvent
import com.example.day.features.console.impl.ui.components.ChatSettingsView
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel.State

@Composable
internal fun ConsoleScreen(
    viewModel: ConsoleViewModel,
    modifier: Modifier
) {
    val state = viewModel.getStateAsFlow().collectAsStateWithLifecycle().value

    ConsoleScreenInternal(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
private fun ConsoleScreenInternal(
    state: State,
    onEvent: (ConsoleViewModel.Event) -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ChatListView(
                model = state.chatList,
                onEvent = { listEvent ->
                    when (listEvent) {
                        is ChatListUiEvent.ItemLongClick -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsView(
                    modifier = Modifier.clickable {
                        onEvent(ConsoleViewModel.Event.OpenSettingsClick)
                    }
                )
                ChatBarView(
                    model = state.chatBar,
                    onEvent = { barEvent ->
                        when (barEvent) {
                            ChatBarUiEvent.SendClick -> onEvent(ConsoleViewModel.Event.SubmitButtonClick)
                            is ChatBarUiEvent.TextChange -> onEvent(
                                ConsoleViewModel.Event.InputChanged(
                                    barEvent.text
                                )
                            )
                        }
                    }
                )
            }
        }
        if (state.settings != null) {
            ChatSettingsView(
                model = state.settings,
                onSubmit = { onEvent(ConsoleViewModel.Event.SettingsSubmitClick(it)) },
                onCancel = { onEvent(ConsoleViewModel.Event.SettingsCancelClick) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SettingsView(modifier: Modifier) {
    val buttonColor = LocalChatColors.current.sendButtonDisabled

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(buttonColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Send",
            tint = Color.White,
            modifier = Modifier
                .size(12.dp)
        )
    }
}