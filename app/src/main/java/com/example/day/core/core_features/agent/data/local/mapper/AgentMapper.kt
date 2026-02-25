package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.domain.model.Agent
import javax.inject.Inject

/**
 * Mapper for converting between Agent domain model and AgentEntity.
 */
internal class AgentMapper @Inject constructor() {
    
    companion object {
        const val IS_COMMON_TRUE = 1
        const val IS_COMMON_FALSE = 0
    }
    
    fun toDomain(entity: AgentEntity): Agent {
        return Agent(
            id = entity.id,
            systemName = entity.systemName,
            title = entity.title,
            chatUserId = entity.chatUserId,
            isCommon = entity.isCommon == IS_COMMON_TRUE
        )
    }
    
    fun toEntity(agent: Agent): AgentEntity {
        return AgentEntity(
            id = agent.id,
            systemName = agent.systemName,
            title = agent.title,
            chatUserId = agent.chatUserId,
            isCommon = if (agent.isCommon) IS_COMMON_TRUE else IS_COMMON_FALSE
        )
    }
}
