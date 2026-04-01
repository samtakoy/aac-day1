package com.example.day.core.core_features.pr_review.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.day.app.MyApp
import java.util.concurrent.TimeUnit

class TelegramPollingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appComponent = (applicationContext as? MyApp)?.appComponent
            ?: return Result.failure()

        val telegramRepo = appComponent.telegramRepository()
        val prHandleRepo = appComponent.prHandleRepository()
        val startPrReviewUseCase = appComponent.startPrReviewUseCase()

        val lastUpdateId = prHandleRepo.getLastTelegramUpdateId()
        Log.d(TAG, "lastUpdateId=$lastUpdateId, offset=${lastUpdateId + 1}")
        val result = telegramRepo.getPrUpdates(offset = lastUpdateId + 1)

        result.fold(
            onSuccess = { events ->
                if (events.isEmpty()) {
                    Log.d(TAG, "пусто")
                } else {
                    val event = events[0]
                    Log.d(TAG, "новый pr: ${event.prNumber}")
                    startPrReviewUseCase(event.prNumber, event.repo)
                    prHandleRepo.saveLastTelegramUpdateId(event.updateId)
                }
            },
            onFailure = { error ->
                Log.e(TAG, "ошибка: ${error.message}")
            }
        )

        val nextRequest = OneTimeWorkRequestBuilder<TelegramPollingWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(nextRequest)

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "telegram_polling"
        const val WORK_TAG = "telegram_polling_tag"
        private const val TAG = "TelegramPolling"
    }
}
