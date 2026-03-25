package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean
)

fun List<ToolResult>.toModelRequestMessages(): List<ModelRequest.Message> =
    map { ModelRequest.Message(role = ModelRequest.Role.Tool, content = it.content, toolCallId = it.toolCallId) }

sealed class ToolExecutionResult {
    data class Completed(val results: List<ToolResult>) : ToolExecutionResult()
    data class AwaitingApproval(val runId: String) : ToolExecutionResult()
}

interface ToolExecutor {
    suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        prompt: AContextMessage,
        loopMessages: List<ModelRequest.Message>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult
}
