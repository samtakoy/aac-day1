package com.example.day.core.core_features.agent.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.day.core.core_features.chat.data.local.model.ChatEntity

/**
 * Entity representing the many-to-many relationship between Agents and Chats.
 * Used when isCommon = 0 (agent is only available in specific chats).
 * 
 * @property agentId Foreign key to AgentEntity
 * @property chatId Foreign key to ChatEntity
 */
@Entity(
    tableName = "agent_to_chat",
    primaryKeys = ["agent_id", "chat_id"],
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["agent_id"]),
        Index(value = ["chat_id"])
    ]
)
internal data class AgentToChatEntity(
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    
    @ColumnInfo(name = "chat_id")
    val chatId: Long
)
