package com.example.day.core.core_features.reminder.data

import com.example.day.core.core_features.reminder.data.local.dao.ReminderDao
import com.example.day.core.core_features.reminder.data.local.mapper.ReminderMapper
import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val mapper: ReminderMapper
) : ReminderRepository {
    override suspend fun insert(reminder: Reminder) {
        dao.insert(mapper.toEntity(reminder))
    }

    override suspend fun getById(id: String): Reminder? {
        return dao.getById(id)?.let(mapper::toDomain)
    }

    override suspend fun update(reminder: Reminder) {
        dao.update(mapper.toEntity(reminder))
    }
}
