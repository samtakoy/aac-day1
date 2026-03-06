package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.HandlerResult
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext
import com.example.day.core.core_features.agent.domain.workers.task.states.toHandlerResult

/**
 * Interface for task state handlers.
 * Each state (INIT, PLANNING, etc.) implements this to define its behavior.
 */
interface TaskStateHandler {
    val state: TaskState

    /**
     * Build system prompt for this state.
     * The prompt should include all context from previous states via artifacts.
     */
    fun buildSystemPrompt(context: TaskContext): String

    /**
     * Process LLM response and determine next state.
     * Legacy method for backward compatibility - returns StateResult.
     */
    suspend fun handleLegacy(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): StateResult

    /**
     * Process LLM response and determine next state.
     * Default implementation converts StateResult to HandlerResult.
     */
    suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): HandlerResult {
        return handleLegacy(context, userInput, llmResponse).toHandlerResult(state)
    }
}

/**
 * Result of state handling.
 *
 * @property replyToUser Text to show to user
 * @property nextState Next state or null to stay in current
 * @property memoryUpdates Key-value pairs to save to LTM
 * @property newStep New step number (for EXECUTION transitions)
 */
data class StateResult(
    val replyToUser: String,
    val nextState: TaskState?,
    val memoryUpdates: Map<String, String>,
    val newStep: Int? = null
)
