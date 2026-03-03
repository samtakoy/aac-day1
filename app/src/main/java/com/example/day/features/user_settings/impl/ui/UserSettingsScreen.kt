package com.example.day.features.user_settings.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.core.ui.uikit.components.ltm.LongTermFactsListView
import com.example.day.features.user_settings.impl.ui.components.CreateProfileDialog
import com.example.day.features.user_settings.impl.ui.components.FactEditDialog
import com.example.day.features.user_settings.impl.ui.components.SelectProfileDialog
import com.example.day.features.user_settings.impl.ui.viewmodel.UserSettingsViewModel
import com.example.day.features.user_settings.impl.ui.viewmodel.UserSettingsViewModelImpl

@Composable
internal fun UserSettingsScreen(
    viewModel: UserSettingsViewModelImpl,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.getStateAsFlow().collectAsStateWithLifecycle()

    UserSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
private fun UserSettingsContent(
    state: UserSettingsViewModel.State,
    onEvent: (UserSettingsViewModel.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Настройки пользователя",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ─── Секция профиля ───────────────────────────────────────────────
        Text(
            text = "Текущий профиль:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.currentProfile?.title ?: "не выбран",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (state.currentProfile != null) FontWeight.Medium else FontWeight.Normal,
            color = if (state.currentProfile != null)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Кнопки управления профилем
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onEvent(UserSettingsViewModel.Event.SelectProfileClick) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Выбрать")
            }
            OutlinedButton(
                onClick = { onEvent(UserSettingsViewModel.Event.CreateProfileClick) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Добавить")
            }
            if (state.currentProfile != null) {
                Button(
                    onClick = { onEvent(UserSettingsViewModel.Event.ResetProfileClick) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сбросить")
                }
            }
        }

        // ─── Аватар профиля (только отображение) ─────────────────────────
        val avatar = state.currentProfile?.textAvatar
        if (avatar != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Аватар профиля",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = avatar,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Для генерации/сброса аватара используйте команды:\n@@talk(profile --avatar generate) | @@talk(profile --avatar reset)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        /* TODO пока не показываем, потому что в стейте UI накосячено и он все равно не обновится
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onEvent(UserSettingsViewModel.Event.RegenerateAvatar) },
                modifier = Modifier
            ) {
                Text("Пересоздать аватар")
            }
        }*/

        // ─── Факты профиля ────────────────────────────────────────────────
        if (state.currentProfile != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Факты профиля",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (state.profileFacts.isNotEmpty()) {
                    Text(
                        text = "${state.profileFacts.size} шт.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            LongTermFactsListView(
                facts = state.profileFacts,
                modifier = Modifier.fillMaxWidth(),
                onEdit = { fact -> onEvent(UserSettingsViewModel.Event.EditFactClick(fact)) },
                onDelete = { fact -> onEvent(UserSettingsViewModel.Event.DeleteFactClick(fact)) },
                onAdd = { onEvent(UserSettingsViewModel.Event.AddFactClick) }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // ─── Диалоги ─────────────────────────────────────────────────────────

    if (state.showSelectDialog) {
        SelectProfileDialog(
            profiles = state.allProfiles,
            currentProfileName = state.currentProfile?.title,
            onSelect = { name -> onEvent(UserSettingsViewModel.Event.ProfileSelected(name)) },
            onDismiss = { onEvent(UserSettingsViewModel.Event.SelectDialogDismiss) }
        )
    }

    if (state.showCreateDialog) {
        CreateProfileDialog(
            name = state.createProfileName,
            error = state.createProfileError,
            onNameChange = { onEvent(UserSettingsViewModel.Event.CreateProfileNameChanged(it)) },
            onConfirm = { onEvent(UserSettingsViewModel.Event.CreateProfileConfirm) },
            onDismiss = { onEvent(UserSettingsViewModel.Event.CreateDialogDismiss) }
        )
    }

    if (state.editingFact != null) {
        FactEditDialog(
            isNewFact = state.isAddingNewFact,
            factKey = state.editFactKey,
            factCategory = state.editFactCategory,
            factValue = state.editFactValue,
            error = state.factEditError,
            onKeyChange = { onEvent(UserSettingsViewModel.Event.FactKeyChanged(it)) },
            onCategoryChange = { onEvent(UserSettingsViewModel.Event.FactCategoryChanged(it)) },
            onValueChange = { onEvent(UserSettingsViewModel.Event.FactValueChanged(it)) },
            onConfirm = { onEvent(UserSettingsViewModel.Event.FactEditConfirm) },
            onDismiss = { onEvent(UserSettingsViewModel.Event.FactEditDismiss) }
        )
    }
}
