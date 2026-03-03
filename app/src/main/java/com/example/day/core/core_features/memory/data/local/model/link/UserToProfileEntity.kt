package com.example.day.core.core_features.memory.data.local.model.link

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.memory.data.local.model.user.UserProfileEntity

/**
 * Links a user to their currently selected profile.
 * If no record exists for a user, they work without a profile.
 * One-to-one relationship: one user can have only one active profile at a time.
 */
@Entity(
    tableName = "user_to_profile",
    primaryKeys = ["user_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["profile_id"])
    ]
)
internal data class UserToProfileEntity(
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "profile_id")
    val profileId: Long
)
