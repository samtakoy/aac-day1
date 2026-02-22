package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatGroupColors
import com.example.day.core.core_features.chat.domain.model.ChatType
import javax.inject.Inject

class CreateChatGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(title: String, chatType: ChatType, colorIndex: Int? = null): Long {
        require(title.isNotBlank()) { "Group title cannot be blank" }
        
        // Берем группу с максимальным id, получаем её colorIndex и увеличиваем на 1
        val lastGroup = repository.getChatGroupWithMaxId()
        val nextColorIndex = if (lastGroup != null) {
            (lastGroup.colorIndex + 1) % ChatGroupColors.colors.size
        } else {
            0
        }
        val color = colorIndex ?: nextColorIndex
        
        return repository.createChatGroup(title, chatType, color)
    }
}
