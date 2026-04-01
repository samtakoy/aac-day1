package com.example.day.core.core_features.pr_review.domain.usecase

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.pr_review.data.worker.TelegramPollingWorker
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import javax.inject.Inject

class SetPrHandleEnabledUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings? = null) {
        prHandleRepository.setPrHandleState(isEnabled, chatId, modelSettings)
        if (isEnabled) {
            workManager.cancelAllWorkByTag(TelegramPollingWorker.WORK_TAG)
            val request = OneTimeWorkRequestBuilder<TelegramPollingWorker>()
                .addTag(TelegramPollingWorker.WORK_TAG)
                .build()
            workManager.enqueue(request)
        } else {
            workManager.cancelAllWorkByTag(TelegramPollingWorker.WORK_TAG)
        }
    }
}
