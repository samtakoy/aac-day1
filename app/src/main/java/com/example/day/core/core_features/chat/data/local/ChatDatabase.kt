package com.example.day.core.core_features.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.data.local.model.UserEntity

@Database(
    entities = [UserEntity::class, ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
