package com.example.day.features.console.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ConsoleFeatureEntry {
    @Composable
    fun EntryPoint(
        chatId: Long,
        modifier: Modifier
    )
}