package com.example.day.core.core_features.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Agent-Chat binding operations.
 */
@Dao
internal interface AgentToChatDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(binding: AgentToChatEntity)
    
    @Delete
    suspend fun delete(binding: AgentToChatEntity)
    
    @Query("DELETE FROM agent_to_chat WHERE agent_id = :agentId AND chat_id = :chatId")
    suspend fun deleteByAgentAndChat(agentId: Long, chatId: Long)
    
    @Query("DELETE FROM agent_to_chat WHERE agent_id = :agentId")
    suspend fun deleteAllChatsForAgent(agentId: Long)
    
    @Query("DELETE FROM agent_to_chat WHERE chat_id = :chatId")
    suspend fun deleteAllAgentsForChat(chatId: Long)
    
    @Query("""
        SELECT chat_id FROM agent_to_chat 
        WHERE agent_id = :agentId
    """)
    fun getChatsForAgent(agentId: Long): Flow<List<Long>>
    
    @Query("""
        SELECT agent_id FROM agent_to_chat 
        WHERE chat_id = :chatId
    """)
    fun getAgentsForChat(chatId: Long): Flow<List<Long>>
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM agent_to_chat 
            WHERE agent_id = :agentId AND chat_id = :chatId
        )
    """)
    suspend fun isAgentBoundToChat(agentId: Long, chatId: Long): Boolean
}
