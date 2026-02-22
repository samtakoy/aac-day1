package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import javax.inject.Inject

class UpdateChatGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(group: ChatGroup) {
        require(group.title.isNotBlank()) { "Group title cannot be blank" }
        repository.updateChatGroup(group)
    }
}
