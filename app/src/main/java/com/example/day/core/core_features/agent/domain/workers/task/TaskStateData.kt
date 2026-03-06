package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type-safe sealed class representing all TaskWorker states with associated data.
 * Provides compile-time validation for state transitions and eliminates string-based state storage.
 * 
 * Lifecycle: INIT → PLANNING → EXECUTION → VERIFICATION → DONE
 */
@Serializable
sealed class TaskStateData {
    /** The current state of the task */
    abstract val state: TaskState
    
    /** The original user task description */
    abstract val userTask: String
    
    /**
     * Initial state when a new task is created.
     * This is the entry point for any new task.
     */
    @Serializable
    @SerialName("init")
    data class Init(
        @SerialName("user_task")
        override val userTask: String,
        
        @SerialName("created_at")
        val createdAt: Long = System.currentTimeMillis()
    ) : TaskStateData() {
        override val state: TaskState = TaskState.INIT
    }
    
    /**
     * Planning phase - the system is analyzing the task and creating a plan.
     * Contains the planning result if available, otherwise null during initial planning.
     */
    @Serializable
    @SerialName("planning")
    data class Planning(
        @SerialName("user_task")
        override val userTask: String,
        
        @SerialName("planning_result")
        val planningResult: PlanningData? = null,
        
        @SerialName("retry_count")
        val retryCount: Int = 0,
        
        @SerialName("error_message")
        val errorMessage: String? = null
    ) : TaskStateData() {
        override val state: TaskState = TaskState.PLANNING
    }
    
    /**
     * Execution phase - the plan is being executed step by step.
     * Contains the plan, execution results, and current step information.
     */
    @Serializable
    @SerialName("execution")
    data class Execution(
        @SerialName("user_task")
        override val userTask: String,
        
        @SerialName("plan")
        val plan: PlanningData,
        
        @SerialName("execution_results")
        val executionResults: List<ExecutionStep> = emptyList(),
        
        @SerialName("current_step")
        val currentStep: Int = 0,
        
        @SerialName("is_step_running")
        val isStepRunning: Boolean = false
    ) : TaskStateData() {
        override val state: TaskState = TaskState.EXECUTION
    }
    
    /**
     * Verification phase - the execution results are being verified.
     * Contains the plan, execution results, and verification results.
     */
    @Serializable
    @SerialName("verification")
    data class Verification(
        @SerialName("user_task")
        override val userTask: String,
        
        @SerialName("plan")
        val plan: PlanningData,
        
        @SerialName("execution_results")
        val executionResults: List<ExecutionStep>,
        
        @SerialName("verification_results")
        val verificationResults: VerificationData? = null,
        
        @SerialName("retry_count")
        val retryCount: Int = 0
    ) : TaskStateData() {
        override val state: TaskState = TaskState.VERIFICATION
    }
    
    /**
     * Done state - the task has completed.
     * Contains the final result and whether verification passed.
     */
    @Serializable
    @SerialName("done")
    data class Done(
        @SerialName("user_task")
        override val userTask: String,
        
        @SerialName("final_result")
        val finalResult: String,
        
        @SerialName("verification_passed")
        val verificationPassed: Boolean,
        
        @SerialName("completed_at")
        val completedAt: Long = System.currentTimeMillis()
    ) : TaskStateData() {
        override val state: TaskState = TaskState.DONE
    }
    
    companion object {
        /**
         * Factory method to create initial state for a new task
         */
        fun initial(userTask: String) = Init(userTask = userTask)
    }
}

/**
 * Planning data containing the task breakdown and reasoning
 */
@Serializable
@SerialName("planning_data")
data class PlanningData(
    @SerialName("task_breakdown")
    val taskBreakdown: List<TaskStep>,
    
    @SerialName("reasoning")
    val reasoning: String,
    
    @SerialName("estimated_steps")
    val estimatedSteps: Int
)

/**
 * A single step in the task breakdown
 */
@Serializable
@SerialName("task_step")
data class TaskStep(
    @SerialName("step_number")
    val stepNumber: Int,
    
    @SerialName("description")
    val description: String,
    
    @SerialName("expert")
    val expert: String? = null,
    
    @SerialName("artifact")
    val artifact: String? = null
)

/**
 * An execution step with status and results
 */
@Serializable
@SerialName("execution_step")
data class ExecutionStep(
    @SerialName("step_number")
    val stepNumber: Int,
    
    @SerialName("description")
    val description: String,
    
    @SerialName("tool")
    val tool: String? = null,
    
    @SerialName("parameters")
    val parameters: Map<String, String> = emptyMap(),
    
    @SerialName("result")
    val result: String? = null,
    
    @SerialName("status")
    val status: StepStatus = StepStatus.PENDING,
    
    @SerialName("error")
    val error: String? = null,
    
    @SerialName("started_at")
    val startedAt: Long? = null,
    
    @SerialName("completed_at")
    val completedAt: Long? = null
)

/**
 * Status of an execution step
 */
@Serializable
enum class StepStatus {
    @SerialName("pending")
    PENDING,
    
    @SerialName("running")
    RUNNING,
    
    @SerialName("completed")
    COMPLETED,
    
    @SerialName("failed")
    FAILED,
    
    @SerialName("skipped")
    SKIPPED
}

/**
 * Verification data containing checks and overall result
 */
@Serializable
@SerialName("verification_data")
data class VerificationData(
    @SerialName("checks")
    val checks: List<VerificationCheck>,
    
    @SerialName("overall_passed")
    val overallPassed: Boolean,
    
    @SerialName("feedback")
    val feedback: String? = null
)

/**
 * A single verification check
 */
@Serializable
@SerialName("verification_check")
data class VerificationCheck(
    @SerialName("name")
    val name: String,
    
    @SerialName("passed")
    val passed: Boolean,
    
    @SerialName("details")
    val details: String? = null
)
