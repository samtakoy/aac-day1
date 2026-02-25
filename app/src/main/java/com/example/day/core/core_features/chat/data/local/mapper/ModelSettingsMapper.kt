package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ModelSettingsEntity
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class ModelSettingsMapper @Inject constructor() {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    fun toDomain(entity: ModelSettingsEntity): ModelSettings {
        return ModelSettings(
            name = entity.name,
            stopSequence = entity.stopSequence.toImmutableList(),
            maxTokens = entity.maxTokens,
            maxCompletionTokens = entity.maxCompletionTokens,
            jsonFormat = entity.jsonFormat,
            temperature = entity.temperature,
            topP = entity.topP,
            topK = entity.topK,
            presencePenalty = entity.presencePenalty,
            frequencyPenalty = entity.frequencyPenalty,
            seed = entity.seed,
            reasoningEffort = entity.reasoningEffort
        )
    }
    
    fun toEntity(model: ModelSettings): ModelSettingsEntity {
        return ModelSettingsEntity(
            name = model.name,
            stopSequence = model.stopSequence.toList(),
            maxTokens = model.maxTokens,
            maxCompletionTokens = model.maxCompletionTokens,
            jsonFormat = model.jsonFormat,
            temperature = model.temperature,
            topP = model.topP,
            topK = model.topK,
            presencePenalty = model.presencePenalty,
            frequencyPenalty = model.frequencyPenalty,
            seed = model.seed,
            reasoningEffort = model.reasoningEffort
        )
    }
    
    fun toJson(model: ModelSettings): String {
        return json.encodeToString(toEntity(model))
    }
    
    fun fromJson(jsonString: String): ModelSettings {
        val entity = json.decodeFromString<ModelSettingsEntity>(jsonString)
        return toDomain(entity)
    }
}
