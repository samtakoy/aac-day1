package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.states.*
import com.example.day.core.core_features.llm.domain.model.ModelRequest

/**
 * Finite State Machine for task workflow.
 * Manages state handlers and per-state message history.
 */
class TaskStateMachine {

    private val handlers = mapOf(
        TaskState.INIT to InitStateHandler(),
        TaskState.PLANNING to PlanningStateHandler(),
        TaskState.EXECUTION to ExecutionStateHandler(),
        TaskState.VERIFICATION to VerificationStateHandler(),
        TaskState.DONE to DoneStateHandler()
    )

    // Per-state message history (preserved across transitions for context)
    private val stateHistories = mutableMapOf<TaskState, MutableList<ModelRequest.Message>>()

    /**
     * Get handler for current state.
     */
    fun getHandler(state: TaskState): TaskStateHandler {
        return handlers[state] ?: error("No handler for state: $state")
    }

    /**
     * Get message history for specific state.
     */
    fun getHistory(state: TaskState): List<ModelRequest.Message> {
        return stateHistories[state] ?: emptyList()
    }

    /**
     * Add message to state's history.
     */
    fun addToHistory(state: TaskState, role: ModelRequest.Role, content: String) {
        stateHistories.getOrPut(state) { mutableListOf() }
            .add(ModelRequest.Message(role, content))
    }

    /**
     * Clear history for specific state.
     */
    fun clearHistory(state: TaskState) {
        stateHistories.remove(state)
    }

    /**
     * Clear all histories (called on task completion or reset).
     */
    fun clearAllHistories() {
        stateHistories.clear()
    }
}
