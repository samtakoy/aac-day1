package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.AContextEntityData
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.Empty
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.Full
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.StickyFacts
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.Branching
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.SlidingWindow
import com.example.day.core.core_features.agent.data.local.model.AContextEntitySettings.Summarization
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class AContextSettingsMapper @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun toEntityData(context: AContextParams): AContextEntitySettings {
        return when (context) {
            is AContextParams.Full -> Full
            is AContextParams.SlidingWindow -> SlidingWindow(
                windowSize = context.windowSize,
            )
            is AContextParams.Summarization -> Summarization(
                msgLimit = context.msgLimit,
                extraLimit = context.extraLimit,
            )
            is AContextParams.StickyFacts -> StickyFacts(
                windowSize = context.windowSize,
                maxFacts = context.maxFacts,
            )
            is AContextParams.Branching -> Branching(
                defaultBranchId = context.defaultBranchId
            )
            AContextParams.Empty -> Empty
        }
    }

    fun toDomain(entityData: AContextEntitySettings): AContextParams {
        return when (entityData) {
            is Full -> AContextParams.Full
            is SlidingWindow -> AContextParams.SlidingWindow(
                windowSize = entityData.windowSize,
            )
            is Summarization -> AContextParams.Summarization(
                msgLimit = entityData.msgLimit,
                extraLimit = entityData.extraLimit,
            )
            is StickyFacts -> AContextParams.StickyFacts(
                windowSize = entityData.windowSize,
                maxFacts = entityData.maxFacts,
            )
            is Branching -> AContextParams.Branching(
                defaultBranchId = entityData.defaultBranchId
            )
            Empty -> AContextParams.Empty
        }
    }

    fun jsonToContext(jsonString: String): AContextParams? {
        return try {
            val entityData = json.decodeFromString<AContextEntitySettings>(
                jsonString
            )
            toDomain(entityData)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert AContext to JSON string
     */
    fun contextToJson(context: AContextParams): String {
        val entityData = toEntityData(context)
        return json.encodeToString(entityData)
    }
}