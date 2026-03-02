package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import javax.inject.Inject

/**
 * Use case for getting agent context.
 * Wraps [AgentContextRepository.getContext] to follow clean architecture pattern.
 */
class GetAgentContextUseCase @Inject constructor(
    private val repository: AgentContextRepository
) {
    /**
     * Get agent context (conversation history).
     *
     * @param agentId the agent ID
     * @return AContext if found, null otherwise
     */
    suspend operator fun invoke(agentId: Long): AContext? {
        return repository.getContext(agentId)
    }
}
