package com.example.day.features.console.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Entry point for PLANNER-type chat groups.
 * Uses PlannerTalkDelegate with PlannerWorker for three-layer memory architecture.
 */
interface PlannerConsoleFeatureEntry {
    @Composable
    fun EntryPoint(chatId: Long, modifier: Modifier)
}
