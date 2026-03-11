package com.example.day.core.core_features.reminder.domain.model

data class Reminder(
    val id: String,
    val agentId: Long,
    val chatId: Long?,
    val text: String,
    val remindAt: Long,
    val createdAt: Long,
    val status: ReminderStatus,
    val repeatIntervalMinutes: Long?,
    val lastTriggeredAt: Long?
)

enum class ReminderStatus {
    Pending,
    Done,
    Failed
}
