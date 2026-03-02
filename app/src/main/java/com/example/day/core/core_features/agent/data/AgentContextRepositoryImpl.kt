package com.example.day.core.core_features.agent.data

import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.mapper.AgentContextMapper
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import javax.inject.Inject

internal class AgentContextRepositoryImpl @Inject constructor(
    private val agentContextMemoryDao: AgentContextMemoryDao,
    private val agentContextMapper: AgentContextMapper,

) : AgentContextRepository {

    override suspend fun saveContext(
        agentId: Long,
        context: AContext
    ) {
        val entity = agentContextMapper.toEntity(agentId, context)
        agentContextMemoryDao.insertOrUpdate(entity)
    }

    override suspend fun getContext(agentId: Long): AContext? {
        return agentContextMemoryDao.getMemory(agentId)?.let(agentContextMapper::toDomain)
    }

    override suspend fun saveContextParams(
        agentId: Long,
        params: AContextParams
    ) {
        val entity = agentContextMapper.paramsToJson(params)
        agentContextMemoryDao.updateParams(agentId, entity)
    }

    override suspend fun getContextParams(agentId: Long): AContextParams? {
        return agentContextMemoryDao.getParams(agentId)?.let(agentContextMapper::jsonToParams)
    }

    override suspend fun saveContextState(agentId: Long, context: AContextState) {
        val entity = agentContextMapper.contextToJson(context)
        agentContextMemoryDao.updateState(agentId, entity)
    }

    override suspend fun getContextState(agentId: Long): AContextState? {
        return agentContextMemoryDao.getState(agentId)?.let(agentContextMapper::jsonToContext)
    }

    override suspend fun clearAgentContext(agentId: Long) {
        agentContextMemoryDao.delete(agentId)
    }
}