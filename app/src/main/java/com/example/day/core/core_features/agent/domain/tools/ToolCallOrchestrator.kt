package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent

interface ToolCallOrchestrator {
    suspend fun execute(
        request: OrchestratorRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<OrchestratorResult>
}
