package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

/**
 * Clears all task-related memory keys for an agent.
 * Used when task is completed (DONE state) to clean up LTM.
 *
 * Deletes keys matching patterns:
 * - workflow:*
 * - init:*
 * - plan:*
 * - exec:*
 * - verif:*
 */
class ClearTaskMemoryForAgentUseCase @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository
) {

    companion object {
        /**
         * Task-related key prefixes that should be deleted.
         */
        private val TASK_KEY_PREFIXES = listOf(
            "workflow:",
            "init:",
            "plan:",
            "exec:",
            "verif:"
        )
    }

    /**
     * Clear all task-related memory for an agent.
     *
     * @param agentId Agent ID
     * @return Number of keys deleted
     */
    suspend operator fun invoke(agentId: Long): Int {
        // Get LTM group for agent
        val ltmGroup = ltmGroupRepository.getById(agentId)!!
        val ltmGroupId = ltmGroup.id

        // Get all facts for this group
        val allFacts = longTermMemoryRepository.getFactsByGroup(ltmGroupId)

        // Filter task-related keys
        val taskFacts = allFacts.filter { fact ->
            TASK_KEY_PREFIXES.any { prefix ->
                fact.memoryKey.startsWith(prefix, ignoreCase = true)
            }
        }

        // Delete each task-related fact by id
        var deletedCount = 0
        taskFacts.forEach { fact ->
            try {
                longTermMemoryRepository.deleteFact(fact.id)
                deletedCount++
            } catch (e: Exception) {
                // Log error but continue with other keys
                // In production, use proper logging
                println("Failed to delete fact ${fact.memoryKey}: ${e.message}")
            }
        }

        return deletedCount
    }
}