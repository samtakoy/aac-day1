package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

sealed class OrchestratorResult {
    data class Completed(
        val responseText: String,
        val assistantMessage: ModelRequest.Message
    ) : OrchestratorResult()

    data class PendingApproval(
        val toolCalls: List<ModelResult.Success.ToolCall>,
        val assistantMessage: ModelRequest.Message
    ) : OrchestratorResult()
}
