package com.example.day.core.core_features.agent.domain.workers.base

import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Worker events for notifying about LLM request lifecycle.
 */
sealed interface WorkerEvent {
    /** Agent notifies that it will make a request - sends information before each LLM request */
    object RequestStart : WorkerEvent
    /** Agent notifies that it received a successful response to the request */
    class RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    /** Agent notifies that the LLM request returned an error */
    class RequestError(val text: String) : WorkerEvent
}
