package com.example.day.core.core_features.agent.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.day.core.core_features.chat.data.local.model.UserEntity

/**
 * Entity representing an Agent in the database.
 * 
 * @property id Unique identifier (auto-generated)
 * @property systemName Internal system name
 * @property title Display title
 * @property chatUserId Foreign key to UserEntity (the "avatar" user this agent communicates as)
 * @property isCommon If 1, agent can be used in any chat without explicit binding
 */
@Entity(
    tableName = "agents",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chat_user_id"])]
)
internal data class AgentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "system_name")
    val systemName: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "chat_user_id")
    val chatUserId: Long,
    
    @ColumnInfo(name = "is_common")
    val isCommon: Int  // 1 = true, 0 = false
)
