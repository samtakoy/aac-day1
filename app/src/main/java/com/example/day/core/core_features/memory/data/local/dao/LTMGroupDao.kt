package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity
import com.example.day.core.core_features.memory.data.local.model.link.LTMGroupToAgentEntity
import com.example.day.core.core_features.memory.data.local.model.link.LTMGroupToChatGroupEntity

/**
 * DAO for LTM Group operations and link management.
 */
@Dao
internal interface LTMGroupDao {

    @Insert
    suspend fun insert(group: LTMGroupEntity): Long

    @Query("SELECT * FROM ltm_groups WHERE id = :id")
    suspend fun getById(id: Long): LTMGroupEntity?

    @Query("DELETE FROM ltm_groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- LTMGroup to ChatGroup Link Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatGroupLink(link: LTMGroupToChatGroupEntity)

    @Query("SELECT * FROM ltm_group_to_chat_group WHERE chat_group_id = :chatGroupId")
    suspend fun getLinkByChatGroupId(chatGroupId: Long): LTMGroupToChatGroupEntity?

    @Query("SELECT ltm_group_id FROM ltm_group_to_chat_group WHERE chat_group_id = :chatGroupId")
    suspend fun getLTMGroupIdByChatGroupId(chatGroupId: Long): Long?

    @Query("DELETE FROM ltm_group_to_chat_group WHERE chat_group_id = :chatGroupId")
    suspend fun deleteLinkByChatGroupId(chatGroupId: Long)

    /**
     * Finds existing LTM group for a chat group or creates new one with link.
     * Must be called within a transaction.
     */
    @Transaction
    suspend fun findOrCreateByChatGroup(chatGroupId: Long): Long {
        // Try to find existing link
        val existingId = getLTMGroupIdByChatGroupId(chatGroupId)
        if (existingId != null) {
            return existingId
        }

        // Create new LTM group
        val newGroupId = insert(LTMGroupEntity())

        // Create link
        insertChatGroupLink(
            LTMGroupToChatGroupEntity(
                ltmGroupId = newGroupId,
                chatGroupId = chatGroupId
            )
        )

        return newGroupId
    }

    // --- LTMGroup to Agent Link Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentLink(link: LTMGroupToAgentEntity)

    @Query("SELECT * FROM ltm_group_to_agent WHERE agent_id = :agentId")
    suspend fun getLinkByAgentId(agentId: Long): LTMGroupToAgentEntity?

    @Query("SELECT ltm_group_id FROM ltm_group_to_agent WHERE agent_id = :agentId")
    suspend fun getLTMGroupIdByAgentId(agentId: Long): Long?

    @Query("DELETE FROM ltm_group_to_agent WHERE agent_id = :agentId")
    suspend fun deleteLinkByAgentId(agentId: Long)

    /**
     * Finds existing LTM group for an agent or creates new one with link.
     * Must be called within a transaction.
     */
    @Transaction
    suspend fun findOrCreateByAgent(agentId: Long): Long {
        // Try to find existing link
        val existingId = getLTMGroupIdByAgentId(agentId)
        if (existingId != null) {
            return existingId
        }

        // Create new LTM group
        val newGroupId = insert(LTMGroupEntity())

        // Create link
        insertAgentLink(
            LTMGroupToAgentEntity(
                ltmGroupId = newGroupId,
                agentId = agentId
            )
        )

        return newGroupId
    }
}
