package com.example.day.core.core_features.reminder.data.local.mapper

import com.example.day.core.core_features.reminder.data.local.model.ReminderEntity
import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.model.ReminderStatus
import javax.inject.Inject

internal class ReminderMapper @Inject constructor() {
    fun toEntity(domain: Reminder): ReminderEntity {
        return ReminderEntity(
            id = domain.id,
            agentId = domain.agentId,
            chatId = domain.chatId,
            text = domain.text,
            remindAt = domain.remindAt,
            createdAt = domain.createdAt,
            status = domain.status.name,
            repeatIntervalMinutes = domain.repeatIntervalMinutes,
            lastTriggeredAt = domain.lastTriggeredAt
        )
    }

    fun toDomain(entity: ReminderEntity): Reminder {
        return Reminder(
            id = entity.id,
            agentId = entity.agentId,
            chatId = entity.chatId,
            text = entity.text,
            remindAt = entity.remindAt,
            createdAt = entity.createdAt,
            status = runCatching { ReminderStatus.valueOf(entity.status) }
                .getOrDefault(ReminderStatus.Pending),
            repeatIntervalMinutes = entity.repeatIntervalMinutes,
            lastTriggeredAt = entity.lastTriggeredAt
        )
    }
}
