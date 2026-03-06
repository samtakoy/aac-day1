package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * Interface for task state handlers.
 * Each state (init, planning, execution, etc.) implements this to define its behavior.
 *
 * Key features:
 * - [stateName] as String for flexible state identification
 * - [buildSystemPrompt] for building context-aware prompts
 * - [handle] for processing LLM responses and determining next actions
 */
interface TaskStateHandler {
    /** The name of the state this handler manages */
    val stateName: StateId

    /**
     * Build system prompt for this state.
     * The prompt should include all context from previous states via artifacts.
     */
    suspend fun buildSystemPrompt(context: TaskContext): String

    /** Приветственное сообщение ассистента самому себе когда история пустая */
    suspend fun buildAssistantPreFillPrompt(context: TaskContext): String?

    /**
     * Process LLM response and determine next state.
     */
    suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): HandlerResult

    suspend fun handleUserAction(
        context: TaskContext,
        action: String
    ) : HandlerResult
}
