package com.example.day.features.chats.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ChatsFeatureEntry {
    @Composable
    fun EntryPoint(
        groupId: Long,
        modifier: Modifier = Modifier,
        onNavigateBack: (() -> Unit)? = null
    )
}