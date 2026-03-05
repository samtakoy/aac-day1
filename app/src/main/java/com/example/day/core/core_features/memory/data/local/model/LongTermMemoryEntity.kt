package com.example.day.core.core_features.memory.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Long-term memory storage for user facts and preferences.
 * Uses auto-generated id as primary key.
 * Uniqueness is enforced by composite unique index (ltm_group_id + memory_key + category).
 * Memory is isolated per LTMGroup, which can be linked to ChatGroup, UserProfile, etc.
 */
@Entity(
    tableName = "long_term_memory",
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["ltm_group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ltm_group_id"]),
        Index(value = ["memory_key"]),
        Index(value = ["category"]),
        Index(value = ["ltm_group_id", "memory_key", "category"], unique = true)
    ]
)
internal data class LongTermMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "memory_key")
    val memoryKey: String,

    @ColumnInfo(name = "ltm_group_id")
    val ltmGroupId: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "fact")
    val fact: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
