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
 * Handles the "setup_sticky" command for StickyFacts strategy.
 */
class SetupStickyFactsHandler @Inject constructor(
    private val setupUseCase: SetupStrategyUseCase,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "setup_sticky"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()
        val factsParam = paramsMap[ContextStrategyConstants.PARAM_MAX_FACTS]?.toIntOrNull()

        if (msgParam == null) {
            return CommandResult.Error(
                "Неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT}"
            )
        }

        val agent = getOrCreateAgent(chat)
        val maxFacts = factsParam ?: 20 // Default value
        val strategyParams = AContextParams.StickyFacts(msgParam, maxFacts)

        return when (val result = setupUseCase.execute(agent, CtxStrategyType.STICKY_FACTS, strategyParams)) {
            is SetupResult.Updated -> CommandResult.Success(
                "Настройки StickyFacts обновлены: окно=$msgParam, макс.фактов=$maxFacts"
            )
            is SetupResult.Migrated -> CommandResult.Success(
                "Стратегия переключена на StickyFacts. " +
                "Окно: $msgParam сообщений, макс.фактов: $maxFacts. " +
                "Мигрировано сообщений: ${result.messagesMigrated} из ${result.totalMessages}"
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
