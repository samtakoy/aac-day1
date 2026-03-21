package com.example.day.features.console.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.chat.bar.ChatBarView
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiEvent
import com.example.day.core.ui.uikit.chat.list.ChatListView
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiEvent
import com.example.day.features.console.impl.ui.components.ChatSettingsView
import com.example.day.features.console.impl.ui.components.MemoryInspectorView
import com.example.day.features.console.impl.ui.components.StageCreationDialog
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel

@Composable
internal fun ConsoleScreen(
    viewModel: ConsoleViewModel,
    modifier: Modifier
) {
    val state by viewModel.getStateAsFlow().collectAsStateWithLifecycle()
    val stageCreationState by viewModel.getStageCreationState().collectAsStateWithLifecycle()
    val userConfirmationState by viewModel.getUserConfirmationState().collectAsStateWithLifecycle()
    val memoryInspectorState by viewModel.getMemoryInspectorState().collectAsStateWithLifecycle()
    var showMemoryInspector by remember { mutableStateOf(false) }

    ConsoleScreenInternal(
        state = state,
        onEvent = viewModel::onEvent,
        onMemoryClick = {
            viewModel.onEvent(ConsoleViewModel.Event.OpenMemoryInspector)
            showMemoryInspector = true
        },
        modifier = modifier
    )

    // Stage Creation Dialog (PLANNER groups)
    StageCreationDialog(
        state = stageCreationState,
        onConfirm = { viewModel.onEvent(ConsoleViewModel.Event.ConfirmStageCreation) },
        onDismiss = { viewModel.onEvent(ConsoleViewModel.Event.DeclineStageCreation) }
    )

    // User Confirmation Dialog (for dangerous/expensive operations)
    userConfirmationState?.let { confirmState ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ConsoleViewModel.Event.DeclineUserConfirmation) },
            title = { Text(confirmState.title) },
            text = { Text(confirmState.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ConsoleViewModel.Event.ConfirmUserConfirmation) }) {
                    Text(confirmState.actionLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ConsoleViewModel.Event.DeclineUserConfirmation) }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Memory Inspector — full-height dialog with tab navigation for all 3 memory layers
    if (showMemoryInspector) {
        Dialog(onDismissRequest = { showMemoryInspector = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Память агента",
                            style = MaterialTheme.typography.titleLarge
                        )
                        TextButton(onClick = { showMemoryInspector = false }) {
                            Text("Закрыть")
                        }
                    }
                    MemoryInspectorView(
                        uiModel = memoryInspectorState,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleScreenInternal(
    state: ConsoleViewModel.State,
    onEvent: (ConsoleViewModel.Event) -> Unit,
    onMemoryClick: () -> Unit,
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
            if (state.isStageCompleted) {
                StageCompletedBanner()
            }
            ChatListView(
                model = state.chatList,
                onEvent = { listEvent ->
                    when (listEvent) {
                        is ChatListUiEvent.ItemLongClick -> {}
                        is ChatListUiEvent.ChatButtonClick -> {
                            onEvent(ConsoleViewModel.Event.ChatButtonClick(listEvent.messageId, listEvent.actionId))
                        }
                    }
                },
                onInfoMessageExpand = { id, isExpanded ->
                    onEvent(ConsoleViewModel.Event.MessageExpandedChange(id, isExpanded))
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsButtonView(
                    modifier = Modifier.clickable {
                        onEvent(ConsoleViewModel.Event.OpenSettingsClick)
                    }
                )
                MemoryButtonView(
                    modifier = Modifier.clickable { onMemoryClick() }
                )
                ChatBarView(
                    model = state.chatBar,
                    onEvent = { barEvent ->
                        when (barEvent) {
                            ChatBarUiEvent.SendClick -> onEvent(ConsoleViewModel.Event.SubmitButtonClick)
                            is ChatBarUiEvent.TextChange -> onEvent(
                                ConsoleViewModel.Event.InputChanged(barEvent.text)
                            )
                        }
                    }
                )
            }
        }
        if (state.settings != null) {
            ChatSettingsView(
                state = state.settings,
                onSubmit = { chatTitle, settings ->
                    onEvent(ConsoleViewModel.Event.SettingsSubmitClick(chatTitle, settings))
                },
                onCancel = { onEvent(ConsoleViewModel.Event.SettingsCancelClick) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SettingsButtonView(modifier: Modifier) {
    val buttonColor = LocalChatColorScheme.current.sendButtonDisabled

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(buttonColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MemoryButtonView(modifier: Modifier) {
    val buttonColor = LocalChatColorScheme.current.sendButtonDisabled

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(buttonColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Memory,
            contentDescription = "Memory",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StageCompletedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Этап завершён. Результат сохранён в рабочую память проекта.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
