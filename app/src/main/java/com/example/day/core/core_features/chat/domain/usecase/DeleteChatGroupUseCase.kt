package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import javax.inject.Inject

class DeleteChatGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(groupId: Long) {
        repository.deleteChatGroup(groupId)
    }
}
