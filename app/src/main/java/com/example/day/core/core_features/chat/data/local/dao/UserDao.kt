package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface UserDao {
    @Query("SELECT * FROM users WHERE type = :type LIMIT 1")
    suspend fun getUserByType(type: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}
