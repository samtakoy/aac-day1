package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionManager
import com.example.day.core.core_features.agent.domain.tools.LlmExecutionRequest
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolRegistry
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider

/**
 * Main AI Agent orchestrator.
 * Coordinates strategy, LLM, and context repository to process user messages.
 */
class AIAgent(
    val config: AgentConfig,
    private val contextRepository: AgentContextRepository,
    private val llmProvider: LlmRequestUseCase, // TODO remove
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,
    private val toolRegistry: ToolRegistry,
    private val executionSessionManager: ExecutionSessionManager,
    private val runIdProvider: () -> String,
    private val orchestrator: ToolCallOrchestrator
) {

    suspend fun process(
        prompt: AContextMessage,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        val memoryMessages = memoryProvider.getMemoryContext()
        val enrichedPrompt = memoryProvider.appendUserPrompt(prompt)
        val snapshot = strategy.process(config, contextRepository)

        val tools = toolRegistry.getTools(agentId = config.id)
        val toolToServerMap = toolRegistry.getToolToServerMap()
        val request = LlmExecutionRequest(
            initialHistory = snapshot.messages.toModelRequestMessages(),
            memoryMessages = memoryMessages,
            prompt = enrichedPrompt,
            systemPrompt = config.systemPrompt,
            modelSettings = config.modelSettings,
            tools = tools,
            context = ToolCallContext(
                agentId = config.id,
                toolToServer = toolToServerMap
            )
        )
        val runId = runIdProvider()
        executionSessionManager.ensure(runId, request)

        val result = orchestrator.execute(
            request = request,
            runId = runId,
            onEvent = onEvent
        )

        return result.map { toolCallingResult ->
            val requestDebugInfo = buildRequestDebugInfo(config.systemPrompt, memoryMessages, enrichedPrompt)
            if (toolCallingResult.isPaused) {
                AIAgentResult(
                    responseText = toolCallingResult.finalResponseText,
                    reportMessage = null,
                    requestDebugInfo = requestDebugInfo,
                    isPaused = true,
                    pendingConfirmationId = toolCallingResult.pendingConfirmationId,
                    runId = toolCallingResult.runId
                )
            } else {
                val extendedSnapshot = snapshot.copy(messages = toolCallingResult.allMessages)
                val strategyResult = strategy.afterResponse(
                    agent = config,
                    response = toolCallingResult.finalResponseText,
                    store = contextRepository,
                    fullContext = extendedSnapshot
                )
                AIAgentResult(
                    responseText = toolCallingResult.finalResponseText,
                    reportMessage = strategyResult.reportMessage,
                    requestDebugInfo = requestDebugInfo,
                    runId = toolCallingResult.runId
                )
            }
        }
    }

    suspend fun resume(
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        val result = orchestrator.resume(
            runId = runId,
            confirmationId = confirmationId,
            approved = approved,
            onEvent = onEvent
        )

        return result.map { toolCallingResult ->
            if (toolCallingResult.isPaused) {
                AIAgentResult(
                    responseText = toolCallingResult.finalResponseText,
                    reportMessage = null,
                    isPaused = true,
                    pendingConfirmationId = toolCallingResult.pendingConfirmationId,
                    runId = toolCallingResult.runId
                )
            } else {
                val snapshot = strategy.process(config, contextRepository)
                val extendedSnapshot = snapshot.copy(messages = toolCallingResult.allMessages)
                val strategyResult = strategy.afterResponse(
                    agent = config,
                    response = toolCallingResult.finalResponseText,
                    store = contextRepository,
                    fullContext = extendedSnapshot
                )
                AIAgentResult(
                    responseText = toolCallingResult.finalResponseText,
                    reportMessage = strategyResult.reportMessage,
                    runId = toolCallingResult.runId
                )
            }
        }
    }

    suspend fun getInfo(): String = strategy.getInfoReport(config, contextRepository)

    suspend fun getFullContext(): String = strategy.getFullContextReport(config, contextRepository)

    suspend fun setupParams(params: Map<String, String>): String =
        strategy.updateParams(config, params, contextRepository)

    private fun buildRequestDebugInfo(
        systemPrompt: String?,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage
    ): String = buildString {
        appendLine("=== LLM request (without history) ===")
        if (!systemPrompt.isNullOrBlank()) {
            appendLine("[SYSTEM]")
            appendLine(systemPrompt.trimEnd())
            appendLine()
        }
        memoryMessages.forEach { msg ->
            appendLine("[MEMORY:${msg.role.name}]")
            appendLine(msg.content?.trimEnd())
            appendLine()
        }
        appendLine("[${prompt.role.name}]")
        append(prompt.content?.trimEnd())
    }
}
