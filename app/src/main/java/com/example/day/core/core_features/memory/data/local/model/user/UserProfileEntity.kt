package com.example.day.core.core_features.memory.data.local.model.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity

/**
 * User profile entity - allows users to have multiple profiles with different facts.
 * Each profile is linked to an LTM group for storing facts specific to that profile.
 */
@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_group_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["memory_group_id"])]
)
internal data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "memory_group_id")
    val memoryGroupId: Long,

    @ColumnInfo(name = "text_avatar")
    val textAvatar: Long,
)
