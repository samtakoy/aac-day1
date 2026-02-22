package com.example.day.features.group_choice.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.dialogs.confirm.ConfirmDialog
import com.example.day.core.ui.uikit.dialogs.group.GroupEditDialog
import com.example.day.core.ui.uikit.dialogs.group.GroupEditDialogState
import com.example.day.features.group_choice.impl.ui.components.GroupsGrid
import com.example.day.features.group_choice.impl.ui.viewmodel.GroupChoiceViewModel
import com.example.day.features.group_choice.impl.ui.viewmodel.GroupChoiceViewModelImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupChoiceScreen(
    viewModel: GroupChoiceViewModelImpl,
    onGroupSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.getStateAsFlow().collectAsState().value

    // Handle navigation when group is selected
    LaunchedEffect(state.selectedGroupId) {
        state.selectedGroupId?.let { groupId ->
            onGroupSelected(groupId)
            viewModel.onEvent(GroupChoiceViewModel.Event.NavigationHandled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Группы чатов") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { viewModel.onEvent(GroupChoiceViewModel.Event.AddGroupClick) }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить группу"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when {
                state.groups.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Нет групп чатов", style = MaterialTheme.typography.headlineMedium)
                        Text("Нажмите кнопку ниже, чтобы создать первую группу")
                    }
                }
                else -> {
                    GroupsGrid(
                        groups = state.groups,
                        onGroupClick = { viewModel.onEvent(GroupChoiceViewModel.Event.GroupClick(it)) },
                        onEditClick = { viewModel.onEvent(GroupChoiceViewModel.Event.EditGroupClick(it)) },
                        onDeleteClick = { viewModel.onEvent(GroupChoiceViewModel.Event.DeleteGroupClick(it)) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (state.editDialog != null) {
                GroupEditDialog(
                    state = state.editDialog,
                    onTitleChange = { viewModel.onEvent(GroupChoiceViewModel.Event.DialogTitleChanged(it)) },
                    onTypeChange = { viewModel.onEvent(GroupChoiceViewModel.Event.DialogTypeSelected(it)) },
                    onConfirm = { viewModel.onEvent(GroupChoiceViewModel.Event.DialogConfirm) },
                    onDismiss = { viewModel.onEvent(GroupChoiceViewModel.Event.DialogDismiss) }
                )
            }

            if (state.deleteConfirm != null) {
                ConfirmDialog(
                    title = "Удалить группу?",
                    message = "Группа \"${state.deleteConfirm.title}\" и все чаты будут удалены.",
                    onConfirm = { viewModel.onEvent(GroupChoiceViewModel.Event.DeleteConfirm) },
                    onDismiss = { viewModel.onEvent(GroupChoiceViewModel.Event.DeleteDismiss) }
                )
            }
        }
    }
}
