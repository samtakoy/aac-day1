package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentRepository
import javax.inject.Inject

/**
 * Use case for clearing agent context.
 * Wraps [AgentRepository.clearAgentContext] to follow clean architecture pattern.
 */
class ClearAgentContextUseCase @Inject constructor(
    private val repository: AgentRepository
) {
    /**
     * Clear agent context from database.
     *
     * @param agentId the agent ID
     */
    suspend operator fun invoke(agentId: Long) {
        repository.clearAgentContext(agentId)
    }
}
