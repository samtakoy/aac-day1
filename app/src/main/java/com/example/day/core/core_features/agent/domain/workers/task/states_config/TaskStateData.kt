package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.state_machine.domain.StateData
import com.example.day.core.core_features.state_machine.domain.model.StateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskStateData : StateData {

    // @SerialName("history")
    // override val history: List<TaskStateMessage>

    /**
     * Initial state when a new task is created.
     * This is the entry point for any new task.
     */
    @Serializable
    data class Init(
        @SerialName("title")
        val title: String? = null,
        @SerialName("description")
        val description: String? = null,
        @SerialName("goal")
        val goal: String? = null,
        @SerialName("expert")
        val expert: String? = null,
        @SerialName("history")
        override val history: List<TaskStateMessage> = emptyList(),
    ) : TaskStateData {
        override val state: StateId = TaskStateConfig.INIT
    }

    /**
     * Planning phase - the system is analyzing the task and creating a plan.
     * Contains the planning result if available, otherwise null during initial planning.
     */
    @Serializable
    data class Planning(
        @SerialName("total_steps")
        val totalSteps: Int = 0,
        @SerialName("steps")
        val steps: List<TaskStep> = emptyList(),
        @SerialName("task_details")
        val taskDetails: String = "",
        @SerialName("history")
        override val history: List<TaskStateMessage> = emptyList(),
    ) : TaskStateData {
        override val state: StateId = TaskStateConfig.PLANNING
    }

    /**
     * Execution phase - the plan is being executed step by step.
     * Contains the plan, execution results, and current step information.
     */
    @Serializable
    data class Execution(
        @SerialName("step_number")
        val stepNumber: Int,
        @SerialName("result")
        val result: String? = null,
        @SerialName("history")
        override val history: List<TaskStateMessage> = emptyList(),
    ) : TaskStateData {
        override val state: StateId = TaskStateConfig.EXECUTION
    }

    /**
     * Verification phase - the execution results are being verified.
     */
    @Serializable
    data class Verification(
        @SerialName("retry_count")
        val retryCount: Int = 0,
        @SerialName("review_details")
        val stepResults: List<ReviewItem> = emptyList(),
        @SerialName("score")
        val score: Int? = null,
        @SerialName("resume")
        val resume: String? = null,
        @SerialName("history")
        override val history: List<TaskStateMessage> = emptyList(),
    ) : TaskStateData {
        override val state: StateId = TaskStateConfig.VERIFICATION
    }

    /**
     * Done state - the task has completed.
     */
    @Serializable
    data class Done(
        @SerialName("history")
        override val history: List<TaskStateMessage> = emptyList(),
    ) : TaskStateData {
        override val state: StateId = TaskStateConfig.DONE
    }

    // ==============
    // Helper classes
    // ==============

    /**
     * A single step in the task breakdown
     */
    @Serializable
    data class TaskStep(
        @SerialName("step_number")
        val stepNumber: Int,

        @SerialName("title")
        val title: String,

        @SerialName("description")
        val description: String,

        @SerialName("expert")
        val expert: String? = null,

        @SerialName("completed")
        val isCompleted: Boolean = false,
    )

    @Serializable
    data class ReviewItem(
        @SerialName("step_number")
        val stepNumber: Int,
        @SerialName("score")
        val score: Int,
        @SerialName("review")
        val feedback: String = ""
    )
}