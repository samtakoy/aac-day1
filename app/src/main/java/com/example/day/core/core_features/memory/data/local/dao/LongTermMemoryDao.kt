package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.day.core.core_features.memory.data.local.dao.relation.LongTermMemoryWithCategoryRelation
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LongTermMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: LongTermMemoryEntity)

    // Get all facts for a specific LTM group
    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId ORDER BY updated_at DESC")
    fun getByGroup(ltmGroupId: Long): Flow<List<LongTermMemoryWithCategoryRelation>>

    @Transaction // Важно! Room выполнит два запроса, транзакция гарантирует целостность
    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId ORDER BY updated_at DESC")
    suspend fun getByGroupOnce(ltmGroupId: Long): List<LongTermMemoryWithCategoryRelation>

    @Query("SELECT * FROM long_term_memory WHERE memory_key = :key AND ltm_group_id = :ltmGroupId")
    suspend fun getByKeyAndGroup(key: String, ltmGroupId: Long): LongTermMemoryWithCategoryRelation?

    @Query("DELETE FROM long_term_memory WHERE memory_key = :key AND ltm_group_id = :ltmGroupId")
    suspend fun deleteByKeyAndGroup(key: String, ltmGroupId: Long)

    @Query("DELETE FROM long_term_memory WHERE ltm_group_id = :ltmGroupId")
    suspend fun clearByGroup(ltmGroupId: Long)

    // Get facts by category
    @Query("SELECT * FROM long_term_memory WHERE ltm_group_id = :ltmGroupId AND category_id = :categoryId ORDER BY updated_at DESC")
    suspend fun getByGroupAndCategory(ltmGroupId: Long, categoryId: Long): List<LongTermMemoryEntity>
}
