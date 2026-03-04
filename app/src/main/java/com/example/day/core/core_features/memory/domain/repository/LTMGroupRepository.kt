package com.example.day.core.core_features.memory.domain.repository

import com.example.day.core.core_features.memory.domain.model.LTMGroup

/**
 * Repository interface for LTM Group operations.
 */
interface LTMGroupRepository {

    /**
     * Creates a new LTM group and returns its ID.
     */
    suspend fun createGroup(): Long

    /**
     * Gets LTM group by ID.
     */
    suspend fun getById(id: Long): LTMGroup?

    /**
     * Deletes LTM group by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Finds existing LTM group ID for a chat group or creates new one with link.
     * This is the main entry point for getting LTM group ID from chat group ID.
     *
     * @param chatGroupId Chat group ID
     * @return LTM group ID (existing or newly created)
     */
    suspend fun findOrCreateByChatGroup(chatGroupId: Long): Long

    /**
     * Gets LTM group ID by chat group ID.
     * Returns null if no link exists.
     */
    suspend fun getLTMGroupIdByChatGroupId(chatGroupId: Long): Long?

    /**
     * Deletes the link between LTM group and chat group.
     * Note: This doesn't delete the LTM group itself, just the link.
     */
    suspend fun deleteLinkByChatGroupId(chatGroupId: Long)

    /**
     * Finds existing LTM group ID for an agent or creates new one with link.
     * This is the main entry point for getting LTM group ID from agent ID.
     *
     * @param agentId Agent ID
     * @return LTM group ID (existing or newly created)
     */
    suspend fun findOrCreateByAgent(agentId: Long): Long

    /**
     * Gets LTM group ID by agent ID.
     * Returns null if no link exists.
     */
    suspend fun getLTMGroupIdByAgentId(agentId: Long): Long?

    /**
     * Deletes the link between LTM group and agent.
     * Note: This doesn't delete the LTM group itself, just the link.
     */
    suspend fun deleteLinkByAgentId(agentId: Long)
}
