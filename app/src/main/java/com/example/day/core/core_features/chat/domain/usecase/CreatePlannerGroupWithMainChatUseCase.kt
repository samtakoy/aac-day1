package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatType
import javax.inject.Inject

/**
 * Use case for creating a PLANNER type group with its main chat.
 * 
 * This is needed because PLANNER groups require special initialization:
 * - Create the group
 * - Create the main "Main Planner" chat
 * - Mark it as the main planner chat (isPlannerMain = true)
 * 
 * All this must happen in a single transaction to ensure data integrity.
 */
class CreatePlannerGroupWithMainChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Creates a new PLANNER group with a main chat.
     * 
     * @param title The title for the group
     * @param chatType The chat type (must be ChatType.PLANNER)
     * @param colorIndex The color index for the group
     * @return The ID of the created main chat
     */
    suspend operator fun invoke(
        title: String,
        chatType: ChatType,
        colorIndex: Int
    ): Long {
        return chatRepository.createPlannerGroupWithMainChat(
            title = title,
            chatType = chatType,
            colorIndex = colorIndex
        )
    }
}
