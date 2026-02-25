package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import javax.inject.Inject

/**
 * Use case for getting agent context.
 * Wraps [AgentRepository.getAgentContext] to follow clean architecture pattern.
 */
class GetAgentContextUseCase @Inject constructor(
    private val repository: AgentRepository
) {
    /**
     * Get agent context (conversation history).
     *
     * @param agentId the agent ID
     * @return AContext if found, null otherwise
     */
    suspend operator fun invoke(agentId: Long): AContext? {
        return repository.getAgentContext(agentId)
    }
}
