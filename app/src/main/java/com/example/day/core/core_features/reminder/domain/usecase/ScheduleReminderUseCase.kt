package com.example.day.core.core_features.reminder.domain.usecase

import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.repository.ReminderRepository
import com.example.day.core.core_features.reminder.domain.scheduler.ReminderScheduler
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder) {
        repository.insert(reminder)
        scheduler.schedule(reminder)
    }
}
