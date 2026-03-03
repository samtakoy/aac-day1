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

    override suspend fun upsertFact(ltmGroupId: Long, memoryKey: String, categoryId: Long, fact: String) {
        val entity = LongTermMemoryEntity(
            memoryKey = memoryKey,
            ltmGroupId = ltmGroupId,
            categoryId = categoryId,
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

    override suspend fun getFactByKey(ltmGroupId: Long, memoryKey: String): LongTermMemoryFact? {
        return memoryDao.getByKeyAndGroup(memoryKey, ltmGroupId)?.let {
            LongTermMemoryEntityMapper.toDomain(it)
        }
    }

    override suspend fun deleteFact(ltmGroupId: Long, memoryKey: String) {
        memoryDao.deleteByKeyAndGroup(memoryKey, ltmGroupId)
    }

    override suspend fun deleteFact(ltmGroupId: Long, memoryKey: String, categoryId: Long) {
        memoryDao.deleteByKeyAndCategoryAndGroup(memoryKey, categoryId, ltmGroupId)
    }

    override suspend fun clearFactsByGroup(ltmGroupId: Long) {
        memoryDao.clearByGroup(ltmGroupId)
    }
}
