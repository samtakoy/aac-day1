package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandResult
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Handles the "setup_sliding" command for SlidingWindow strategy.
 */
class SetupSlidingWindowHandler @Inject constructor(
    private val setupUseCase: SetupStrategyUseCase,
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "setup_sliding"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()

        if (msgParam == null) {
            return CommandResult.Error(
                "Неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT}"
            )
        }

        val agent = getOrCreateAgent(chat)
        val strategyParams = AContextParams.SlidingWindow(msgParam)

        return when (val result = setupUseCase.execute(agent, CtxStrategyType.SLIDING_WINDOW, strategyParams)) {
            is SetupResult.Updated -> CommandResult.Success("Размер окна обновлён: $msgParam сообщений")
            is SetupResult.Migrated -> CommandResult.Success(
                "Стратегия переключена на SlidingWindow. Окно: $msgParam сообщений. " +
                "Мигрировано сообщений: ${result.messagesMigrated} из ${result.totalMessages}"
            )
        }
    }

    private suspend fun getOrCreateAgent(chat: Chat): AgentConfig =
        agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
