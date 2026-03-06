package com.example.day.core.core_features.agent.domain.workers.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type-safe sealed class for state transitions in TaskWorker.
 * Each update type represents a valid state transition with associated data.
 */
@Serializable
sealed class TaskStateUpdate {
    
    /**
     * Start a new task - creates initial state
     * Transitions: null → INIT
     */
    @Serializable
    @SerialName("start_task")
    data class StartTask(
        @SerialName("user_task")
        val userTask: String
    ) : TaskStateUpdate()
    
    /**
     * Set planning error and optionally retry
     * Transitions: PLANNING → PLANNING (with error)
     */
    @Serializable
    @SerialName("set_planning_error")
    data class SetPlanningError(
        @SerialName("error")
        val error: String,
        
        @SerialName("retry_count")
        val retryCount: Int
    ) : TaskStateUpdate()
    
    /**
     * Transition from INIT to PLANNING
     * Transitions: INIT → PLANNING
     */
    @Serializable
    @SerialName("transition_to_planning")
    data class TransitionToPlanning(
        @SerialName("user_task")
        val userTask: String,
        
        @SerialName("planning_result")
        val planningResult: PlanningData? = null
    ) : TaskStateUpdate()
    
    /**
     * Transition from PLANNING to EXECUTION
     * Transitions: PLANNING → EXECUTION
     */
    @Serializable
    @SerialName("transition_to_execution")
    data class TransitionToExecution(
        @SerialName("user_task")
        val userTask: String,
        
        @SerialName("plan")
        val plan: PlanningData
    ) : TaskStateUpdate()
    
    /**
     * Start executing a step
     * Transitions: EXECUTION → EXECUTION (step starts)
     */
    @Serializable
    @SerialName("start_step")
    data class StartStep(
        @SerialName("step_number")
        val stepNumber: Int,
        
        @SerialName("step")
        val step: ExecutionStep
    ) : TaskStateUpdate()
    
    /**
     * Complete a step with result
     * Transitions: EXECUTION → EXECUTION (step completes)
     */
    @Serializable
    @SerialName("complete_step")
    data class CompleteStep(
        @SerialName("step_number")
        val stepNumber: Int,
        
        @SerialName("result")
        val result: String,
        
        @SerialName("status")
        val status: StepStatus
    ) : TaskStateUpdate()
    
    /**
     * Mark a step as failed
     * Transitions: EXECUTION → EXECUTION (step fails)
     */
    @Serializable
    @SerialName("fail_step")
    data class FailStep(
        @SerialName("step_number")
        val stepNumber: Int,
        
        @SerialName("error")
        val error: String
    ) : TaskStateUpdate()
    
    /**
     * Transition from EXECUTION to VERIFICATION
     * Transitions: EXECUTION → VERIFICATION
     */
    @Serializable
    @SerialName("transition_to_verification")
    data class TransitionToVerification(
        @SerialName("user_task")
        val userTask: String,
        
        @SerialName("execution_results")
        val executionResults: List<ExecutionStep>
    ) : TaskStateUpdate()
    
    /**
     * Set verification results
     * Transitions: VERIFICATION → VERIFICATION (with results)
     */
    @Serializable
    @SerialName("set_verification_results")
    data class SetVerificationResults(
        @SerialName("verification_results")
        val verificationResults: VerificationData
    ) : TaskStateUpdate()
    
    /**
     * Complete task with final result
     * Transitions: VERIFICATION → DONE
     *           or EXECUTION → DONE (if verification is skipped)
     */
    @Serializable
    @SerialName("complete_with_result")
    data class CompleteWithResult(
        @SerialName("user_task")
        val userTask: String,
        
        @SerialName("final_result")
        val finalResult: String,
        
        @SerialName("verification_passed")
        val verificationPassed: Boolean
    ) : TaskStateUpdate()
}
