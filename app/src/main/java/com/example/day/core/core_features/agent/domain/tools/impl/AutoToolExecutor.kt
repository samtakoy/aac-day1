package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import javax.inject.Inject

class AutoToolExecutor @Inject constructor(
    private val toolProvider: ToolProvider
) : ToolExecutor {

    companion object { private const val TAG = "AutoToolExecutor" }

    override suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        prompt: AContextMessage,
        loopMessages: List<ModelRequest.Message>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult {
        val results = toolCalls.map { call ->
            onEvent?.invoke(WorkerEvent.ToolCallStarted(call.id, call.function.name, call.function.arguments))
            Log.d(TAG, "Executing: ${call.function.name}")

            val toolResult = toolProvider.executeToolCall(call, context)
            val content = toolResult.getOrElse { err ->
                "${ToolCallingConstants.MCP_TOOL_ERROR_PREFIX}: ${err.message ?: ToolCallingConstants.UNKNOWN_TOOL_ERROR}"
            }

            onEvent?.invoke(WorkerEvent.ToolCallFinished(call.id, call.function.name, content, toolResult.isFailure))
            ToolResult(toolCallId = call.id, content = content, isError = toolResult.isFailure)
        }
        return ToolExecutionResult.Completed(results)
    }
}
