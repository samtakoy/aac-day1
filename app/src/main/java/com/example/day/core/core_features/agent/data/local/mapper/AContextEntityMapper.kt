package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AContextEntityData
import com.example.day.core.core_features.agent.data.local.model.AContextEntityData.*
import com.example.day.core.core_features.agent.data.local.model.AContextMessageEntityData
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.Role
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
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
    fun toEntityData(context: AContextState): AContextEntityData {
        return when (context) {
            is AContextState.Full -> Full(
                messages = context.messages.map{ toEntityData(it) }
            )
            is AContextState.SlidingWindow -> SlidingWindow(
                messages = context.messages.map { toEntityData(it) }
            )
            is AContextState.Summary -> Summarization(
                summary = context.summary,
                messages = context.messages.map { toEntityData(it) }
            )
            is AContextState.StickyFacts -> StickyFacts(
                facts = context.facts,
                messages = context.messages.map { toEntityData(it) }
            )
            is AContextState.Branching -> Branching(
                branches = context.branches.mapValues { (_, messages) ->
                    messages.map { toEntityData(it) }
                },
                currentBranchId = context.currentBranchId,
                defaultBranchId = context.defaultBranchId
            )
            AContextState.Empty -> Empty
        }
    }

    /**
     * Convert data AContextEntityData to domain AContext
     */
    fun toDomain(entityData: AContextEntityData): AContextState {
        return when (entityData) {
            is Full -> AContextState.Full(
                messages = entityData.messages.map{ toDomain(it) }.toPersistentList()
            )
            is SlidingWindow -> AContextState.SlidingWindow(
                messages = entityData.messages.map { toDomain(it) }.toPersistentList()
            )
            is Summarization -> AContextState.Summary(
                summary = entityData.summary,
                messages = entityData.messages.map { toDomain(it) }.toPersistentList()
            )
            is StickyFacts -> AContextState.StickyFacts(
                facts = entityData.facts,
                messages = entityData.messages.map { toDomain(it) }.toPersistentList()
            )
            is Branching -> AContextState.Branching(
                branches = entityData.branches.mapValues { (_, messages) ->
                    messages.map { toDomain(it) }.toPersistentList()
                }.toPersistentMap(),
                currentBranchId = entityData.currentBranchId,
                defaultBranchId = entityData.defaultBranchId
            )
            else -> AContextState.Empty
        }
    }

    /**
     * Convert domain AContextMessage to data AContextMessageEntityData
     */
    fun toEntityData(message: AContextMessage): AContextMessageEntityData {
        return AContextMessageEntityData(
            role = message.role.name,
            content = message.content,
        )
    }

    /**
     * Convert data AContextMessageEntityData to domain AContextMessage
     */
    fun toDomain(entityData: AContextMessageEntityData): AContextMessage {
        return AContextMessage(
            role = Role.valueOf(entityData.role),
            content = entityData.content
        )
    }
}
