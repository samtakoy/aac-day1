package com.example.day.core.core_features.agent.domain.workers.base

import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Worker events for notifying about LLM request lifecycle.
 */
sealed interface WorkerEvent {
    /** Agent notifies that it will make a request - sends information before each LLM request */
    object RequestStart : WorkerEvent
    /** Agent notifies that it received a successful response to the request */
    class RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    /** Agent notifies that the LLM request returned an error */
    class RequestError(val text: String) : WorkerEvent

    // ========== TOOL CALLING EVENTS ==========

    /** Model requested a tool call */
    class ToolCallStarted(
        val toolCallId: String,
        val toolName: String,
        val arguments: String
    ) : WorkerEvent

    /** Tool call finished */
    class ToolCallFinished(
        val toolCallId: String,
        val toolName: String,
        val result: String,
        val isError: Boolean
    ) : WorkerEvent

    // ========== PLANNER-SPECIFIC EVENTS ==========
    
    /**
     * Planner suggests creating a new stage chat.
     * UI should show confirmation button - chat is NOT created automatically.
     */
    class StageCreationSuggested(
        val stageTitle: String,
        val workingSummary: String
    ) : WorkerEvent
    
    /**
     * A stage has been marked as completed and artifact saved.
     */
    class StageCompleted(
        val chatId: Long,
        val artifactContent: String
    ) : WorkerEvent
    
    /**
     * A fact has been saved to long-term memory.
     */
    class FactSaved(
        val memoryKey: String,
        val category: String,
        val fact: String
    ) : WorkerEvent
}
