package com.example.day.core.core_features.agent.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Название типа памяти пока для простоты это поле dbName из [com.example.day.core.core_features.memory.domain.provider.base.MemoryType]
 * */
@Entity(
    tableName = "agent_to_memory_type",
    primaryKeys = ["agent_id", "memory_type"], // Ключ по обоим полям!
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memory_type")]
)
data class AgentToMemoryTypeEntity(
    @ColumnInfo(name = "agent_id") val agentId: Long,
    @ColumnInfo(name = "memory_type") val memoryType: String // Храним системное имя из Enum
)