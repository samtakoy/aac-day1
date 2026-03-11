package com.example.day.core.core_features.reminder.di

import android.content.Context
import androidx.work.WorkManager
import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.reminder.data.ReminderRepositoryImpl
import com.example.day.core.core_features.reminder.data.ReminderSchedulerImpl
import com.example.day.core.core_features.reminder.data.local.dao.ReminderDao
import com.example.day.core.core_features.reminder.domain.repository.ReminderRepository
import com.example.day.core.core_features.reminder.domain.scheduler.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal interface ReminderCoreFeatureModule {
    @Binds
    @Singleton
    fun bindsReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    fun bindsReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler

    companion object {
        @Provides
        internal fun provideReminderDao(db: ChatDatabase): ReminderDao = db.reminderDao()

        @Provides
        @Singleton
        fun provideWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
