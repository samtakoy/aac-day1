package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import javax.inject.Inject

/**
 * UseCase для создания stage (sub) чата в PLANNER группе.
 * Вызывается, когда пользователь подтверждает создание этапа.
 */
class CreatePlannerStageChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Создаёт новый stage чат с рабочим контекстом из родительского чата.
     *
     * @param parentChatId ID родительского чата (main planner чата)
     * @param stageTitle Название этапа (например, "Этап 1: Проектирование БД")
     * @param workingSummary Контекст задачи для передачи в новый чат
     * @return ID созданного чата
     */
    suspend operator fun invoke(
        parentChatId: Long,
        stageTitle: String,
        workingSummary: String?
    ): Long {
        return chatRepository.createSubChat(
            parentId = parentChatId,
            title = stageTitle,
            workingSummary = workingSummary
        )
    }
}
