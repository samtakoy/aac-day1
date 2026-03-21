package com.example.day.core.core_features.agent.domain.workers.base

import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Worker events for notifying about LLM request lifecycle.
 */
sealed interface WorkerEvent {
    object RequestStart : WorkerEvent
    class RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    class RequestError(val text: String) : WorkerEvent

    sealed interface Tool : WorkerEvent {
        class ToolCallStarted(
            val toolCallId: String,
            val toolName: String,
            val arguments: String
        ) : Tool

        class ToolCallFinished(
            val toolCallId: String,
            val toolName: String,
            val result: String,
            val isError: Boolean
        ) : Tool
    }

    sealed interface Planner : WorkerEvent {
        class StageCreationSuggested(
            val stageTitle: String,
            val workingSummary: String
        ) : Planner

        class StageCompleted(
            val chatId: Long,
            val artifactContent: String
        ) : Planner

        class FactSaved(
            val memoryKey: String,
            val category: String,
            val fact: String
        ) : Planner
    }

    sealed interface UserConfirmation : WorkerEvent {
        class Requested(
            val confirmationId: String,
            val runId: String,
            val title: String,
            val message: String,
            val actionLabel: String
        ) : UserConfirmation
    }
}
