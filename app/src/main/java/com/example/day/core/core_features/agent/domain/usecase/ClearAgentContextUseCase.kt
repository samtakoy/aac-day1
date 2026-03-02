package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import javax.inject.Inject

/**
 * Use case for clearing agent context.
 * Wraps [AgentContextRepository.clearAgentContext] to follow clean architecture pattern.
 */
class ClearAgentContextUseCase @Inject constructor(
    private val repository: AgentContextRepository
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
