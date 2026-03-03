package com.example.day.features.user_settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface UserSettingsFeatureEntry {
    @Composable
    fun EntryPoint(modifier: Modifier = Modifier, onDismiss: () -> Unit)
}
