package com.example.day.core.core_features.reminder.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.day.app.MyApp

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(ReminderWorkConstants.INPUT_REMINDER_ID)
            ?: return Result.failure()
        val appComponent = (applicationContext as? MyApp)?.appComponent
            ?: return Result.failure()
        return appComponent.executeReminderUseCase().invoke(reminderId)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.failure() }
            )
    }
}
