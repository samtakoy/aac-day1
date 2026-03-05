package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "context" command - displays full agent context.
 */
class ContextCommandHandler @Inject constructor(
    private val aiAgentFactory: AIAgentFactory
) : CommandHandler {

    override val commandName = "context"

    override suspend fun handle(
        params: List<Pair<String, String?>>,
        chat: Chat
    ): CommandResult {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model }
        )
        return CommandResult.Success(agent.getFullContext())
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
