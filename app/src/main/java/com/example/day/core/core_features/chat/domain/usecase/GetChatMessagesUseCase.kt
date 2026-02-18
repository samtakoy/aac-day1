package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatMessagesWithStatusUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long, status: ChatMessageStatus): List<ChatMessage> {
        return repository.getChatMessages(chatId, status)
    }
}
