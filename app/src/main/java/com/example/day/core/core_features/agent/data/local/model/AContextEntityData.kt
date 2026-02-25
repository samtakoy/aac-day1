package com.example.day.core.core_features.agent.data.local.model

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
