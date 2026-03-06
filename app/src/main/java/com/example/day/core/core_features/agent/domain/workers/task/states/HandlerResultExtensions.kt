package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.HandlerResult

/**
 * Extension functions for converting between HandlerResult and StateResult.
 * Provides backward compatibility for existing state handlers.
 */

/**
 * Converts StateResult to HandlerResult for type-safe state management.
 */
fun StateResult.toHandlerResult(currentState: TaskState): HandlerResult {
    return if (nextState != null) {
        HandlerResult.Transition(
            targetState = nextState,
            update = null,  // Legacy conversion - update will be determined from memoryUpdates
            message = replyToUser,
            memoryUpdates = memoryUpdates,
            newStep = newStep
        )
    } else {
        HandlerResult.Success(
            message = replyToUser,
            stateUpdate = null,
            memoryUpdates = memoryUpdates,
            newStep = newStep
        )
    }
}

/**
 * Converts HandlerResult to StateResult for backward compatibility.
 */
fun HandlerResult.toStateResult(): StateResult {
    return when (this) {
        is HandlerResult.Success -> {
            StateResult(
                replyToUser = this.message,
                nextState = null,
                memoryUpdates = this.memoryUpdates,
                newStep = null
            )
        }
        
        is HandlerResult.Failure -> {
            StateResult(
                replyToUser = this.error,
                nextState = this.state,
                memoryUpdates = mapOf("error" to this.error),
                newStep = null
            )
        }
        
        is HandlerResult.Transition -> {
            StateResult(
                replyToUser = this.message,
                nextState = this.targetState,
                memoryUpdates = this.memoryUpdates,
                newStep = this.newStep
            )
        }
        
        is HandlerResult.Waiting -> {
            StateResult(
                replyToUser = this.message,
                nextState = null,
                memoryUpdates = emptyMap(),
                newStep = null
            )
        }
    }
}

/**
 * Extension property to extract TaskState from HandlerResult.Transition.
 */
val HandlerResult.Transition.targetStateTaskState: TaskState?
    get() = this.targetState

/**
 * Extension function to check if HandlerResult indicates a transition.
 */
fun HandlerResult.isTransition(): Boolean = this is HandlerResult.Transition

/**
 * Extension function to check if HandlerResult indicates failure.
 */
fun HandlerResult.isFailure(): Boolean = this is HandlerResult.Failure

/**
 * Extension function to get the error message if this is a failure.
 */
fun HandlerResult.errorMessage(): String? = (this as? HandlerResult.Failure)?.error
