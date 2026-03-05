package com.example.day.core.core_features.agent.domain.workers.branch

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandResult
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "delete_branch" command.
 */
class DeleteBranchCommandHandler @Inject constructor(
    private val validator: BranchCommandValidator,
    private val branchManager: BranchManager,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "delete_branch"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val branchId = params.toMap()[ContextStrategyConstants.PARAM_BRANCH_ID]
            ?: return CommandResult.Error(
                "Требуется указать ID ветки: @@talk(delete_branch --id myBranch)"
            )

        val agent = getOrCreateAgent(chat)

        if (!validator.ensureBranchingStrategy(agent, chat.id)) {
            return CommandResult.Success()
        }

        return when (val result = branchManager.deleteBranch(agent.id, branchId)) {
            is BranchResult.Success -> CommandResult.Success(result.message)
            is BranchResult.Error -> CommandResult.Error(result.message)
        }
    }

    private suspend fun getOrCreateAgent(chat: Chat): AgentConfig =
        agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = chat.settings.systemPromt,
            defaultModel = { chat.settings.model }
        )

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
