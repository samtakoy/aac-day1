package com.example.day.core.core_features.agent.data.repository

import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of AgentMemoryRepository.
 * Acts as a proxy, using LTMGroupRepository and LongTermMemoryRepository internally.
 */
internal class AgentMemoryRepositoryImpl @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val memoryRepository: LongTermMemoryRepository
) : AgentMemoryRepository {

    override suspend fun getFacts(agentId: Long): List<LongTermMemoryFact> {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return emptyList()
        return memoryRepository.getFactsByGroup(ltmGroupId)
    }

    override fun getFactsAsFlow(agentId: Long): Flow<List<LongTermMemoryFact>> {
        return flow {
            val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: 0L
            emit(ltmGroupId)
        }.flatMapConcat { ltmGroupId ->
            memoryRepository.getFactsByGroupFlow(ltmGroupId)
        }
    }

    override suspend fun getFact(agentId: Long, memoryKey: String, category: String): LongTermMemoryFact? {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return null
        return memoryRepository.getFactByKey(ltmGroupId, memoryKey, category = category)
    }

    override suspend fun upsertFact(agentId: Long, memoryKey: String, category: String, fact: String) {
        val ltmGroupId = ltmGroupRepository.findOrCreateByAgent(agentId)
        memoryRepository.upsertFact(ltmGroupId, memoryKey, category, fact)
    }

    override suspend fun deleteFact(id: Long) {
        memoryRepository.deleteFact(id)
    }

    override suspend fun deleteFact(agentId: Long, memoryKey: String, category: String) {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return
        memoryRepository.deleteFact(ltmGroupId, memoryKey, category)
    }

    override suspend fun deleteFacts(agentId: Long, memoryKey: String) {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return
        memoryRepository.deleteFacts(ltmGroupId, memoryKey)
    }

    override suspend fun clearAllFacts(agentId: Long) {
        val ltmGroupId = ltmGroupRepository.getLTMGroupIdByAgentId(agentId) ?: return
        memoryRepository.clearFactsByGroup(ltmGroupId)
    }
}
