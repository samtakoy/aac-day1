package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatMessage
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
        status: ChatMessageStatus,
        type: ChatMessage.Type,
        buttons: ChatMessage.Buttons? = null
    ): Long {
        // Если сообщение от пользователя - удаляем активные сообщения с кнопками
        if (userType == UserType.User) {
            val buttonMessages = repository.getMessagesByType(chatId, ChatMessage.Type.Buttons)
            buttonMessages.forEach { msg ->
                if (msg.buttons?.isEnabled == true) {
                    repository.removeMessage(msg.id)
                }
            }
        }
        return repository.addMessage(chatId, timestamp, userType, text, status, type, buttons)
    }
}
