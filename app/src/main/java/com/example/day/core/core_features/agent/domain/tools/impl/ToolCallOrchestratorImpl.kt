package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.tools.AssistantToolCall
import com.example.day.core.core_features.agent.domain.tools.ExecutionSession
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionManager
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionStatus
import com.example.day.core.core_features.agent.domain.tools.LlmExecutionRequest
import com.example.day.core.core_features.agent.domain.tools.PendingToolCall
import com.example.day.core.core_features.agent.domain.tools.ToolCallManager
import com.example.day.core.core_features.agent.domain.tools.ToolCallManagerResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolCallSession
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolCallingResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessage
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase,
    private val executionSessionManager: ExecutionSessionManager,
    private val toolCallManager: ToolCallManager
) : ToolCallOrchestrator {

    private companion object {
        private const val TAG = "ToolCallOrchestrator(ktor)"
        private const val MAX_TOOL_LOOPS = ToolCallingConstants.MAX_TOOL_LOOPS
        private const val USER_DENIED_TOOL = "Tool execution denied by user"
    }

    override suspend fun execute(
        request: LlmExecutionRequest,
        runId: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> {
        var session = executionSessionManager.get(runId)
            ?: return Result.failure(IllegalStateException("Execution session not found: $runId"))

        val messages = if (session.llmMessages.isNotEmpty()) {
            session.llmMessages.toMutableList()
        } else {
            buildInitialMessages(request)
        }
        val newMessages = if (session.newMessages.isNotEmpty()) {
            session.newMessages.toMutableList()
        } else {
            buildInitialNewMessages(request)
        }
        val toolCallSessions = session.toolCallSessions.toMutableList()

        session = session.copy(
            status = ExecutionSessionStatus.Running,
            llmMessages = messages.toList(),
            newMessages = newMessages.toList(),
            toolCallSessions = toolCallSessions.toList()
        )
        executionSessionManager.update(session)

        return continueExecution(
            runId = runId,
            session = session,
            request = request,
            messages = messages,
            newMessages = newMessages,
            toolCallSessions = toolCallSessions,
            onEvent = onEvent
        )
    }

    override suspend fun resume(
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> {
        var session = executionSessionManager.get(runId)
            ?: return Result.failure(IllegalStateException("Execution session not found: $runId"))

        val pendingToolCall = session.pendingToolCall
        if (pendingToolCall == null) {
            return if (session.resolvedConfirmationId == confirmationId) {
                Result.success(buildResultFromSession(session))
            } else {
                Result.failure(IllegalStateException("No pending confirmation for runId: $runId"))
            }
        }

        if (pendingToolCall.confirmationId != confirmationId) {
            return Result.failure(IllegalStateException("Stale confirmationId for runId: $runId"))
        }

        val request = session.requestSnapshot
        val messages = session.llmMessages.toMutableList()
        val newMessages = session.newMessages.toMutableList()
        val toolCallSessions = session.toolCallSessions.toMutableList()
        val assistantToolCall = session.currentAssistantToolCall
            ?: return Result.failure(IllegalStateException("Missing assistant tool call session for runId: $runId"))
        val toolResults = session.currentToolResults.toMutableList()
        val toolMessages = toolResults.map(::toToolMessage).toMutableList()

        val pendingResult = if (approved) {
            toolCallManager.executePendingToolCall(pendingToolCall, request.context)
        } else {
            ToolExecutionResult.Denied(USER_DENIED_TOOL)
        }

        val resolvedToolResult = toToolResult(pendingToolCall, pendingResult)
        toolResults.add(resolvedToolResult)
        toolMessages.add(toToolMessage(resolvedToolResult))
        onEvent?.invoke(
            WorkerEvent.Tool.ToolCallFinished(
                toolCallId = pendingToolCall.toolCallId,
                toolName = pendingToolCall.toolName,
                result = resolvedToolResult.content,
                isError = resolvedToolResult.isError
            )
        )

        for (index in (session.currentToolIndex + 1) until assistantToolCall.toolCalls.size) {
            val call = assistantToolCall.toolCalls[index]
            onEvent?.invoke(
                WorkerEvent.Tool.ToolCallStarted(
                    toolCallId = call.id,
                    toolName = call.function.name,
                    arguments = call.function.arguments
                )
            )

            when (val managedCall = toolCallManager.handleToolCall(call, request.context)) {
                is ToolCallManagerResult.Executed -> {
                    val toolResult = toToolResult(
                        pendingToolCall = PendingToolCall(
                            toolCallId = call.id,
                            toolName = call.function.name,
                            arguments = call.function.arguments,
                            type = call.type,
                            confirmationId = ""
                        ),
                        executionResult = managedCall.result
                    )
                    toolResults.add(toolResult)
                    toolMessages.add(toToolMessage(toolResult))
                    onEvent?.invoke(
                        WorkerEvent.Tool.ToolCallFinished(
                            toolCallId = call.id,
                            toolName = call.function.name,
                            result = toolResult.content,
                            isError = toolResult.isError
                        )
                    )
                }

                is ToolCallManagerResult.ConfirmationRequired -> {
                    session = session.copy(
                        status = ExecutionSessionStatus.WaitingUserConfirmation,
                        pendingToolCall = managedCall.pending,
                        currentAssistantToolCall = assistantToolCall,
                        currentToolResults = toolResults.toList(),
                        currentToolIndex = index,
                        resolvedConfirmationId = confirmationId
                    )
                    executionSessionManager.update(session)
                    onEvent?.invoke(
                        WorkerEvent.UserConfirmation.Requested(
                            confirmationId = managedCall.request.confirmationId,
                            runId = runId,
                            title = managedCall.request.title,
                            message = managedCall.request.message,
                            actionLabel = managedCall.request.actionLabel
                        )
                    )
                    return Result.success(
                        ToolCallingResult(
                            finalResponseText = session.finalResponseText,
                            toolCallSessions = toolCallSessions,
                            allMessages = newMessages.toAContextMessages(),
                            isPaused = true,
                            pendingConfirmationId = managedCall.request.confirmationId,
                            runId = runId
                        )
                    )
                }
            }
        }

        messages.addAll(toolMessages)
        newMessages.addAll(toolMessages)
        toolCallSessions.add(
            ToolCallSession(
                assistantMessage = assistantToolCall,
                toolResults = toolResults.toList()
            )
        )

        session = session.copy(
            status = ExecutionSessionStatus.Running,
            pendingToolCall = null,
            currentAssistantToolCall = null,
            currentToolResults = emptyList(),
            currentToolIndex = 0,
            llmMessages = messages.toList(),
            newMessages = newMessages.toList(),
            toolCallSessions = toolCallSessions.toList(),
            resolvedConfirmationId = confirmationId
        )
        executionSessionManager.update(session)

        return continueExecution(
            runId = runId,
            session = session,
            request = request,
            messages = messages,
            newMessages = newMessages,
            toolCallSessions = toolCallSessions,
            onEvent = onEvent
        )
    }

    private suspend fun continueExecution(
        runId: String,
        session: ExecutionSession,
        request: LlmExecutionRequest,
        messages: MutableList<ModelRequest.Message>,
        newMessages: MutableList<ModelRequest.Message>,
        toolCallSessions: MutableList<ToolCallSession>,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ToolCallingResult> {
        var currentSession = session
        var loopIndex = 0
        var lastLlmResult: ModelResult.Success? = null

        while (loopIndex < MAX_TOOL_LOOPS) {
            val llmResult = llmProvider.askLlm(
                model = request.modelSettings,
                prompt = null,
                systemPrompt = request.systemPrompt,
                history = messages,
                tools = request.tools.ifEmpty { null },
                onEvent = onEvent
            ).getOrElse { error ->
                Log.e(TAG, "LLM request failed on iteration $loopIndex", error)
                return Result.failure(error)
            }

            lastLlmResult = llmResult
            val choice = llmResult.choices.firstOrNull()
            val toolCalls = choice?.message?.toolCalls

            if (toolCalls.isNullOrEmpty()) {
                currentSession = currentSession.copy(
                    status = ExecutionSessionStatus.Completed,
                    llmMessages = messages.toList(),
                    newMessages = newMessages.toList(),
                    toolCallSessions = toolCallSessions.toList(),
                    pendingToolCall = null,
                    currentAssistantToolCall = null,
                    currentToolResults = emptyList(),
                    currentToolIndex = 0,
                    finalResponseText = llmResult.getContent()
                )
                executionSessionManager.update(currentSession)
                return Result.success(buildResultFromSession(currentSession))
            }

            val assistantMessage = ModelRequest.Message(
                role = ModelRequest.Role.Assistant,
                content = choice.message.content,
                toolCalls = toolCalls.map { call ->
                    ModelRequest.ToolCall(
                        id = call.id,
                        type = call.type,
                        function = ModelRequest.FunctionCall(
                            name = call.function.name,
                            arguments = call.function.arguments
                        )
                    )
                }
            )
            val assistantToolCall = AssistantToolCall(
                content = choice.message.content,
                toolCalls = toolCalls.toList()
            )
            messages.add(assistantMessage)
            newMessages.add(assistantMessage)

            val toolResults = mutableListOf<ToolResult>()
            val toolMessages = mutableListOf<ModelRequest.Message>()

            for ((index, call) in toolCalls.withIndex()) {
                onEvent?.invoke(
                    WorkerEvent.Tool.ToolCallStarted(
                        toolCallId = call.id,
                        toolName = call.function.name,
                        arguments = call.function.arguments
                    )
                )

                when (val managedCall = toolCallManager.handleToolCall(call, request.context)) {
                    is ToolCallManagerResult.Executed -> {
                        val toolResult = toToolResult(
                            pendingToolCall = PendingToolCall(
                                toolCallId = call.id,
                                toolName = call.function.name,
                                arguments = call.function.arguments,
                                type = call.type,
                                confirmationId = ""
                            ),
                            executionResult = managedCall.result
                        )
                        toolResults.add(toolResult)
                        toolMessages.add(toToolMessage(toolResult))
                        onEvent?.invoke(
                            WorkerEvent.Tool.ToolCallFinished(
                                toolCallId = call.id,
                                toolName = call.function.name,
                                result = toolResult.content,
                                isError = toolResult.isError
                            )
                        )
                    }

                    is ToolCallManagerResult.ConfirmationRequired -> {
                        currentSession = currentSession.copy(
                            status = ExecutionSessionStatus.WaitingUserConfirmation,
                            pendingToolCall = managedCall.pending,
                            llmMessages = messages.toList(),
                            newMessages = newMessages.toList(),
                            toolCallSessions = toolCallSessions.toList(),
                            currentAssistantToolCall = assistantToolCall,
                            currentToolResults = toolResults.toList(),
                            currentToolIndex = index,
                            finalResponseText = lastLlmResult?.getContent().orEmpty()
                        )
                        executionSessionManager.update(currentSession)
                        onEvent?.invoke(
                            WorkerEvent.UserConfirmation.Requested(
                            confirmationId = managedCall.request.confirmationId,
                            runId = runId,
                            title = managedCall.request.title,
                                message = managedCall.request.message,
                                actionLabel = managedCall.request.actionLabel
                            )
                        )
                        return Result.success(
                            ToolCallingResult(
                                finalResponseText = currentSession.finalResponseText,
                                toolCallSessions = toolCallSessions,
                                allMessages = newMessages.toAContextMessages(),
                                isPaused = true,
                                pendingConfirmationId = managedCall.request.confirmationId,
                                runId = runId
                            )
                        )
                    }
                }
            }

            messages.addAll(toolMessages)
            newMessages.addAll(toolMessages)
            toolCallSessions.add(
                ToolCallSession(
                    assistantMessage = assistantToolCall,
                    toolResults = toolResults.toList()
                )
            )

            currentSession = currentSession.copy(
                status = ExecutionSessionStatus.Running,
                llmMessages = messages.toList(),
                newMessages = newMessages.toList(),
                toolCallSessions = toolCallSessions.toList(),
                pendingToolCall = null,
                currentAssistantToolCall = null,
                currentToolResults = emptyList(),
                currentToolIndex = 0,
                finalResponseText = lastLlmResult.getContent()
            )
            executionSessionManager.update(currentSession)
            loopIndex++
        }

        currentSession = currentSession.copy(
            status = ExecutionSessionStatus.Completed,
            llmMessages = messages.toList(),
            newMessages = newMessages.toList(),
            toolCallSessions = toolCallSessions.toList(),
            finalResponseText = lastLlmResult?.getContent().orEmpty()
        )
        executionSessionManager.update(currentSession)
        return Result.success(buildResultFromSession(currentSession))
    }

    private fun buildInitialMessages(request: LlmExecutionRequest): MutableList<ModelRequest.Message> {
        return (request.memoryMessages.map { it.toModelRequestMessage() } + request.initialHistory).toMutableList().apply {
            if (request.prompt.content?.isBlank() == false) {
                add(request.prompt.toModelRequestMessage())
            }
        }
    }

    private fun buildInitialNewMessages(request: LlmExecutionRequest): MutableList<ModelRequest.Message> {
        return buildList {
            addAll(request.initialHistory)
            if (request.prompt.content?.isBlank() == false) {
                add(request.prompt.toModelRequestMessage())
            }
        }.toMutableList()
    }

    private fun toToolResult(
        pendingToolCall: PendingToolCall,
        executionResult: ToolExecutionResult
    ): ToolResult {
        val content = when (executionResult) {
            is ToolExecutionResult.Success -> executionResult.payload
            is ToolExecutionResult.Denied -> executionResult.reason
            is ToolExecutionResult.Failed -> {
                "${ToolCallingConstants.MCP_TOOL_ERROR_PREFIX}: ${executionResult.error}"
            }
        }
        val isError = executionResult is ToolExecutionResult.Failed
        return ToolResult(
            toolCallId = pendingToolCall.toolCallId,
            content = content,
            isError = isError
        )
    }

    private fun toToolMessage(toolResult: ToolResult): ModelRequest.Message {
        return ModelRequest.Message(
            role = ModelRequest.Role.Tool,
            content = toolResult.content,
            toolCallId = toolResult.toolCallId
        )
    }

    private fun buildResultFromSession(session: ExecutionSession): ToolCallingResult {
        return ToolCallingResult(
            finalResponseText = session.finalResponseText,
            toolCallSessions = session.toolCallSessions,
            allMessages = session.newMessages.toAContextMessages(),
            isPaused = session.status == ExecutionSessionStatus.WaitingUserConfirmation,
            pendingConfirmationId = session.pendingToolCall?.confirmationId,
            runId = session.runId
        )
    }

    private fun List<ModelRequest.Message>.toAContextMessages(): List<AContextMessage> {
        return map { msg ->
            AContextMessage(
                role = when (msg.role) {
                    ModelRequest.Role.System -> AContextMessage.Role.SYSTEM
                    ModelRequest.Role.User -> AContextMessage.Role.USER
                    ModelRequest.Role.Assistant -> AContextMessage.Role.ASSISTANT
                    ModelRequest.Role.Tool -> AContextMessage.Role.TOOL
                },
                content = msg.content,
                toolCallId = msg.toolCallId,
                toolCalls = msg.toolCalls?.map { call ->
                    AContextMessage.ToolCallRef(
                        id = call.id,
                        type = call.type,
                        functionName = call.function.name,
                        arguments = call.function.arguments
                    )
                }
            )
        }
    }
}



