package com.example.day.core.core_features.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.agent.data.local.model.AgentContextMemoryEntity

/**
 * Data Access Object for Agent Context Memory operations.
 */
@Dao
internal interface AgentContextMemoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contextMemory: AgentContextMemoryEntity)
    
    @Query("SELECT * FROM agent_context_memory WHERE agent_id = :agentId LIMIT 1")
    suspend fun getMemory(agentId: Long): AgentContextMemoryEntity?

    @Query("SELECT context FROM agent_context_memory WHERE agent_id = :agentId LIMIT 1")
    suspend fun getState(agentId: Long): String?

    @Query("SELECT settings FROM agent_context_memory WHERE agent_id = :agentId LIMIT 1")
    suspend fun getParams(agentId: Long): String?

    @Query("UPDATE agent_context_memory SET settings = :settings WHERE agent_id = :agentId")
    suspend fun updateParams(agentId: Long, settings: String)

    @Query("UPDATE agent_context_memory SET context = :context WHERE agent_id = :agentId")
    suspend fun updateState(agentId: Long, context: String)
    
    @Query("DELETE FROM agent_context_memory WHERE agent_id = :agentId")
    suspend fun delete(agentId: Long)
    
    @Query("DELETE FROM agent_context_memory")
    suspend fun deleteAll()
}
