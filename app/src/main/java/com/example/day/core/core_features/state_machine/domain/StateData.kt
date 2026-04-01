package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateMessage
import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * Interface representing task state data.
 * Provides a flexible way to define custom states without recompilation.
 *
 * TODO возможно не используется
 */
interface StateData {
    /** The current state ID as String */
    val state: StateId
    val history: List<TaskStateMessage>
}
