package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatByIdAsFlowUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: Long): Flow<Chat?> = repository.getChatByIdAsFlow(chatId)
}
