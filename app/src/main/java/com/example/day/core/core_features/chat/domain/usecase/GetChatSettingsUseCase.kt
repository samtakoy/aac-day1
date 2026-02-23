package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import javax.inject.Inject

class GetChatSettingsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long): ChatSettings? {
        return repository.getChatSettings(chatId)
    }
}
