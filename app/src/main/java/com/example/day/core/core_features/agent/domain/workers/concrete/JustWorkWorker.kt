package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
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
 * Unlike TalkWorker, it is not bound to chat commands and accepts agent configuration directly.
 *
 * Usage:
 * ```
 * val config = JustWorkConfig(
 *     agentName = "git_file_investigator",
 *     chatId = chatId,
 *     systemPrompt = "...",
 *     allowedTools = listOf("get_git_file_list", "get_file_analysis"),
 *     defaultModel = { ... },
 *     defaultContext = { AContextDefaultFactory.createFull() }
 * )
 * val result = justWorkWorker.doWork(config, userPrompt)
 * ```
 */
class JustWorkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentRepository: AgentRepository,
    private val json: Json
) {

    /**
     * Process a message using an agent with custom configuration.
     * Returns the agent's final response text.
     * Progress notifications (tool events, debug info) are still sent to chat as info messages.
     *
     * @param config Agent configuration
     * @param userPrompt User message to process
     * @param userRole Role of the user message (default: USER)
     * @param onEvent Callback for worker events (optional)
     * @return Result with the agent's final response text
     */
    suspend fun doWork(
        config: JustWorkConfig,
        userPrompt: String,
        userRole: AContextMessage.Role = AContextMessage.Role.USER,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): Result<String> {
        if (config.recreateAgent) {
            // TODO пока так, но нужно переделать на очистку историй сообщений агента
            //  Пока он используется с чистой историей на старте
            agentRepository.deleteAgent(
                systemName = config.agentName,
                chatId = config.chatId
            )
        }

        val agent = aiAgentFactory.getOrCreate(
            systemName = config.agentName,
            chatId = config.chatId,
            systemPrompt = "",  // ПУСТО! systemPrompt настраивается через onCreateCallback
            defaultModel = config.defaultModel,
                defaultContext = { AContextDefaultFactory.createFull() },
            onCreateCallback = { agentId ->
                // Настройка systemPrompt и allowedTools при первом создании агента
                // Сохраняем системный промпт
                if (config.systemPrompt.isNotBlank()) {
                    agentMemoryRepository.upsertFact(
                        agentId = agentId,
                        memoryKey = AgentSystemPromptMemoryProvider.MEMORY_KEY,
                        category = AgentSystemPromptMemoryProvider.CATEGORY,
                        fact = config.systemPrompt
                    )
                }
                
                // Сохраняем список разрешенных tools (если не пустой)
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

        val eventHandler: suspend (WorkerEvent) -> Unit = { event ->
            when (event) {
                is WorkerEvent.ToolCallStarted -> {
                    chatTools.addInfoMessage(
                        config.chatId,
                        "${ToolCallingConstants.TOOL_EVENT_START_PREFIX}: ${event.toolName}"
                    )
                }
                is WorkerEvent.ToolCallFinished -> {
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

        return agent.process(AContextMessage(userRole, userPrompt), eventHandler)
            .mapCatching { result ->
                result.requestDebugInfo?.let { chatTools.addInfoMessage(config.chatId, it) }
                result.reportMessage?.let { chatTools.addInfoMessage(config.chatId, it) }
                result.responseText
            }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}
