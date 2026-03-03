package com.example.day.core.core_features.memory.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Long-term memory storage for user facts and preferences.
 * Uses composite key (memoryKey + ltmGroupId) for proper UPSERT operations.
 * Memory is isolated per LTMGroup, which can be linked to ChatGroup, UserProfile, etc.
 */
@Entity(
    tableName = "long_term_memory",
    primaryKeys = ["memory_key", "category_id", "ltm_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["ltm_group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LTMCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["ltm_group_id"]),
        Index(value = ["category_id"])
    ]
)
internal data class LongTermMemoryEntity(
    @ColumnInfo(name = "memory_key")
    val memoryKey: String,

    @ColumnInfo(name = "ltm_group_id")
    val ltmGroupId: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "fact")
    val fact: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
