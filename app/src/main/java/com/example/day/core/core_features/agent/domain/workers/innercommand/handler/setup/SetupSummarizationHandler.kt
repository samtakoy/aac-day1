package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandResult
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "setup" command for Summarization strategy.
 */
class SetupSummarizationHandler @Inject constructor(
    private val setupUseCase: SetupStrategyUseCase,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "setup"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()
        val extraParam = paramsMap[ContextStrategyConstants.PARAM_EXTRA_LIMIT]?.toIntOrNull()

        if (msgParam == null || extraParam == null) {
            return CommandResult.Error(
                "Неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT} и --${ContextStrategyConstants.PARAM_EXTRA_LIMIT}"
            )
        }

        val agent = getOrCreateAgent(chat)
        val params = AContextParams.Summarization(msgParam, extraParam)

        return when (val result = setupUseCase.execute(agent, CtxStrategyType.SUMMARIZATION, params)) {
            is SetupResult.Updated -> CommandResult.Success("Настройки обновлены: msg=$msgParam, extra=$extraParam")
            is SetupResult.Migrated -> CommandResult.Success(
                "Стратегия переключена на Summarization. msg=$msgParam, extra=$extraParam. " +
                "Мигрировано сообщений: ${result.messagesMigrated}"
            )
        }
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
