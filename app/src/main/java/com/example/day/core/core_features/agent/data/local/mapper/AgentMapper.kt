package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.relation.AgentWithMemoriesRelation
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.llm.data.local.mapper.ModelSettingsMapper
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import javax.inject.Inject

/**
 * Mapper for converting between Agent domain model and AgentEntity.
 */
internal class AgentMapper @Inject constructor(
    private val strategyTypeMapper: CtxStrategyTypeMapper,
    private val modelSettingsMapper: ModelSettingsMapper
) {
    
    companion object {
        const val IS_COMMON_TRUE = 1
        const val IS_COMMON_FALSE = 0
    }
    
    fun toDomain(entity: AgentWithMemoriesRelation): AgentConfig {
        return AgentConfig(
            id = entity.agent.id,
            systemName = entity.agent.systemName,
            title = entity.agent.title,
            chatUserId = entity.agent.chatUserId,
            isCommon = entity.agent.isCommon == IS_COMMON_TRUE,
            modelSettings = modelSettingsMapper.fromJson(entity.agent.modelSettings),
            systemPrompt = entity.agent.systemPrompt,
            contextStrategyType = strategyTypeMapper.toDomain(entity.agent.contextStrategyType),
            memoryTypes = memoryTypesToDomain(entity.memoryTypeNames)
        )
    }
    
    fun toEntity(agentConfig: AgentConfig): AgentEntity {
        return AgentEntity(
            id = agentConfig.id,
            systemName = agentConfig.systemName,
            title = agentConfig.title,
            chatUserId = agentConfig.chatUserId,
            isCommon = if (agentConfig.isCommon) IS_COMMON_TRUE else IS_COMMON_FALSE,
            modelSettings = modelSettingsMapper.toJson(agentConfig.modelSettings),
            systemPrompt = agentConfig.systemPrompt,
            contextStrategyType = strategyTypeMapper.toEntity(agentConfig.contextStrategyType)
        )
    }

    private fun memoryTypesToDomain(types: List<String>): List<MemoryType> {
        return types.mapNotNull { dbName ->
            try {
                MemoryType.entries.find { it.dbName == dbName }
            } catch (_: Exception) {
                null
            }
        }
    }
}
