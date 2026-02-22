package com.example.day.features.group_choice.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface GroupChoiceFeatureEntry {
    @Composable
    fun EntryPoint(
        modifier: Modifier = Modifier,
        onGroupSelected: (Long) -> Unit
    )
}
