package com.example.day.core.core_features.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import com.example.day.core.core_features.agent.data.local.model.AgentContextMemoryEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToMemoryTypeEntity
import com.example.day.core.core_features.memory.data.local.dao.ArtifactDao
import com.example.day.core.core_features.memory.data.local.dao.UserProfileDao
import com.example.day.core.core_features.memory.data.local.dao.LTMGroupDao
import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.ChatGroupDao
import com.example.day.core.core_features.chat.data.local.dao.ChatSettingsDao
import com.example.day.core.core_features.chat.data.local.dao.ChatTypeDao
import com.example.day.core.core_features.memory.data.local.dao.LongTermMemoryDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.ChatSettingsEntity
import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.memory.data.local.model.ProjectArtifactEntity
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.mcp.data.local.dao.FileAnalysisDao
import com.example.day.core.core_features.memory.data.local.model.link.LTMGroupToAgentEntity
import com.example.day.core.core_features.memory.data.local.model.link.LTMGroupToChatGroupEntity
import com.example.day.core.core_features.memory.data.local.model.link.LTMGroupToUserProfileEntity
import com.example.day.core.core_features.memory.data.local.model.link.UserToProfileEntity
import com.example.day.core.core_features.memory.data.local.model.user.UserProfileEntity
import com.example.day.core.core_features.mcp.data.local.dao.GitFileCacheDao
import com.example.day.core.core_features.mcp.data.local.dao.McpServerDao
import com.example.day.core.core_features.mcp.data.local.entity.FileAnalysisEntity
import com.example.day.core.core_features.mcp.data.local.entity.GitFileCacheEntity
import com.example.day.core.core_features.mcp.data.local.entity.McpServerEntity
import com.example.day.core.core_features.reminder.data.local.dao.ReminderDao
import com.example.day.core.core_features.reminder.data.local.model.ReminderEntity

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
        AgentContextMemoryEntity::class,
        AgentToMemoryTypeEntity::class,
        // Memory entities
        LTMGroupEntity::class,
        LongTermMemoryEntity::class,
        ProjectArtifactEntity::class,
        UserProfileEntity::class,
        // Link entities
        LTMGroupToAgentEntity::class,
        LTMGroupToChatGroupEntity::class,
        LTMGroupToUserProfileEntity::class,
        UserToProfileEntity::class,
        McpServerEntity::class,
        ReminderEntity::class,
        GitFileCacheEntity::class,
        FileAnalysisEntity::class
    ],
    version = 16,
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
    abstract fun agentMemoryDao(): AgentMemoryDao
    abstract fun agentToChatDao(): AgentToChatDao
    abstract fun agentContextMemoryDao(): AgentContextMemoryDao
    // Memory DAOs
    abstract fun ltmGroupDao(): LTMGroupDao
    abstract fun longTermMemoryDao(): LongTermMemoryDao
    abstract fun artifactDao(): ArtifactDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mcpServerDao(): McpServerDao
    abstract fun reminderDao(): ReminderDao
    abstract fun gitFileCacheDao(): GitFileCacheDao
    abstract fun fileAnalysisDao(): FileAnalysisDao
}
