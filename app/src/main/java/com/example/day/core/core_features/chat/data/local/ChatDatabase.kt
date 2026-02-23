package com.example.day.core.core_features.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.ChatGroupDao
import com.example.day.core.core_features.chat.data.local.dao.ChatSettingsDao
import com.example.day.core.core_features.chat.data.local.dao.ChatTypeDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.ChatSettingsEntity
import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.data.local.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ChatTypeEntity::class,
        ChatGroupEntity::class,
        ChatSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun chatTypeDao(): ChatTypeDao
    abstract fun chatGroupDao(): ChatGroupDao
    abstract fun chatSettingsDao(): ChatSettingsDao
}
