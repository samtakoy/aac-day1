package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AContextEntityData
import com.example.day.core.core_features.agent.data.local.model.AContextMessageEntityData
import com.example.day.core.core_features.agent.data.local.model.SummarizationStateEntityData
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.NO_SUMMARY_LIMIT
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationState
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Mapper for converting between AContext domain models and entity data models.
 * Handles mapping between [AContext] <-> [AContextEntityData] and 
 * [AContextMessage] <-> [AContextMessageEntityData].
 */
internal class AContextEntityMapper @Inject constructor() {

    /**
     * Convert domain AContext to data AContextEntityData
     */
    fun toEntityData(context: AContext): AContextEntityData {
        val state = context.summarizationState
        return AContextEntityData(
            agentName = context.agentName,
            systemPrompt = context.systemPrompt,
            messages = context.messages.map { toEntityData(it) },
            summarizationState = SummarizationStateEntityData(
                strategy = state.strategyName,
                msgLimit = if (state is SummarizationEnabledState) state.msgLimit else NO_SUMMARY_LIMIT,
                extraLimit = if (state is SummarizationEnabledState) state.extraLimit else 8,
                summary = state.retrieveSummary()
            )
        )
    }

    /**
     * Convert data AContextEntityData to domain AContext
     */
    fun toDomain(entityData: AContextEntityData): AContext {
        val stateData = entityData.summarizationState
        val summarizationState = when (stateData.strategy) {
            "summarization" -> SummarizationState.enabled(
                msgLimit = stateData.msgLimit,
                extraLimit = stateData.extraLimit
            ).let { state -> 
                // Восстанавливаем summary если есть
                if (stateData.summary != null) {
                    state.withSummary(stateData.summary)
                } else {
                    state
                }
            }
            else -> SummarizationState.disabled()
        }
        
        return AContext(
            agentName = entityData.agentName,
            systemPrompt = entityData.systemPrompt,
            messages = entityData.messages.map { toDomain(it) }.toImmutableList(),
            summarizationState = summarizationState
        )
    }

    /**
     * Convert domain AContextMessage to data AContextMessageEntityData
     */
    fun toEntityData(message: AContextMessage): AContextMessageEntityData {
        return AContextMessageEntityData(
            role = message.role.name,
            content = message.content,
            orderNumber = message.orderNumber
        )
    }

    /**
     * Convert data AContextMessageEntityData to domain AContextMessage
     */
    fun toDomain(entityData: AContextMessageEntityData): AContextMessage {
        return AContextMessage(
            role = Role.valueOf(entityData.role),
            content = entityData.content,
            orderNumber = entityData.orderNumber
        )
    }
}
