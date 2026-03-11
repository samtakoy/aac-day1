package com.example.day.core.core_features.reminder.data

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.day.core.core_features.reminder.data.worker.ReminderWorkConstants
import com.example.day.core.core_features.reminder.data.worker.ReminderWorker
import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.scheduler.ReminderScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
internal class ReminderSchedulerImpl @Inject constructor(
    private val workManager: WorkManager
) : ReminderScheduler {
    override fun schedule(reminder: Reminder) {
        val delayMs = max(reminder.remindAt - System.currentTimeMillis(), 0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(ReminderWorkConstants.INPUT_REMINDER_ID to reminder.id)
            )
            .build()
        val workName = ReminderWorkConstants.WORK_NAME_PREFIX + reminder.id
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(reminderId: String) {
        val workName = ReminderWorkConstants.WORK_NAME_PREFIX + reminderId
        workManager.cancelUniqueWork(workName)
    }
}
