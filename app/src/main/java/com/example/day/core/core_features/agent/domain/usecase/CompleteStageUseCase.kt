package com.example.day.core.core_features.agent.domain.usecase

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import javax.inject.Inject

// Новый UseCase: инкапсулирует всю логику завершения этапа
class CompleteStageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val artifactRepository: ArtifactRepository
) {
    suspend operator fun invoke(
        stageChatId: Long,
        stageTitle: String,
        parentId: Long,
        outcome: String
    ): Result<Unit> = runCatching {
        // 1. Сохраняем артефакт
        artifactRepository.saveArtifact(
            chatId = stageChatId,
            stageTitle = stageTitle,
            content = outcome
        )

        val parentChat = chatRepository.getChatById(parentId)

        val updatedSummary = buildParentSummary(
            currentSummary = parentChat?.workingSummary,
            stageTitle = stageTitle,
            outcome = outcome
        )

        chatRepository.updateWorkingSummary(parentId, updatedSummary)
    }

    private fun buildParentSummary(
        currentSummary: String?,
        stageTitle: String,
        outcome: String
    ): String = buildString {
        currentSummary?.takeIf { it.isNotEmpty() }?.let { append(it).append("\n\n") }
        append("### ").append(stageTitle).append("\n").append(outcome)
    }
}