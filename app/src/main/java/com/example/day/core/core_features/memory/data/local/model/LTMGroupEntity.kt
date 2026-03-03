package com.example.day.core.core_features.memory.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LTM Group entity - serves as a container for grouping long-term memory facts.
 * Used to decouple memory from specific entities (ChatGroup, UserProfile, etc.).
 * Enables flexible memory allocation across different contexts.
 */
@Entity(tableName = "ltm_groups")
internal data class LTMGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0
)
