package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import javax.inject.Inject

/**
 * Use case for saving agent context.
 * Wraps [AgentContextRepository.saveContext] to follow clean architecture pattern.
 */
class SaveAgentContextUseCase @Inject constructor(
    private val repository: AgentContextRepository
) {
    /**
     * Save agent context (conversation history).
     *
     * @param agentId the agent ID
     * @param context the context to save
     */
    suspend operator fun invoke(agentId: Long, context: AContext) {
        repository.saveContext(agentId, context)
    }
}
