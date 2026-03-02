package com.example.day.core.core_features.agent.domain.workers.branch

import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import javax.inject.Inject

/**
 * Validates that an agent has the Branching strategy enabled.
 */
class BranchCommandValidator @Inject constructor(
    private val chatTools: ChatTools
) {
    /**
     * Checks if agent uses Branching strategy. If not, sends error message to chat.
     *
     * @return true if agent uses Branching, false otherwise
     */
    suspend fun ensureBranchingStrategy(agent: AgentConfig, chatId: Long): Boolean {
        if (agent.contextStrategyType != CtxStrategyType.BRANCHING) {
            chatTools.addBotMessage(
                chatId,
                "Стратегия ветвления не активирована. Используйте: @@talk(setup_branches)"
            )
            return false
        }
        return true
    }
}
