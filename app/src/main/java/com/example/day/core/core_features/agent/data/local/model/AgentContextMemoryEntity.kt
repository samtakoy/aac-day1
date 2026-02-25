package com.example.day.core.core_features.agent.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing agent conversation context in the database.
 * Stores AContext serialized as JSON string.
 * 
 * @property agentId Foreign key to AgentEntity (primary key)
 * @property context JSON serialized AContextEntityData
 */
@Entity(
    tableName = "agent_context_memory",
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["agent_id"], unique = true)]
)
internal data class AgentContextMemoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    
    @ColumnInfo(name = "context")
    val context: String  // JSON serialized AContextEntityData
)
