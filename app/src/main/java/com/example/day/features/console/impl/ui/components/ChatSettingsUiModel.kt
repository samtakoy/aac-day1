package com.example.day.features.console.impl.ui.components

import androidx.compose.runtime.Immutable
import com.example.day.features.console.impl.domain.model.ChatSettings

@Immutable
data class ChatSettingsUiModel(
    val title: String,
    val settingsState: ChatSettings
)