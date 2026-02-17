package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import javax.inject.Inject

class ChangeMessageStatusUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(messageId: Long, status: ChatMessageStatus) {
        repository.changeMessageStatus(messageId, status)
    }
}
