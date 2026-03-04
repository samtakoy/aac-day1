package com.example.day.core.core_features.agent.domain.model

/**
 * States of the task state machine (FSM).
 * Lifecycle: INIT → PLANNING → EXECUTION → VERIFICATION → DONE
 */
enum class TaskState {
    INIT,
    PLANNING,
    EXECUTION,
    VERIFICATION,
    DONE;

    companion object {
        fun fromString(value: String?): TaskState? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
