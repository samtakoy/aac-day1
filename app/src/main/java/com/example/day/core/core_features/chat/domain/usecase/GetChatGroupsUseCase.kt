package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatGroupsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatGroup>> {
        return repository.getAllChatGroups()
    }
}
