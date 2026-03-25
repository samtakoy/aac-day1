package com.example.day.core.core_features.agent.domain.tools.hitl

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import javax.inject.Inject

class HitlToolExecutor @Inject constructor(
    private val sessionManager: HitlSessionManager
) : ToolExecutor {

    override suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        prompt: AContextMessage,
        loopMessages: List<ModelRequest.Message>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult {
        val session = HitlSession(
            runId = runId,
            agentId = context.agentId,
            prompt = prompt,
            loopMessages = loopMessages,
            pendingToolCalls = toolCalls
        )
        sessionManager.createSession(session)

        for (call in toolCalls) {
            onEvent?.invoke(
                WorkerEvent.ApprovalRequired(
                    runId = runId,
                    toolCallId = call.id,
                    toolName = call.function.name,
                    arguments = call.function.arguments
                )
            )
        }

        return ToolExecutionResult.AwaitingApproval(runId)
    }
}
