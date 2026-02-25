package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AgentContextMemoryEntity
import com.example.day.core.core_features.agent.data.local.model.AContextEntityData
import com.example.day.core.core_features.agent.domain.model.AContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Mapper for converting between Agent context domain models and database entities.
 * Handles JSON serialization/deserialization of AContext.
 */
internal class AgentContextMapper @Inject constructor(
    private val entityMapper: AContextEntityMapper
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    
    /**
     * Convert domain AContext to database entity with JSON string
     */
    fun toEntity(agentId: Long, context: AContext): AgentContextMemoryEntity {
        val entityData = entityMapper.toEntityData(context)
        val jsonString = json.encodeToString(entityData)
        return AgentContextMemoryEntity(
            agentId = agentId,
            context = jsonString
        )
    }
    
    /**
     * Convert database entity to domain AContext
     * NOTE(code-advice): Silent exception swallowing - catches all exceptions and returns null.
     * This could mask serialization errors. Consider adding logging for production use.
     */
    fun toDomain(entity: AgentContextMemoryEntity): AContext? {
        return try {
            val entityData = json.decodeFromString<AContextEntityData>(
                entity.context
            )
            entityMapper.toDomain(entityData)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert JSON string directly to AContext
     * NOTE(code-advice): Silent exception swallowing - catches all exceptions and returns null.
     * This could mask serialization errors. Consider adding logging for production use.
     */
    fun jsonToContext(jsonString: String): AContext? {
        return try {
            val entityData = json.decodeFromString<AContextEntityData>(
                jsonString
            )
            entityMapper.toDomain(entityData)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert AContext to JSON string
     */
    fun contextToJson(context: AContext): String {
        val entityData = entityMapper.toEntityData(context)
        return json.encodeToString(entityData)
    }
}
