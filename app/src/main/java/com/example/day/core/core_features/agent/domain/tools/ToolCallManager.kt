package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelResult

interface ToolCallManager {
    suspend fun handleToolCall(
        call: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): ToolCallManagerResult

    suspend fun executePendingToolCall(
        pendingToolCall: PendingToolCall,
        context: ToolCallContext
    ): ToolExecutionResult
}

sealed interface ToolCallManagerResult {
    data class Executed(val result: ToolExecutionResult) : ToolCallManagerResult
    data class ConfirmationRequired(
        val request: ConfirmationRequest,
        val pending: PendingToolCall
    ) : ToolCallManagerResult
}
