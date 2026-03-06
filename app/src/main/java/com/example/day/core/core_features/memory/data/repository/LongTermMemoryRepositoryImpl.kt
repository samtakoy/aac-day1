package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.data.local.dao.LongTermMemoryDao
import com.example.day.core.core_features.memory.data.local.mapper.LongTermMemoryEntityMapper
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class LongTermMemoryRepositoryImpl @Inject constructor(
    private val memoryDao: LongTermMemoryDao
) : LongTermMemoryRepository {

    override suspend fun upsertFact(ltmGroupId: Long, memoryKey: String, category: String, fact: String) {
        val entity = LongTermMemoryEntity(
            memoryKey = memoryKey,
            ltmGroupId = ltmGroupId,
            category = category,
            fact = fact
        )
        memoryDao.upsert(entity)
    }

    override suspend fun getFactsByGroup(ltmGroupId: Long): List<LongTermMemoryFact> {
        return LongTermMemoryEntityMapper.toDomainList(
            memoryDao.getByGroupOnce(ltmGroupId)
        )
    }

    override fun getFactsByGroupFlow(ltmGroupId: Long): Flow<List<LongTermMemoryFact>> {
        return memoryDao.getByGroup(ltmGroupId).map { entities ->
            LongTermMemoryEntityMapper.toDomainList(entities)
        }
    }

    override suspend fun getFactByKey(ltmGroupId: Long, memoryKey: String, category: String): LongTermMemoryFact? {
        return memoryDao.getByCompositeKey(ltmGroupId, memoryKey, category)?.let {
            LongTermMemoryEntityMapper.toDomain(it)
        }
    }

    override suspend fun deleteFact(id: Long) {
        memoryDao.deleteById(id)
    }

    override suspend fun deleteFact(ltmGroupId: Long, memoryKey: String, category: String) {
        memoryDao.deleteByCompositeKey(ltmGroupId, memoryKey, category)
    }

    override suspend fun deleteFacts(ltmGroupId: Long, memoryKey: String) {
        memoryDao.deleteByMemoryKey(ltmGroupId, memoryKey)
    }

    override suspend fun clearFactsByGroup(ltmGroupId: Long) {
        memoryDao.clearByGroup(ltmGroupId)
    }
}
