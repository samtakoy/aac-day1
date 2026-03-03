package com.example.day.core.core_features.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.agent.data.local.model.AgentToMemoryTypeEntity

@Dao
interface AgentMemoryDao {

    /** Добавить конкретный тип памяти агенту */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMemoryTypeToAgent(link: AgentToMemoryTypeEntity)

    /** Удалить конкретный тип памяти у агента */
    @Query("DELETE FROM agent_to_memory_type WHERE agent_id = :agentId AND memory_type = :memoryType")
    suspend fun removeMemoryTypeFromAgent(agentId: Long, memoryType: String)

    /** Полностью очистить типы памяти агента (полезно при полном обновлении) */
    @Query("DELETE FROM agent_to_memory_type WHERE agent_id = :agentId")
    suspend fun clearAgentMemories(agentId: Long)

    /** Получить все типы памяти для конкретного агента */
    @Query("SELECT memory_type FROM agent_to_memory_type WHERE agent_id = :agentId")
    suspend fun getMemoryTypesForAgent(agentId: Long): List<String>
}