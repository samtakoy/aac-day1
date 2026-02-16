package com.example.day.core.feature_entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

@Immutable
interface FeatureEntry {
    @Composable
    fun ComposableEntryPoint(
        modifier: Modifier
    )
}