package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.memory.data.local.model.link.UserToProfileEntity
import com.example.day.core.core_features.memory.data.local.model.user.UserProfileEntity

@Dao
internal interface UserProfileDao {

    // ─── Profile CRUD ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(entity: UserProfileEntity): Long

    @Query("SELECT * FROM user_profiles WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserProfileEntity?

    @Query("DELETE FROM user_profiles WHERE title = :title")
    suspend fun deleteByTitle(title: String)

    @Query("UPDATE user_profiles SET text_avatar = :avatar WHERE id = :id")
    suspend fun updateTextAvatar(id: Long, avatar: String?)

    @Query("SELECT * FROM user_profiles ORDER BY title ASC")
    suspend fun getAllProfiles(): List<UserProfileEntity>

    // ─── UserToProfile binding ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserToProfile(link: UserToProfileEntity)

    @Query("DELETE FROM user_to_profile WHERE user_id = :userId")
    suspend fun deleteUserToProfile(userId: Long)

    @Query(
        """
        SELECT up.* FROM user_profiles up
        INNER JOIN user_to_profile utp ON utp.profile_id = up.id
        WHERE utp.user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getProfileForUser(userId: Long): UserProfileEntity?
}
