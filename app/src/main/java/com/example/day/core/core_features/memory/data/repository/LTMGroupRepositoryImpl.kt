package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.data.local.dao.LTMGroupDao
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity
import com.example.day.core.core_features.memory.domain.model.LTMGroup
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import javax.inject.Inject

internal class LTMGroupRepositoryImpl @Inject constructor(
    private val ltmGroupDao: LTMGroupDao
) : LTMGroupRepository {

    override suspend fun createGroup(): Long {
        return ltmGroupDao.insert(LTMGroupEntity())
    }

    override suspend fun getById(id: Long): LTMGroup? {
        return ltmGroupDao.getById(id)?.let { entity ->
            LTMGroup(id = entity.id)
        }
    }

    override suspend fun deleteById(id: Long) {
        ltmGroupDao.deleteById(id)
    }

    override suspend fun findOrCreateByChatGroup(chatGroupId: Long): Long {
        return ltmGroupDao.findOrCreateByChatGroup(chatGroupId)
    }

    override suspend fun getLTMGroupIdByChatGroupId(chatGroupId: Long): Long? {
        return ltmGroupDao.getLTMGroupIdByChatGroupId(chatGroupId)
    }

    override suspend fun deleteLinkByChatGroupId(chatGroupId: Long) {
        ltmGroupDao.deleteLinkByChatGroupId(chatGroupId)
    }

    override suspend fun findOrCreateByAgent(agentId: Long): Long {
        return ltmGroupDao.findOrCreateByAgent(agentId)
    }

    override suspend fun getLTMGroupIdByAgentId(agentId: Long): Long? {
        return ltmGroupDao.getLTMGroupIdByAgentId(agentId)
    }

    override suspend fun deleteLinkByAgentId(agentId: Long) {
        ltmGroupDao.deleteLinkByAgentId(agentId)
    }
}
