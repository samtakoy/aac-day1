package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.llm.data.local.mapper.ModelSettingsMapper
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
    
    fun toDomain(entity: AgentEntity): AgentConfig {
        return AgentConfig(
            id = entity.id,
            systemName = entity.systemName,
            title = entity.title,
            chatUserId = entity.chatUserId,
            isCommon = entity.isCommon == IS_COMMON_TRUE,
            modelSettings = modelSettingsMapper.fromJson(entity.modelSettings),
            systemPrompt = entity.systemPrompt,
            contextStrategyType = strategyTypeMapper.toDomain(entity.contextStrategyType)
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
}
