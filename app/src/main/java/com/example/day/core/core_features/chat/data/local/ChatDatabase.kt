package com.example.day.core.core_features.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import com.example.day.core.core_features.agent.data.local.model.AgentContextMemoryEntity
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
        ChatSettingsEntity::class,
        AgentEntity::class,
        AgentToChatEntity::class,
        AgentContextMemoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun chatTypeDao(): ChatTypeDao
    abstract fun chatGroupDao(): ChatGroupDao
    abstract fun chatSettingsDao(): ChatSettingsDao
    abstract fun agentDao(): AgentDao
    abstract fun agentToChatDao(): AgentToChatDao
    abstract fun agentContextMemoryDao(): AgentContextMemoryDao
}
