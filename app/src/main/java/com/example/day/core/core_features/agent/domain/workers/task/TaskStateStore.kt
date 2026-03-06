package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.MemoryKey

/**
 * Interface for single source of truth state management in TaskWorker.
 * 
 * Provides type-safe state management with:
 * - In-memory caching for fast access
 * - Long-Term Memory (LTM) persistence
 * - Backward compatibility with legacy format
 * 
 * This interface defines the contract for state storage and retrieval,
 * separating the concern of state management from the implementation details.
 */
interface TaskStateStore {
    
    /**
     * Get current state for an agent.
     * First checks in-memory cache, then falls back to LTM.
     * 
     * @param agentId The agent ID to get state for
     * @return The current TaskStateData, or null if no state exists
     */
    suspend fun getState(agentId: String): TaskStateData?
    
    /**
     * Apply a state update and persist atomically.
     * 
     * @param agentId The agent ID to update state for
     * @param update The state update to apply
     * @return The new TaskStateData after applying the update
     */
    suspend fun updateState(agentId: String, update: TaskStateUpdate): TaskStateData
    
    /**
     * Clear all state for an agent (task completion cleanup).
     * 
     * @param agentId The agent ID to clear state for
     */
    suspend fun clearState(agentId: String)
    
    /**
     * Get state from in-memory cache only (no LTM load).
     * Useful for checking current state without I/O.
     * 
     * @param agentId The agent ID to get state for
     * @return The cached TaskStateData, or null if not in cache
     */
    fun getStateInMemory(agentId: String): TaskStateData?
    
    /**
     * Check if state exists for an agent.
     * 
     * @param agentId The agent ID to check
     * @return true if state exists, false otherwise
     */
    suspend fun hasState(agentId: String): Boolean
    
    /**
     * Force reload from LTM, bypassing cache.
     * 
     * @param agentId The agent ID to reload state for
     * @return The reloaded TaskStateData, or null if no state exists
     */
    suspend fun reloadState(agentId: String): TaskStateData?
    
    /**
     * Save memory using MemoryKey (type-safe).
     * This is a convenience method for saving individual state components.
     * 
     * @param agentId The agent ID
     * @param key The type-safe memory key
     * @param value The value to save (will be serialized to JSON)
     */
    suspend fun saveMemory(
        agentId: String,
        key: MemoryKey,
        value: String
    )
    
    /**
     * Get memory using MemoryKey (type-safe).
     * This is a convenience method for retrieving individual state components.
     * 
     * @param agentId The agent ID
     * @param key The type-safe memory key
     * @return The stored value, or null if not found
     */
    suspend fun getMemory(
        agentId: String,
        key: MemoryKey
    ): String?
    
    /**
     * Clear a specific memory key.
     * 
     * @param agentId The agent ID
     * @param key The type-safe memory key to clear
     */
    suspend fun clearMemory(
        agentId: String,
        key: MemoryKey
    )
}
