package com.example.day.core.core_features.chat.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Long-term memory storage for user facts and preferences.
 * Uses composite key (memoryKey + groupId) for proper UPSERT operations.
 * Memory is isolated per ChatGroup.
 */
@Entity(
    tableName = "long_term_memory",
    primaryKeys = ["memory_key", "group_id"],
    indices = [Index("group_id")]
)
internal data class LongTermMemoryEntity(
    @ColumnInfo(name = "memory_key")
    val memoryKey: String,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "category")
    val category: String = "",

    @ColumnInfo(name = "fact")
    val fact: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
