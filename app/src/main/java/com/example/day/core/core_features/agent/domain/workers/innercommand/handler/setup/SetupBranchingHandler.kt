package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextBranchingStrategy
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandResult
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "setup_branches" command for Branching strategy.
 */
class SetupBranchingHandler @Inject constructor(
    private val setupUseCase: SetupStrategyUseCase,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "setup_branches"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val mainBranchParam = paramsMap[ContextStrategyConstants.PARAM_DEFAULT_BRANCH]
        val defaultBranch = mainBranchParam ?: ContextBranchingStrategy.DEFAULT_BRANCH_ID

        val agent = getOrCreateAgent(chat)
        val strategyParams = AContextParams.Branching(defaultBranchId = defaultBranch)

        return when (val result = setupUseCase.execute(agent, CtxStrategyType.BRANCHING, strategyParams)) {
            is SetupResult.Updated -> CommandResult.Success("Настройки Branching обновлены: основная ветка=$defaultBranch")
            is SetupResult.Migrated -> CommandResult.Success(
                "Стратегия переключена на Branching (ветвление). " +
                "Основная ветка: $defaultBranch. " +
                "Мигрировано сообщений: ${result.totalMessages}"
            )
        }
    }

    private suspend fun getOrCreateAgent(chat: Chat): AgentConfig =
        agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPromt = chat.settings.systemPromt,
            model = chat.settings.model
        )

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
