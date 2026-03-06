package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.MemoryKey

/**
 * Memory key validation utilities.
 * 
 * Provides validation functions for memory keys to ensure type safety
 * and prevent invalid memory operations.
 */
object MemoryKeyValidator {
    
    /**
     * Validates a memory key is valid for the given state.
     */
    fun isValidForState(key: MemoryKey, state: TaskStateData): Boolean {
        return when (state) {
            is TaskStateData.Init -> key in validInitKeys
            is TaskStateData.Planning -> key in validPlanningKeys
            is TaskStateData.Execution -> key in validExecutionKeys
            is TaskStateData.Verification -> key in validVerificationKeys
            is TaskStateData.Done -> key in validDoneKeys
        }
    }
    
    /**
     * Returns the set of valid keys for INIT state.
     */
    val validInitKeys = setOf(
        MemoryKey.Planning,
        MemoryKey.CurrentState,
        MemoryKey.UserTask
    )
    
    /**
     * Returns the set of valid keys for PLANNING state.
     */
    val validPlanningKeys = setOf(
        MemoryKey.Planning,
        MemoryKey.Execution,
        MemoryKey.CurrentState,
        MemoryKey.UserTask,
        MemoryKey.Error
    )
    
    /**
     * Returns the set of valid keys for EXECUTION state.
     */
    val validExecutionKeys = setOf(
        MemoryKey.Execution,
        MemoryKey.Plan,
        MemoryKey.CurrentState,
        MemoryKey.CurrentStep,
        MemoryKey.TotalSteps,
        MemoryKey.UserTask,
        MemoryKey.Error
    )
    
    /**
     * Returns the set of valid keys for VERIFICATION state.
     */
    val validVerificationKeys = setOf(
        MemoryKey.Verification,
        MemoryKey.ExecutionResultsSummary,
        MemoryKey.Feedback,
        MemoryKey.CurrentState,
        MemoryKey.UserTask,
        MemoryKey.Error
    )
    
    /**
     * Returns the set of valid keys for DONE state.
     */
    val validDoneKeys = setOf(
        MemoryKey.Result,
        MemoryKey.VerificationPassed,
        MemoryKey.CurrentState,
        MemoryKey.UserTask
    )
    
    /**
     * Validates that a key exists in the MemoryKey sealed class.
     */
    fun isValidMemoryKey(key: String): Boolean {
        return try {
            MemoryKey.fromString(key)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets a validation error message if the key is invalid for the state.
     */
    fun getValidationError(key: MemoryKey, state: TaskStateData): String? {
        return if (!isValidForState(key, state)) {
            "Memory key '${key.key}' is not valid for state '${state::class.simpleName}'"
        } else {
            null
        }
    }
}
