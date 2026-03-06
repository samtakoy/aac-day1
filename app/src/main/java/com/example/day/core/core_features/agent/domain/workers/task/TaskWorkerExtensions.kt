package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskState

/**
 * Extension functions for integrating TaskStateStore with TaskWorker.
 * 
 * These extensions provide optional integration between the legacy TaskWorker
 * and the new type-safe TaskStateStore. The integration is opt-in and
 * maintains backward compatibility.
 */

/**
 * Data class to hold integrated state information.
 */
data class IntegratedState(
    val stateData: TaskStateData?,
    val legacyState: TaskState,
    val usesNewFormat: Boolean
)

/**
 * Extension function to check if new format state exists.
 */
suspend fun TaskStateStore.checkNewFormatExists(agentId: String): Boolean {
    return getState(agentId) != null
}

/**
 * Extension function to get integrated state (prefers new format).
 */
suspend fun TaskStateStore.getIntegratedState(agentId: String): IntegratedState {
    // Try new format first
    val newState = getState(agentId)
    if (newState != null) {
        return IntegratedState(
            stateData = newState,
            legacyState = newState.toTaskState(),
            usesNewFormat = true
        )
    }
    
    // Return empty state - legacy will be handled separately
    return IntegratedState(
        stateData = null,
        legacyState = TaskState.INIT,
        usesNewFormat = false
    )
}

/**
 * Extension function to start a new task with type-safe state.
 */
suspend fun TaskStateStore.startNewTask(agentId: String, userTask: String): TaskStateData {
    return updateState(agentId, TaskStateUpdate.StartTask(userTask))
}

/**
 * Extension function to transition to planning with type-safe state.
 */
suspend fun TaskStateStore.transitionToPlanning(
    agentId: String,
    userTask: String,
    planningResult: PlanningData?
): TaskStateData {
    return updateState(agentId, TaskStateUpdate.TransitionToPlanning(
        userTask = userTask,
        planningResult = planningResult ?: createEmptyPlanningData()
    ))
}

private fun createEmptyPlanningData(): PlanningData {
    return PlanningData(
        taskBreakdown = emptyList(),
        reasoning = "",
        estimatedSteps = 0
    )
}

/**
 * Extension function to transition to execution with type-safe state.
 */
suspend fun TaskStateStore.transitionToExecution(
    agentId: String,
    userTask: String,
    plan: PlanningData
): TaskStateData {
    return updateState(agentId, TaskStateUpdate.TransitionToExecution(
        userTask = userTask,
        plan = plan
    ))
}

/**
 * Extension function to transition to verification with type-safe state.
 */
suspend fun TaskStateStore.transitionToVerification(
    agentId: String,
    userTask: String,
    executionResults: List<ExecutionStep>
): TaskStateData {
    return updateState(agentId, TaskStateUpdate.TransitionToVerification(
        userTask = userTask,
        executionResults = executionResults
    ))
}

/**
 * Extension function to complete a step with type-safe state.
 */
suspend fun TaskStateStore.completeStep(
    agentId: String,
    stepNumber: Int,
    result: String,
    status: StepStatus
): TaskStateData {
    return updateState(agentId, TaskStateUpdate.CompleteStep(
        stepNumber = stepNumber,
        result = result,
        status = status
    ))
}

/**
 * Extension function to complete task with final result.
 */
suspend fun TaskStateStore.completeWithResult(
    agentId: String,
    userTask: String,
    finalResult: String,
    verificationPassed: Boolean
): TaskStateData {
    return updateState(agentId, TaskStateUpdate.CompleteWithResult(
        userTask = userTask,
        finalResult = finalResult,
        verificationPassed = verificationPassed
    ))
}

/**
 * Extension function to convert TaskStateData to TaskState.
 */
fun TaskStateData.toTaskState(): TaskState {
    return when (this) {
        is TaskStateData.Init -> TaskState.INIT
        is TaskStateData.Planning -> TaskState.PLANNING
        is TaskStateData.Execution -> TaskState.EXECUTION
        is TaskStateData.Verification -> TaskState.VERIFICATION
        is TaskStateData.Done -> TaskState.DONE
    }
}
