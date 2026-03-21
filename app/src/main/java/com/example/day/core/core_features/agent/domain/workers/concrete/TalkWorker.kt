package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommandParser
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandDispatcher
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

/**
 * Agent with context support and context compression.
 * Saves message history between requests to database.
 */
class TalkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val commandDispatcher: CommandDispatcher,
    private val chatTools: ChatTools,
    private val json: Json
) : AWorker {
    companion object {
        const val AGENT_NAME = "talk_agent"
    }

    private val innerCommandParser = InnerCommandParser()

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val commandHandled = processCommand(userPrompt, chat)
        if (commandHandled) return
        processMessage(userPrompt, userRole, chat, onEvent)
    }

    suspend fun handleConfirmation(
        chat: Chat,
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val result = getAgent(chat).resume(
            runId = runId,
            confirmationId = confirmationId,
            approved = approved,
            onEvent = createEventHandler(chat.id, onEvent)
        )
        handleAgentResult(chat.id, result)
    }

    private suspend fun processCommand(task: String, chat: Chat): Boolean {
        val parseResult = innerCommandParser.tryExtractCommand(task)

        return when {
            parseResult.command == null && parseResult.error == null -> false
            parseResult.error != null -> {
                chatTools.addBotMessage(chat.id, parseResult.error)
                true
            }
            parseResult.command != null -> {
                commandDispatcher.dispatch(parseResult.command, chat)
            }
            else -> false
        }
    }

    private suspend fun processMessage(
        userPrompt: String,
        userRole: AContextMessage.Role,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val result = getAgent(chat).process(
            AContextMessage(userRole, userPrompt),
            createEventHandler(chat.id, onEvent)
        )
        handleAgentResult(chat.id, result)
    }

    private suspend fun getAgent(chat: Chat): AIAgent {
        return aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
    }

    private fun createEventHandler(
        chatId: Long,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): suspend (WorkerEvent) -> Unit = { event ->
        when (event) {
            is WorkerEvent.Tool.ToolCallStarted -> {
                chatTools.addInfoMessage(
                    chatId,
                    "${ToolCallingConstants.TOOL_EVENT_START_PREFIX}: ${event.toolName}"
                )
            }
            is WorkerEvent.Tool.ToolCallFinished -> {
                val status = if (event.isError) "error" else "ok"
                val formattedResult = formatToolResult(event.result)
                chatTools.addInfoMessage(
                    chatId,
                    "${ToolCallingConstants.TOOL_EVENT_RESULT_PREFIX} ($status): ${event.toolName}\n$formattedResult"
                )
            }
            else -> Unit
        }
        onEvent?.invoke(event)
    }

    private suspend fun handleAgentResult(chatId: Long, result: Result<AIAgentResult>) {
        result.onSuccess { agentResult ->
            agentResult.requestDebugInfo?.let { chatTools.addInfoMessage(chatId, it) }
            agentResult.reportMessage?.let { chatTools.addInfoMessage(chatId, it) }
            if (agentResult.isPaused) {
                chatTools.addInfoMessage(chatId, ToolCallingConstants.WAITING_CONFIRMATION_MESSAGE)
                return@onSuccess
            }
            if (agentResult.responseText.isNotBlank()) {
                chatTools.addBotMessage(chatId, agentResult.responseText)
            }
        }.onFailure { exception ->
            chatTools.addBotMessage(chatId, exception.stackTraceToString())
        }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}
