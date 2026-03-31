package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme

/**
 * A modal dialog wrapper for ChatSettingsView.
 * Displays chat settings in a dialog with proper styling.
 *
 * @param state The UI model containing settings state
 * @param onDismiss Callback when dialog is dismissed
 * @param onSubmit Callback when settings are submitted
 * @param colors Chat color scheme
 */
@Composable
fun ChatSettingsDialog(
    state: ChatSettingsUiModel,
    onDismiss: () -> Unit,
    onSubmit: (chatTitle: String, settings: ChatSettings) -> Unit,
    onHandlePrToggle: (Boolean) -> Unit = {},
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = colors.background
        ) {
            ChatSettingsView(
                state = state,
                onSubmit = onSubmit,
                onCancel = onDismiss,
                onHandlePrToggle = onHandlePrToggle,
                colors = colors
            )
        }
    }
}
