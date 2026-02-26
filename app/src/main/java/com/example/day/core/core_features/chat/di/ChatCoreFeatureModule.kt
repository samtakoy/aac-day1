package com.example.day.core.core_features.chat.di

import android.content.Context
import androidx.room.Room
import com.example.day.core.core_features.chat.data.ChatRepositoryImpl
import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.ChatGroupDao
import com.example.day.core.core_features.chat.data.local.dao.ChatSettingsDao
import com.example.day.core.core_features.chat.data.local.dao.ChatTypeDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.tools.ChatToolsImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal interface ChatCoreFeatureModule {
    @Binds
    fun bindsChatRepository(impl: ChatRepositoryImpl): ChatRepository
    companion object {
        @Provides
        @Singleton
        fun providesChatDatabase(context: Context): ChatDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "chat_database.db"
            )
                .build()
        }

        @Provides
        internal fun provideUserDao(db: ChatDatabase): UserDao = db.userDao()

        @Provides
        internal fun provideChatDao(db: ChatDatabase): ChatDao = db.chatDao()

        @Provides
        internal fun provideMessageDao(db: ChatDatabase): MessageDao = db.messageDao()

        @Provides
        internal fun provideChatGroupDao(db: ChatDatabase): ChatGroupDao = db.chatGroupDao()

        @Provides
        internal fun provideChatTypeDao(db: ChatDatabase): ChatTypeDao = db.chatTypeDao()

        @Provides
        internal fun provideChatSettingsDao(db: ChatDatabase): ChatSettingsDao = db.chatSettingsDao()
    }
}
