package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import javax.inject.Inject

/**
 * Retrieves all LTM facts for an agent via its ltmGroupId.
 */
class GetFactsByAgentUseCase @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val memoryRepository: LongTermMemoryRepository
) {
    suspend operator fun invoke(agentId: Long): List<LongTermMemoryFact> {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return emptyList()
        return memoryRepository.getFactsByGroup(ltmGroupId)
    }
}
