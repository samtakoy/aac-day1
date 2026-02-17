package com.example.day.features.console.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.core.ui.uikit.chat.bar.ChatBarView
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiEvent
import com.example.day.core.ui.uikit.chat.list.ChatListView
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiEvent
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
            ChatBarView(
                model = state.chatBar,
                onEvent = { barEvent ->
                    when (barEvent) {
                        ChatBarUiEvent.SendClick -> onEvent(ConsoleViewModel.Event.SubmitButtonClick)
                        is ChatBarUiEvent.TextChange -> onEvent(ConsoleViewModel.Event.InputChanged(barEvent.text))
                    }
                }
            )
        }
    }
}