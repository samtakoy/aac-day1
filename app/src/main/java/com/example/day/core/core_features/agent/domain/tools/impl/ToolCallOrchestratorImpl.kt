package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.tools.OrchestratorRequest
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase
) : ToolCallOrchestrator {

    companion object {
        private const val TAG = "ToolCallOrchestrator"
    }

    override suspend fun execute(
        request: OrchestratorRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<OrchestratorResult> {
        onEvent?.invoke(WorkerEvent.RequestStart)

        val llmResult = llmProvider.exec(
            modelSettings = request.modelSettings,
            systemPrompt = request.systemPrompt,
            messages = request.messages,
            prompt = null,
            tools = request.tools.ifEmpty { null }
        ).onSuccess { onEvent?.invoke(WorkerEvent.RequestSuccess(it)) }
         .onFailure { onEvent?.invoke(WorkerEvent.RequestError(it.message ?: "error")) }
         .getOrElse { return Result.failure(it) }

        val choice = llmResult.choices.firstOrNull()
        val toolCalls = choice?.message?.toolCalls

        if (toolCalls.isNullOrEmpty()) {
            val responseText = llmResult.getContent()
            Log.d(TAG, "No tool calls — final response")
            return Result.success(
                OrchestratorResult.Completed(
                    responseText = responseText,
                    assistantMessage = ModelRequest.Message(ModelRequest.Role.Assistant, responseText)
                )
            )
        }

        Log.d(TAG, "${toolCalls.size} tool calls requested")
        val assistantMessage = ModelRequest.Message(
            role = ModelRequest.Role.Assistant,
            content = choice.message.content.orEmpty(),
            toolCalls = toolCalls.map { call ->
                ModelRequest.ToolCall(
                    id = call.id, type = call.type,
                    function = ModelRequest.FunctionCall(call.function.name, call.function.arguments)
                )
            }
        )
        return Result.success(OrchestratorResult.PendingApproval(toolCalls, assistantMessage))
    }
}
