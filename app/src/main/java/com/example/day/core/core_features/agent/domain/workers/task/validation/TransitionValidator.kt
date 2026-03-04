package com.example.day.core.core_features.agent.domain.workers.task.validation

import com.example.day.core.core_features.agent.domain.model.TaskState

/**
 * Validates state transitions in TaskWorker state machine.
 * Prevents invalid transitions like INIT -> DONE directly.
 */
class TransitionValidator {

    /**
     * Matrix of allowed transitions.
     * Key: From state
     * Value: Set of allowed To states
     */
    private val validTransitions = mapOf(
        TaskState.INIT to setOf(TaskState.PLANNING),
        TaskState.PLANNING to setOf(TaskState.EXECUTION),
        TaskState.EXECUTION to setOf(TaskState.VERIFICATION),
        TaskState.VERIFICATION to setOf(TaskState.EXECUTION, TaskState.DONE),
        TaskState.DONE to setOf(TaskState.INIT) // For starting new task
    )

    /**
     * Check if transition from one state to another is valid.
     *
     * @param from Current state
     * @param to Target state
     * @return true if transition is allowed
     */
    fun isValid(from: TaskState, to: TaskState): Boolean {
        // Same state is always valid (no transition)
        if (from == to) return true

        // Check if transition exists in matrix
        val allowedStates = validTransitions[from]
        return allowedStates?.contains(to) ?: false
    }

    /**
     * Validate transition and return result with error message if invalid.
     */
    fun validate(from: TaskState, to: TaskState): ValidationResult {
        return if (isValid(from, to)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                error = "Invalid transition from $from to $to. " +
                    "Allowed transitions from $from: ${getAllowedTransitions(from)}"
            )
        }
    }

    /**
     * Get list of allowed transitions from a given state.
     */
    fun getAllowedTransitions(from: TaskState): List<TaskState> {
        return validTransitions[from]?.toList() ?: emptyList()
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val error: String) : ValidationResult()
    }
}