package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent

interface ToolCallOrchestrator {
    suspend fun execute(
        request: LlmExecutionRequest,
        runId: String,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<ToolCallingResult>

    suspend fun resume(
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<ToolCallingResult>
}
