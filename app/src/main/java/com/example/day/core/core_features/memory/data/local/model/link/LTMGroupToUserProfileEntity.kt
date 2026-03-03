package com.example.day.core.core_features.memory.data.local.model.link

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity
import com.example.day.core.core_features.memory.data.local.model.user.UserProfileEntity

/**
 * Links an LTM group to a user profile (one-to-one relationship).
 * Each user profile has exactly one LTM group for storing facts.
 * Deleting the profile will cascade delete this link.
 */
@Entity(
    tableName = "ltm_group_to_user_profile",
    primaryKeys = ["ltm_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["ltm_group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ltm_group_id"]),
        Index(value = ["user_profile_id"], unique = true)
    ]
)
internal data class LTMGroupToUserProfileEntity(
    @ColumnInfo(name = "ltm_group_id")
    val ltmGroupId: Long,

    @ColumnInfo(name = "user_profile_id")
    val userProfileId: Long
)
