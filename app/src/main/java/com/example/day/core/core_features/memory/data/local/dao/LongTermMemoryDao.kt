package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LongTermMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: LongTermMemoryEntity)

    // Get all facts for a specific LTM group
    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId ORDER BY updated_at DESC")
    fun getByGroup(ltmGroupId: Long): Flow<List<LongTermMemoryEntity>>

    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId ORDER BY updated_at DESC")
    suspend fun getByGroupOnce(ltmGroupId: Long): List<LongTermMemoryEntity>

    @Query("SELECT * FROM long_term_memory WHERE memory_key = :key AND ltm_group_id = :ltmGroupId")
    suspend fun getByKeyAndGroup(key: String, ltmGroupId: Long): LongTermMemoryEntity?

    @Query("SELECT * FROM long_term_memory WHERE id = :id")
    suspend fun getById(id: Long): LongTermMemoryEntity?

    // Delete by id
    @Query("DELETE FROM long_term_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Delete by composite key (ltm_group_id + memory_key + category)
    @Query("DELETE FROM long_term_memory WHERE ltm_group_id = :ltmGroupId AND memory_key = :memoryKey AND category = :category")
    suspend fun deleteByCompositeKey(ltmGroupId: Long, memoryKey: String, category: String)

    @Query("DELETE FROM long_term_memory WHERE ltm_group_id = :ltmGroupId")
    suspend fun clearByGroup(ltmGroupId: Long)

    // Get facts by category
    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId AND category = :category ORDER BY updated_at DESC")
    suspend fun getByGroupAndCategory(ltmGroupId: Long, category: String): List<LongTermMemoryEntity>
}
