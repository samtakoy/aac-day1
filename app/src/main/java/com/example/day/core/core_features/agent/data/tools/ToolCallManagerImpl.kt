package com.example.day.core.core_features.agent.data.tools

import com.example.day.core.core_features.agent.domain.tools.ConfirmationHandler
import com.example.day.core.core_features.agent.domain.tools.PendingToolCall
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallManager
import com.example.day.core.core_features.agent.domain.tools.ToolCallManagerResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolRegistry
import com.example.day.core.core_features.llm.domain.model.ModelResult
import java.util.UUID
import javax.inject.Inject

internal class ToolCallManagerImpl @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val confirmationHandler: ConfirmationHandler
) : ToolCallManager {

    override suspend fun handleToolCall(
        call: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): ToolCallManagerResult {
        val toolName = call.function.name
        val arguments = call.function.arguments

        if (confirmationHandler.requiresConfirmation(toolName, arguments)) {
            val confirmationId = UUID.randomUUID().toString()
            return ToolCallManagerResult.ConfirmationRequired(
                request = confirmationHandler.buildRequest(
                    toolName = toolName,
                    arguments = arguments,
                    confirmationId = confirmationId
                ),
                pending = PendingToolCall(
                    toolCallId = call.id,
                    toolName = toolName,
                    arguments = arguments,
                    type = call.type,
                    confirmationId = confirmationId
                )
            )
        }

        return ToolCallManagerResult.Executed(
            executePendingToolCall(
                pendingToolCall = PendingToolCall(
                    toolCallId = call.id,
                    toolName = toolName,
                    arguments = arguments,
                    type = call.type,
                    confirmationId = ""
                ),
                context = context
            )
        )
    }

    override suspend fun executePendingToolCall(
        pendingToolCall: PendingToolCall,
        context: ToolCallContext
    ): ToolExecutionResult {
        val result = toolRegistry.executeToolCall(
            toolCall = ModelResult.Success.ToolCall(
                id = pendingToolCall.toolCallId,
                type = pendingToolCall.type,
                function = ModelResult.Success.FunctionCall(
                    name = pendingToolCall.toolName,
                    arguments = pendingToolCall.arguments
                )
            ),
            context = context
        )

        return result.fold(
            onSuccess = { payload ->
                ToolExecutionResult.Success(payload)
            },
            onFailure = { error ->
                ToolExecutionResult.Failed(
                    error.message ?: ToolCallingConstants.UNKNOWN_TOOL_ERROR
                )
            }
        )
    }
}
