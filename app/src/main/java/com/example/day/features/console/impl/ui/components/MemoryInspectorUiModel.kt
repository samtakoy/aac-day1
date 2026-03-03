package com.example.day.features.console.impl.ui.components

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * UI Model for Memory Inspector component
 * Displays three layers of assistant memory:
 * - Short-term: current chat messages
 * - Working: current task context (working_summary)
 * - Long-term: persistent user facts
 */
data class MemoryInspectorUiModel(
    val shortTermMessages: ImmutableList<ShortTermMemoryItem> = emptyList<ShortTermMemoryItem>().toImmutableList(),
    val workingMemory: String? = null,
    val longTermFacts: ImmutableList<LongTermFactItem> = emptyList<LongTermFactItem>().toImmutableList()
)

data class ShortTermMemoryItem(
    val role: String,
    val content: String,
    val timestamp: Long
)

data class LongTermFactItem(
    val memoryKey: String,
    val categoryId: Long,
    val category: String,
    val fact: String,
    val updatedAt: Long
)

