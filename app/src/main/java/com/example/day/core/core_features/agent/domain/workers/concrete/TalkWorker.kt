package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import android.util.Log
import com.example.day.core.core_features.agent.domain.prompt.McpSystemPrompt
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.tools.McpToolCallParser
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommandParser
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandDispatcher
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.mcp.domain.McpFormatting
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
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
 * - @@talk <text> - send text to LLM
 */
class TalkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val commandDispatcher: CommandDispatcher,
    private val chatTools: ChatTools,
    private val mcpRepository: McpRepository,
    private val mcpTools: McpTools,
    private val json: Json
) : AWorker {
    private companion object {
        const val TAG = "TalkWorker"
        const val AGENT_NAME = "talk_agent"
    }

    // TODO почему это тут оказалось?
    private val innerCommandParser = InnerCommandParser()

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val commandHandled = processCommand(userPrompt, chat)
        if (commandHandled) return
        processMessage(userPrompt, chat, onEvent)
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
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            McpSystemPrompt.appendTo(chat.settings.systemPromt),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
        agent.process(chat.settings, task, onEvent)
            .onSuccess { result ->
                result.requestDebugInfo?.let { chatTools.addInfoMessage(chat.id, it) }
                result.reportMessage?.let { chatTools.addInfoMessage(chat.id, it) }

                // TODO рефакторинг - тут специфичная логика про обработку tools
                val parsed = McpToolCallParser.tryParse(result.responseText, json)
                if (parsed == null || !McpToolNames.ALLOWED_TOOL_NAMES.contains(parsed.tool)) {
                    chatTools.addBotMessage(chat.id, result.responseText)
                    return@onSuccess
                }

                val serverId = resolveServerId()
                if (serverId == null) {
                    chatTools.addBotMessage(chat.id, "MCP сервер не настроен")
                    return@onSuccess
                }

                Log.d(TAG, "Auto MCP tool call: ${parsed.tool}")
                if (parsed.cleanedText.isNotBlank()) {
                    chatTools.addBotMessage(chat.id, parsed.cleanedText)
                }

                chatTools.addInfoMessage(chat.id, "🔧 MCP: ${parsed.tool}")
                // Вызов MCP тула
                val toolResult = mcpTools.callTool(serverId, parsed.tool, parsed.arguments)
                toolResult.onSuccess { text ->
                    val formatted = McpFormatting.formatResult(text, json)
                    chatTools.addInfoMessage(chat.id, "MCP Result:\n$formatted")
                }.onFailure { error ->
                    Log.e(TAG, "MCP tool failed: ${error.message}", error)
                    chatTools.addBotMessage(chat.id, "Ошибка MCP: ${error.message}")
                }
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }

    private suspend fun resolveServerId(): String? {
        val servers = mcpRepository.getServers().first()
        val enabled = servers.firstOrNull { it.isEnabled }
        return enabled?.id ?: servers.firstOrNull()?.id
    }
}
