package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import javax.inject.Inject

/**
 * Use case for getting or creating an agent.
 * Wraps [AgentRepository.getOrCreateAgent] to follow clean architecture pattern.
 */
class GetOrCreateAgentUseCase @Inject constructor(
    private val repository: AgentRepository
) {
    /**
     * Get or create agent by systemName and isCommon flag.
     * 
     * If isCommon = true:
     *   1. Find agent by systemName only (common agents)
     *   2. If not found - create new common agent
     * 
     * If isCommon = false:
     *   1. Find agent by systemName + chatId (chat-specific)
     *   2. If not found - create new agent and bind to chatId
     *
     * @param systemName system name of the agent
     * @param isCommon if true, agent can be used in any chat without binding
     * @param chatId chat id to bind agent to (if isCommon = false)
     * @param chatSettings settings from the chat (modelSettings, systemPrompt)
     * @return existing or newly created Agent
     */
    suspend operator fun invoke(
        systemName: String,
        isCommon: Boolean,
        chatSettings: ChatSettings
    ): AgentConfig {
        return repository.getOrCreateAgent(
            systemName = systemName,
            isCommon = isCommon,
            chatSettings = chatSettings
        )
    }
}
