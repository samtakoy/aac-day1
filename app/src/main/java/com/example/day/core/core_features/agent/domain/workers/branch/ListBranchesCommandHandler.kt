package com.example.day.core.core_features.agent.domain.workers.branch

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandResult
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "list_branches" command.
 */
class ListBranchesCommandHandler @Inject constructor(
    private val validator: BranchCommandValidator,
    private val branchManager: BranchManager,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "list_branches"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val agent = getOrCreateAgent(chat)

        if (!validator.ensureBranchingStrategy(agent, chat.id)) {
            return CommandResult.Success()
        }

        val branchList = branchManager.listBranches(agent.id)
        return CommandResult.Success(branchList)
    }

    private suspend fun getOrCreateAgent(chat: Chat): AgentConfig =
        agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            isCommon = false,
            chatId = chat.id,
            chatSettings = chat.settings
        )

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
