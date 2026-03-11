package com.example.day.core.core_features.reminder.domain.repository

import com.example.day.core.core_features.reminder.domain.model.Reminder

interface ReminderRepository {
    suspend fun insert(reminder: Reminder)
    suspend fun getById(id: String): Reminder?
    suspend fun update(reminder: Reminder)
}
