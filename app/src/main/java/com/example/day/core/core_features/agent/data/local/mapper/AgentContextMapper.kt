package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AgentContextMemoryEntity
import com.example.day.core.core_features.agent.data.local.model.AContextEntityData
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Mapper for converting between Agent context domain models and database entities.
 * Handles JSON serialization/deserialization of AContext.
 */
internal class AgentContextMapper @Inject constructor(
    private val entityMapper: AContextEntityMapper,
    private val settingsMapper: AContextSettingsMapper
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
        return AgentContextMemoryEntity(
            agentId = agentId,
            context = contextToJson(context.data),
            settings = paramsToJson(context.params)
        )
    }

    
    /**
     * Convert database entity to domain AContext
     * NOTE(code-advice): Silent exception swallowing - catches all exceptions and returns null.
     * This could mask serialization errors. Consider adding logging for production use.
     */
    fun toDomain(entity: AgentContextMemoryEntity): AContext? {
        return try {
            AContext(
                jsonToParams(entity.settings)!!,
                jsonToContext(entity.context)!!
            )
        } catch (e: Exception) {
            null
        }
    }

    fun jsonToParams(jsonString: String): AContextParams? {
        return try {
            val entity = json.decodeFromString<AContextEntitySettings>(
                jsonString
            )
            settingsMapper.toDomain(entity)
        } catch (_: Throwable) {
            null
        }
    }

    fun paramsToJson(params: AContextParams): String {
        val settings = settingsMapper.toEntityData(params)
        return json.encodeToString(settings)
    }

    /**
     * Convert JSON string directly to AContext
     * NOTE(code-advice): Silent exception swallowing - catches all exceptions and returns null.
     * This could mask serialization errors. Consider adding logging for production use.
     */
    fun jsonToContext(jsonString: String): AContextState? {
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
    fun contextToJson(context: AContextState): String {
        val entityData = entityMapper.toEntityData(context)
        return json.encodeToString(entityData)
    }
}
