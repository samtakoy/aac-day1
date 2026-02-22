package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatType
import javax.inject.Inject

class GetChatTypesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): List<ChatType> {
        repository.ensureChatTypesExist()
        return repository.getAllChatTypes()
    }
}
