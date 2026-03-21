package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.memory.domain.provider.AgentSystemPromptMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.AgentToolsMemoryProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

/**
 * Worker for creating and using agents with custom settings from anywhere in the code.
 */
class JustWorkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentRepository: AgentRepository,
    private val json: Json
) {

    suspend fun doWork(
        config: JustWorkConfig,
        userPrompt: String,
        userRole: AContextMessage.Role = AContextMessage.Role.USER,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<String> {
        if (config.recreateAgent) {
            agentRepository.deleteAgent(
                systemName = config.agentName,
                chatId = config.chatId
            )
        }

        return processAgentResult(
            config = config,
            result = getAgent(config).process(
                AContextMessage(userRole, userPrompt),
                createEventHandler(config, onEvent)
            )
        )
    }

    suspend fun resume(
        config: JustWorkConfig,
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<String> {
        return processAgentResult(
            config = config,
            result = getAgent(config).resume(
                runId = runId,
                confirmationId = confirmationId,
                approved = approved,
                onEvent = createEventHandler(config, onEvent)
            )
        )
    }

    private suspend fun getAgent(config: JustWorkConfig): AIAgent {
        return aiAgentFactory.getOrCreate(
            systemName = config.agentName,
            chatId = config.chatId,
            systemPrompt = "",
            defaultModel = config.defaultModel,
            defaultContext = { AContextDefaultFactory.createFull() },
            onCreateCallback = { agentId ->
                if (config.systemPrompt.isNotBlank()) {
                    agentMemoryRepository.upsertFact(
                        agentId = agentId,
                        memoryKey = AgentSystemPromptMemoryProvider.MEMORY_KEY,
                        category = AgentSystemPromptMemoryProvider.CATEGORY,
                        fact = config.systemPrompt
                    )
                }

                if (config.allowedTools.isNotEmpty()) {
                    val toolsJson = json.encodeToString(config.allowedTools)
                    agentMemoryRepository.upsertFact(
                        agentId = agentId,
                        memoryKey = AgentToolsMemoryProvider.MEMORY_KEY,
                        category = AgentToolsMemoryProvider.CATEGORY,
                        fact = toolsJson
                    )
                }
            }
        )
    }

    private fun createEventHandler(
        config: JustWorkConfig,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): suspend (WorkerEvent) -> Unit = { event ->
        when (event) {
            is WorkerEvent.Tool.ToolCallStarted -> {
                chatTools.addInfoMessage(
                    config.chatId,
                    "${ToolCallingConstants.TOOL_EVENT_START_PREFIX}: ${event.toolName}"
                )
            }
            is WorkerEvent.Tool.ToolCallFinished -> {
                val status = if (event.isError) "error" else "ok"
                val formattedResult = formatToolResult(event.result)
                chatTools.addInfoMessage(
                    config.chatId,
                    "${ToolCallingConstants.TOOL_EVENT_RESULT_PREFIX} ($status): ${event.toolName}\n$formattedResult"
                )
            }
            else -> Unit
        }
        onEvent?.invoke(event)
    }

    private suspend fun processAgentResult(
        config: JustWorkConfig,
        result: Result<AIAgentResult>
    ): Result<String> {
        return result.mapCatching { agentResult ->
            agentResult.requestDebugInfo?.let { chatTools.addInfoMessage(config.chatId, it) }
            agentResult.reportMessage?.let { chatTools.addInfoMessage(config.chatId, it) }
            if (agentResult.isPaused) {
                chatTools.addInfoMessage(config.chatId, ToolCallingConstants.WAITING_CONFIRMATION_MESSAGE)
                error("${ToolCallingConstants.CONFIRMATION_REQUIRED_PREFIX}: ${agentResult.runId}")
            }
            agentResult.responseText
        }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}

