package com.example.day.features.console.impl.ui.delegates

sealed class PlannerUiEvent {
    data class StageCreationSuggested(
        val stageTitle: String,
        val workingSummary: String
    ) : PlannerUiEvent()

    data class StageCompleted(
        val chatId: Long,
        val artifactContent: String
    ) : PlannerUiEvent()

    data class FactSaved(
        val memoryKey: String,
        val category: String,
        val fact: String
    ) : PlannerUiEvent()

    data class UserConfirmation(
        val id: String,
        val runId: String,
        val title: String,
        val message: String,
        val actionLabel: String
    ) : PlannerUiEvent()
}
