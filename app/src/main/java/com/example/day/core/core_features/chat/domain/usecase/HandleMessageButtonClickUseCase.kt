package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import javax.inject.Inject

class HandleMessageButtonClickUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(messageId: Long, actionId: String) {
        val message = repository.getMessageById(messageId) ?: return
        val currentButtons = message.buttons ?: return

        // Помечаем нажатую кнопку и блокируем все
        val updatedList = currentButtons.list.map {
            if (it.actionId == actionId) it.copy(isPressed = true) else it
        }
        val newButtons = currentButtons.copy(list = updatedList, isEnabled = false)

        repository.updateMessageButtons(messageId, newButtons)
    }
}
