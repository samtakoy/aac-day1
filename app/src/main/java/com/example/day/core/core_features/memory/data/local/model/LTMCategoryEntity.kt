package com.example.day.core.core_features.memory.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category entity for long-term memory facts.
 * Title is indexed for fast lookup and stored in lowercase.
 */
@Entity(
    tableName = "ltm_categories",
    indices = [Index(value = ["title"], unique = true)]
)
internal data class LTMCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String
)
