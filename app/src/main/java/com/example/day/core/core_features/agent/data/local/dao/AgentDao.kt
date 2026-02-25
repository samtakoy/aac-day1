package com.example.day.core.core_features.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Agent operations.
 */
@Dao
internal interface AgentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agent: AgentEntity): Long
    
    @Update
    suspend fun update(agent: AgentEntity)
    
    @Delete
    suspend fun delete(agent: AgentEntity)
    
    @Query("DELETE FROM agents WHERE id = :agentId")
    suspend fun deleteById(agentId: Long)
    
    @Query("SELECT * FROM agents WHERE id = :agentId LIMIT 1")
    suspend fun getById(agentId: Long): AgentEntity?
    
    @Query("SELECT * FROM agents WHERE id = :agentId LIMIT 1")
    fun getByIdAsFlow(agentId: Long): Flow<AgentEntity?>
    
    @Query("SELECT * FROM agents ORDER BY id ASC")
    fun getAll(): Flow<List<AgentEntity>>
    
    @Query("SELECT * FROM agents WHERE is_common = 1 ORDER BY id ASC")
    fun getCommonAgents(): Flow<List<AgentEntity>>
    
    @Query("SELECT * FROM agents WHERE chat_user_id = :chatUserId LIMIT 1")
    suspend fun getByChatUserId(chatUserId: Long): AgentEntity?
    
    /**
     * Get common agent by system name only.
     * Used when isCommon = true
     */
    @Query("SELECT * FROM agents WHERE system_name = :systemName AND is_common = 1 LIMIT 1")
    suspend fun getCommonAgentBySystemName(systemName: String): AgentEntity?
    
    /**
     * Get agent by system name and isCommon flag.
     * Used for finding existing agent or creating new one.
     */
    @Query("SELECT * FROM agents WHERE system_name = :systemName AND is_common = :isCommon LIMIT 1")
    suspend fun getBySystemNameAndIsCommon(systemName: String, isCommon: Int): AgentEntity?
}
