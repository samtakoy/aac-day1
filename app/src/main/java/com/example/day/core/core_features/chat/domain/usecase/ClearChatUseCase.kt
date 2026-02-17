package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import javax.inject.Inject

class ClearChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long) {
        repository.clearChat(chatId)
    }
}
