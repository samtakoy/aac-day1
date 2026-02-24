package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

class GetOrCreateChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(title: String, groupId: Long): Chat {
        return repository.getOrCreateChat(title, groupId)
    }
}
