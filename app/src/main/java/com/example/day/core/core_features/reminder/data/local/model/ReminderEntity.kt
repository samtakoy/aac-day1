package com.example.day.core.core_features.reminder.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
internal data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "agent_id")
    val agentId: Long,

    @ColumnInfo(name = "chat_id")
    val chatId: Long?,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "remind_at")
    val remindAt: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "repeat_interval_minutes")
    val repeatIntervalMinutes: Long?,

    @ColumnInfo(name = "last_triggered_at")
    val lastTriggeredAt: Long?
)
