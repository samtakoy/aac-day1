package com.example.day.features.console.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RagConsoleFeatureEntry {
    @Composable
    fun EntryPoint(chatId: Long, modifier: Modifier)
}
