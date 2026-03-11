package com.example.day.core.core_features.reminder.domain.scheduler

import com.example.day.core.core_features.reminder.domain.model.Reminder

interface ReminderScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: String)
}
