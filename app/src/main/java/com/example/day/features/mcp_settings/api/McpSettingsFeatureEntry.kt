package com.example.day.features.mcp_settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Entry point for the MCP Settings feature */
interface McpSettingsFeatureEntry {
    @Composable
    fun EntryPoint(
        modifier: Modifier = Modifier,
        onNavigateBack: () -> Unit
    )
}
