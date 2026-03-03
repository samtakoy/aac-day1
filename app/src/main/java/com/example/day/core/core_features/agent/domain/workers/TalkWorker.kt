package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommandParser
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandDispatcher
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
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
 * - @@talk <text> - send text to LLM
 */
class TalkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val agentRepository: AgentRepository,
    private val commandDispatcher: CommandDispatcher,
    private val chatTools: ChatTools
) : AWorker {

    private val innerCommandParser = InnerCommandParser()

    companion object {
        const val AGENT_NAME = "talk_agent"
    }

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val commandHandled = processCommand(task, chat)
        if (commandHandled) return
        processMessage(task, chat, onEvent)
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
        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, false, chat)
        agent.process(chat.settings, task, onEvent)
            .onSuccess { result ->
                result.reportMessage?.let { chatTools.addInfoMessage(chat.id, it) }
                chatTools.addBotMessage(chat.id, result.responseText)
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }
}
