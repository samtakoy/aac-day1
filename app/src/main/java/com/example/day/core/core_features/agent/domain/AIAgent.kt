package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.ProcessResult
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.agent.domain.tools.OrchestratorRequest
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManager
import com.example.day.core.core_features.agent.domain.tools.hitl.ToolCallDecision
import com.example.day.core.core_features.agent.domain.tools.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import java.util.UUID

class AIAgent(
    val config: AgentConfig,
    private val contextRepository: AgentContextRepository,
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,
    private val toolProvider: ToolProvider,
    private val orchestrator: ToolCallOrchestrator,
    private val toolExecutor: ToolExecutor,
    private val hitlSessionManager: HitlSessionManager
) {
    companion object {
        private const val MAX_TOOL_LOOPS = 10
    }

    private data class LlmContext(
        val llmMessages: MutableList<ModelRequest.Message>,
        val newMessages: MutableList<ModelRequest.Message>,
        val snapshot: ContextStrategyResult
    )

    private suspend fun buildLlmContext(prompt: AContextMessage): LlmContext {
        val memoryMessages = memoryProvider.getMemoryContext()
        val promptMessages = memoryProvider.appendUserPrompt(prompt)
        val snapshot = strategy.process(config, contextRepository)

        val llmMessages = buildList {
            addAll(memoryMessages.map { it.toModelRequestMessage() })
            addAll(snapshot.messages.toModelRequestMessages())
            addAll(promptMessages.context.filter { it.content.isNotBlank() }.map { it.toModelRequestMessage() })
            if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
        }.toMutableList()

        val newMessages = buildList {
            addAll(snapshot.messages.toModelRequestMessages())
            if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
        }.toMutableList()

        return LlmContext(llmMessages, newMessages, snapshot)
    }

    suspend fun process(
        prompt: AContextMessage,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ProcessResult> {
        if (hitlSessionManager.hasActiveSession(config.id)) {
            return Result.failure(HitlSessionBusyError())
        }
        val runId = UUID.randomUUID().toString()
        val (llmMessages, newMessages, snapshot) = buildLlmContext(prompt)
        return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)
    }

    internal suspend fun runToolLoop(
        runId: String,
        llmMessages: MutableList<ModelRequest.Message>,
        newMessages: MutableList<ModelRequest.Message>,
        snapshot: ContextStrategyResult,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ProcessResult> {
        var loopCount = 0

        while (loopCount < MAX_TOOL_LOOPS) {
            val request = OrchestratorRequest(
                messages = llmMessages.toList(),
                systemPrompt = config.systemPrompt,
                modelSettings = config.modelSettings,
                tools = toolProvider.getTools(config.id)
            )
            val result = orchestrator.execute(request, onEvent)
                .getOrElse { return Result.failure(it) }

            when (result) {
                is OrchestratorResult.Completed -> {
                    newMessages.add(result.assistantMessage)
                    val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
                    val strategyResult = strategy.afterResponse(
                        agent = config,
                        response = result.responseText,
                        store = contextRepository,
                        fullContext = extendedSnapshot
                    )
                    return Result.success(ProcessResult.Success(AIAgentResult(result.responseText, strategyResult.reportMessage, debugInfo = "")))
                }
                is OrchestratorResult.PendingApproval -> {
                    llmMessages.add(result.assistantMessage)
                    newMessages.add(result.assistantMessage)

                    when (val exec = toolExecutor.submit(
                        runId = runId,
                        toolCalls = result.toolCalls,
                        prompt = AContextMessage(AContextMessage.Role.USER, ""),
                        loopMessages = newMessages.toList(),
                        context = ToolCallContext(agentId = config.id),
                        onEvent = onEvent
                    )) {
                        is ToolExecutionResult.Completed -> {
                            val toolMessages = exec.results.toModelRequestMessages()
                            llmMessages.addAll(toolMessages)
                            newMessages.addAll(toolMessages)
                        }
                        is ToolExecutionResult.AwaitingApproval -> {
                            return Result.success(ProcessResult.Pending(exec.runId))
                        }
                    }
                }
            }
            loopCount++
        }

        val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
        strategy.afterResponse(config, "", contextRepository, extendedSnapshot)
        return Result.success(ProcessResult.Success(AIAgentResult("", null, debugInfo = "")))
    }

    suspend fun resumeWithDecisions(
        runId: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ProcessResult> {
        val session = hitlSessionManager.getSession(runId)
            ?: return Result.failure(IllegalStateException("Session $runId not found"))

        val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)

        // Append all accumulated loop messages (includes prompt msg + previous iterations + pending assistantMsg)
        llmMessages.addAll(session.loopMessages)
        val newMessages = session.loopMessages.toMutableList()

        // Execute approved / reject others
        val toolResults = session.pendingToolCalls.map { call ->
            val decision = session.decisions[call.id]
            if (decision == ToolCallDecision.APPROVED) {
                val result = toolProvider.executeToolCall(call, ToolCallContext(agentId = session.agentId))
                val content = result.getOrElse { "Error: ${it.message}" }
                onEvent?.invoke(WorkerEvent.ApprovalDecided(runId, call.id, ToolCallDecision.APPROVED))
                ToolResult(toolCallId = call.id, content = content, isError = result.isFailure)
            } else {
                onEvent?.invoke(WorkerEvent.ApprovalDecided(runId, call.id, ToolCallDecision.REJECTED))
                ToolResult(toolCallId = call.id, content = "Rejected by user", isError = true)
            }
        }

        val toolMessages = toolResults.toModelRequestMessages()
        llmMessages.addAll(toolMessages)
        newMessages.addAll(toolMessages)

        hitlSessionManager.closeSession(runId)
        return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)
    }

    suspend fun getInfo(): String = strategy.getInfoReport(config, contextRepository)
    suspend fun getFullContext(): String = strategy.getFullContextReport(config, contextRepository)
    suspend fun setupParams(params: Map<String, String>): String =
        strategy.updateParams(config, params, contextRepository)

    private fun List<ModelRequest.Message>.toAContextMessages() = map { msg ->
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
                AContextMessage.ToolCallRef(call.id, call.type, call.function.name, call.function.arguments)
            }
        )
    }
}

class HitlSessionBusyError : Exception("HITL session already active for this agent")
