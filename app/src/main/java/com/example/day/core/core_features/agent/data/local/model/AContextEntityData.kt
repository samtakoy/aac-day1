package com.example.day.core.core_features.agent.data.local.model

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

/**
 * Data layer representation of AContext for JSON serialization.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContext]
 */
@Serializable
data class AContextEntityData(
    val agentName: String,
    val systemPrompt: String,
    val messages: List<AContextMessageEntityData>  // Use List instead of ImmutableList for serialization
)

/**
 * Data layer representation of AContextMessage for JSON serialization.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContextMessage]
 */
@Serializable
data class AContextMessageEntityData(
    val role: String,  // Role enum serialized as String
    val content: String,
    val orderNumber: Long
)

/**
 * Extension to convert from domain AContext to data AContextEntityData
 */
fun com.example.day.core.core_features.agent.domain.model.AContext.toEntityData(): AContextEntityData {
    return AContextEntityData(
        agentName = agentName,
        systemPrompt = systemPrompt,
        messages = messages.map { it.toEntityData() }
    )
}

/**
 * Extension to convert from data AContextEntityData to domain AContext
 */
fun AContextEntityData.toDomain(): com.example.day.core.core_features.agent.domain.model.AContext {
    return com.example.day.core.core_features.agent.domain.model.AContext(
        agentName = agentName,
        systemPrompt = systemPrompt,
        messages = messages.map { it.toDomain() }.toImmutableList()
    )
}

/**
 * Extension to convert from domain AContextMessage to data AContextMessageEntityData
 */
fun AContextMessage.toEntityData(): AContextMessageEntityData {
    return AContextMessageEntityData(
        role = role.name,
        content = content,
        orderNumber = orderNumber
    )
}

/**
 * Extension to convert from data AContextMessageEntityData to domain AContextMessage
 */
fun AContextMessageEntityData.toDomain(): AContextMessage {
    return AContextMessage(
        role = Role.valueOf(role),
        content = content,
        orderNumber = orderNumber
    )
}
