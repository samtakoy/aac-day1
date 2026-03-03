package com.example.day.features.console.impl.ui.delegates

/**
 * UI Events emitted by PlannerTalkDelegate for the ViewModel to handle.
 * These events are emitted when LLM returns pattern-based tool calls like:
 * - CREATE_STAGE[title:context]
 * - COMPLETE_STAGE[outcome]
 * - SAVE_FACT[key:category:fact]
 */
sealed class PlannerUiEvent {
    /**
     * LLM suggests creating a new stage chat (e.g., "Этап 1").
     * UI should show confirmation dialog to user.
     */
    data class StageCreationSuggested(
        val stageTitle: String,
        val workingSummary: String
    ) : PlannerUiEvent()

    /**
     * LLM completed a stage and provides artifact/outcome.
     */
    data class StageCompleted(
        val chatId: Long,
        val artifactContent: String
    ) : PlannerUiEvent()

    /**
     * LLM saved a fact to long-term memory.
     */
    data class FactSaved(
        val memoryKey: String,
        val category: String,
        val fact: String
    ) : PlannerUiEvent()
}
