package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.model.AContextMessage
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
 * Agent with context support and context compression (Context Management).
 * Saves message history between requests to database.
 *
 * Supports commands:
 * - @@talk(info) - display context settings
 * - @@talk(context) - display full context content
 * - @@talk(setup --msg X --extra Y) - configure Summarization strategy
 * - @@talk(setup_sliding --msg X) - configure SlidingWindow strategy
 * - @@talk(setup_sticky --msg X --facts Y) - configure StickyFacts strategy
 * - @@talk(setup_branches --main X) - configure Branching strategy
 * - @@talk(new_branch --id X) - create new branch
 * - @@talk(switch_branch --id X) - switch to branch
 * - @@talk(list_branches) - list all branches
 * - @@talk(delete_branch --id X) - delete branch
 * - @@talk(agent --addrule "текст") - добавить новое правило диалога
 * - @@talk(agent --listrules) - вывести список всех правил диалога
 * - @@talk(agent --clearrules) - удалить все правила диалога
 * - @@talk(rag --on) - включить RAG-обогащение промптов через rag-server
 * - @@talk(rag --off) - выключить RAG
 * - @@talk(rag --state) - показать статус RAG и URL сервера
 * - @@talk(rag --url <url>) - задать URL rag-server (default: http://10.0.2.2:3001)
 * - @@talk <text> - send text to LLM
 */
class TalkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val commandDispatcher: CommandDispatcher,
    private val chatTools: ChatTools,
    private val json: Json
) : AWorker {
    private companion object {
        const val AGENT_NAME = "talk_agent"
    }

    // TODO почему это тут оказалось?
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

    /**
     * Process a command if present in input.
     * @return true if command was handled
     */
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

    /**
     * Process a regular message (non-command) through the AI agent.
     */
    private suspend fun processMessage(
        userPrompt: String,
        userRole: AContextMessage.Role,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
        val eventHandler: suspend (WorkerEvent) -> Unit = { event ->
            when (event) {
                is WorkerEvent.ToolCallStarted -> {
                    chatTools.addInfoMessage(
                        chat.id,
                        "${ToolCallingConstants.TOOL_EVENT_START_PREFIX}: ${event.toolName}"
                    )
                }
                is WorkerEvent.ToolCallFinished -> {
                    val status = if (event.isError) "error" else "ok"
                    val formattedResult = formatToolResult(event.result)
                    chatTools.addInfoMessage(
                        chat.id,
                        "${ToolCallingConstants.TOOL_EVENT_RESULT_PREFIX} ($status): ${event.toolName}\n$formattedResult"
                    )
                }
                else -> Unit
            }
            onEvent?.invoke(event)
        }

        agent.process(AContextMessage(userRole, userPrompt), eventHandler)
            .onSuccess { result ->
                result.requestDebugInfo?.let { chatTools.addInfoMessage(chat.id, it) }
                result.reportMessage?.let { chatTools.addInfoMessage(chat.id, it) }
                if (result.responseText.isNotBlank()) {
                    chatTools.addBotMessage(chat.id, result.responseText)
                }
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}
