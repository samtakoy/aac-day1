package com.example.day.core.core_features.chat.data.repository

import com.example.day.core.core_features.chat.data.local.dao.LongTermMemoryDao
import com.example.day.core.core_features.chat.data.local.model.LongTermMemoryEntity
import com.example.day.core.core_features.chat.domain.model.LongTermMemory
import com.example.day.core.core_features.chat.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class LongTermMemoryRepositoryImpl @Inject constructor(
    private val memoryDao: LongTermMemoryDao
) : LongTermMemoryRepository {

    override suspend fun upsertFact(groupId: Long, memoryKey: String, category: String, fact: String) {
        val entity = LongTermMemoryEntity(
            memoryKey = memoryKey,
            groupId = groupId,
            category = category,
            fact = fact
        )
        memoryDao.upsert(entity)
    }

    override suspend fun getFactsByGroup(groupId: Long): List<LongTermMemory> {
        return memoryDao.getByGroupOnce(groupId).map { it.toDomain() }
    }

    override fun getFactsByGroupFlow(groupId: Long): Flow<List<LongTermMemory>> {
        return memoryDao.getByGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFactByKey(groupId: Long, memoryKey: String): LongTermMemory? {
        return memoryDao.getByKeyAndGroup(memoryKey, groupId)?.toDomain()
    }

    override suspend fun deleteFact(groupId: Long, memoryKey: String) {
        memoryDao.deleteByKeyAndGroup(memoryKey, groupId)
    }

    override suspend fun clearFactsByGroup(groupId: Long) {
        memoryDao.clearByGroup(groupId)
    }

    private fun LongTermMemoryEntity.toDomain(): LongTermMemory {
        return LongTermMemory(
            memoryKey = memoryKey,
            groupId = groupId,
            category = category,
            fact = fact,
            updatedAt = updatedAt
        )
    }
}
