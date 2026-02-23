package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatSettingsEntity
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import javax.inject.Inject

internal class ChatSettingsMapper @Inject constructor(
    private val modelSettingsMapper: ModelSettingsMapper
) {
    
    fun toDomain(entity: ChatSettingsEntity): ChatSettings {
        return ChatSettings(
            chatId = entity.chatId,
            systemPromt = entity.systemPrompt,
            model = modelSettingsMapper.fromJson(entity.modelSettingsJson)
        )
    }
    
    fun toEntity(model: ChatSettings): ChatSettingsEntity {
        return ChatSettingsEntity(
            chatId = model.chatId,
            systemPrompt = model.systemPromt,
            modelSettingsJson = modelSettingsMapper.toJson(model.model)
        )
    }
}
