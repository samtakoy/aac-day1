package com.example.day.core.core_features.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import com.example.day.core.core_features.agent.data.local.model.relation.AgentWithMemoriesRelation
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
    
    /**
     * Find agent bound to a specific chat by system name.
     * Used when isCommon = false to find chat-specific agent.
     */
    @Query("""
        SELECT a.* FROM agents a
        INNER JOIN agent_to_chat atc ON a.id = atc.agent_id
        WHERE a.system_name = :systemName 
        AND a.is_common = 0 
        AND atc.chat_id = :chatId
        LIMIT 1
    """)
    suspend fun getAgentBySystemNameAndChatId(systemName: String, chatId: Long): AgentWithMemoriesRelation?
}
