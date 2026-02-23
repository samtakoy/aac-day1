package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatWithGroupAndSettings
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.ModelConst
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import javax.inject.Inject

internal class ChatMapper @Inject constructor(
    private val groupMapper: ChatGroupMapper,
    private val settingsMapper: ChatSettingsMapper
) {
    fun toDomain(entity: ChatWithGroupAndSettings): Chat {
        val settings = entity.settings?.let { settingsMapper.toDomain(it) }
            ?: createDefaultSettings(entity.chat.id)
        
        return Chat(
            id = entity.chat.id,
            title = entity.chat.title,
            chatGroup = groupMapper.toDomain(entity.groupWithType),
            settings = settings
        )
    }

    fun toEntity(model: Chat): ChatEntity = ChatEntity(
        id = model.id,
        title = model.title,
        chatGroupId = model.chatGroup.id
    )
    
    private fun createDefaultSettings(chatId: Long): ChatSettings {
        return ChatSettings(
            chatId = chatId,
            systemPromt = "",
            model = ModelSettings(name = ModelConst.DEFAULT_MODEL)
        )
    }
}
