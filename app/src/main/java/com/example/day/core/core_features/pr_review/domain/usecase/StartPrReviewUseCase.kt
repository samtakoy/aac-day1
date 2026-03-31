package com.example.day.core.core_features.pr_review.domain.usecase

import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import com.example.day.core.core_features.pr_review.domain.worker.PrReviewWorker
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartPrReviewUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository,
    private val chatTools: ChatTools,
    private val prReviewWorker: PrReviewWorker
) {
    suspend operator fun invoke(prNumber: Int, repo: String) {
        val state = prHandleRepository.getPrHandleStateFlow().first()
        val chatId = state.chatId
        if (chatId < 0) return

        chatTools.addInfoMessage(chatId, "🔍 Начинаем ревью PR #$prNumber: $repo")

        prReviewWorker.doWork(prNumber, repo, chatId)
            .onSuccess {
                chatTools.addInfoMessage(chatId, "✅ Ревью PR #$prNumber завершено")
            }
            .onFailure { error ->
                chatTools.addInfoMessage(chatId, "❌ Ревью PR #$prNumber завершилось с ошибкой: ${error.message}")
            }
    }
}
