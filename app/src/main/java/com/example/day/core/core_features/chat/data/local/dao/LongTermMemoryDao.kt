package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.chat.data.local.model.LongTermMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LongTermMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: LongTermMemoryEntity)

    // Get all facts for a specific group (isolated memory)
    @Query("SELECT * FROM long_term_memory WHERE group_id = :groupId ORDER BY updated_at DESC")
    fun getByGroup(groupId: Long): Flow<List<LongTermMemoryEntity>>

    @Query("SELECT * FROM long_term_memory WHERE group_id = :groupId ORDER BY updated_at DESC")
    suspend fun getByGroupOnce(groupId: Long): List<LongTermMemoryEntity>

    @Query("SELECT * FROM long_term_memory WHERE memory_key = :key AND group_id = :groupId")
    suspend fun getByKeyAndGroup(key: String, groupId: Long): LongTermMemoryEntity?

    @Query("DELETE FROM long_term_memory WHERE memory_key = :key AND group_id = :groupId")
    suspend fun deleteByKeyAndGroup(key: String, groupId: Long)

    @Query("DELETE FROM long_term_memory WHERE group_id = :groupId")
    suspend fun clearByGroup(groupId: Long)
}
