package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskState

/**
 * Result type for state handler operations.
 * 
 * Provides type-safe responses from state handlers with:
 * - Success/failure indication
 * - Optional error message for failures
 * - Optional state data for responses that include updated state
 * - Optional state update to apply
 */
sealed class HandlerResult {
    
    /**
     * Indicates successful handler execution.
     */
    data class Success(
        val message: String = "",
        val stateUpdate: TaskStateUpdate? = null,
        val memoryUpdates: Map<String, String> = emptyMap(),
        val newStep: Int? = null
    ) : HandlerResult()
    
    /**
     * Indicates handler execution failed.
     */
    data class Failure(
        val error: String,
        val state: TaskState? = null,
        val memoryUpdates: Map<String, String> = emptyMap()
    ) : HandlerResult()
    
    /**
     * Indicates handler needs to transition to a new state.
     * Used when the handler completes its work and wants to trigger
     * a state transition.
     */
    data class Transition(
        val targetState: TaskState,
        val update: TaskStateUpdate? = null,
        val message: String = "",
        val memoryUpdates: Map<String, String> = emptyMap(),
        val newStep: Int? = null
    ) : HandlerResult()
    
    /**
     * Indicates handler needs to wait for an async operation.
     * Used when handler initiates but doesn't complete an operation.
     */
    data class Waiting(
        val message: String = "",
        val memoryUpdates: Map<String, String> = emptyMap()
    ) : HandlerResult()
    
    companion object {
        fun success(message: String = "") = Success(message)
        
        fun successWithUpdate(update: TaskStateUpdate) = Success(stateUpdate = update)
        
        fun successWithMemory(message: String = "", memoryUpdates: Map<String, String> = emptyMap()) = 
            Success(message = message, memoryUpdates = memoryUpdates)
        
        fun failure(error: String) = Failure(error)
        
        fun failureWithState(error: String, state: TaskState) = Failure(error, state)
        
        fun transition(targetState: TaskState, update: TaskStateUpdate? = null) = Transition(targetState, update)
        
        fun transitionWithData(
            targetState: TaskState, 
            update: TaskStateUpdate,
            message: String = "",
            memoryUpdates: Map<String, String> = emptyMap(),
            newStep: Int? = null
        ) = Transition(targetState, update, message, memoryUpdates, newStep)
        
        fun waiting(message: String = "") = Waiting(message)
    }
}

/**
 * Extension to convert HandlerResult to nullable, useful for operators.
 */
fun HandlerResult?.toFailure(defaultError: String = "Unknown error"): HandlerResult {
    return this ?: HandlerResult.failure(defaultError)
}
