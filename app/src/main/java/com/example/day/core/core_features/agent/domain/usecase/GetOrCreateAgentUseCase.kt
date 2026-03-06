package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import javax.inject.Inject

/**
 * Use case for getting or creating an agent.
 * Wraps [AgentRepository.getOrCreateAgent] to follow clean architecture pattern.
 */
class GetOrCreateAgentUseCase @Inject constructor(
    private val repository: AgentRepository
) {
    suspend operator fun invoke(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext
    ): AgentConfig {
        return repository.getOrCreateAgent(
            systemName = systemName,
            chatId = chatId,
            systemPrompt = systemPrompt,
            defaultModel = defaultModel,
            defaultContext = defaultContext
        )
    }
}
