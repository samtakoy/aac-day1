package com.example.day.features.console.impl.ui.components

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatSettings

@Immutable
data class ChatSettingsUiModel(
    val title: String,
    val chatTitle: String,
    val settingsState: ChatSettings
)