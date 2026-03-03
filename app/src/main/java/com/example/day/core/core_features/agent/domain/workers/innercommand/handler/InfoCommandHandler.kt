package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "info" command - displays agent context information.
 */
class InfoCommandHandler @Inject constructor(
    private val aiAgentFactory: AIAgentFactory
) : CommandHandler {

    override val commandName = "info"

    override suspend fun handle(
        params: List<Pair<String, String?>>,
        chat: Chat
    ): CommandResult {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chatId = chat.id,
            systemPromt = chat.settings.systemPromt,
            model = chat.settings.model
        )
        return CommandResult.Success(agent.getInfo())
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
