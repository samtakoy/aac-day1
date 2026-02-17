package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import javax.inject.Inject

class AddChatMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        timestamp: Long,
        userType: UserType,
        text: String,
        status: ChatMessageStatus
    ): Long {
        return repository.addMessage(chatId, timestamp, userType, text, status)
    }
}
