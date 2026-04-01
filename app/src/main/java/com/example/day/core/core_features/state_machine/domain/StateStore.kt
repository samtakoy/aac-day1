package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.state_machine.domain.model.StateId

interface StateStore {
    fun getStateConfig(): StateConfig
    suspend fun getStateId(agentId: Long): StateId?
    suspend fun updateState(agentId: Long, newState: StateId)
    suspend fun getStateData(agentId: Long, stateId: StateId, step: Int): StateData?
    suspend fun updateStateData(agentId: Long, step: Int, data: StateData)
    suspend fun getCurrentStage(agentId: Long): Int
    suspend fun setCurrentStage(agentId: Long, step: Int)
    suspend fun getTotalStages(agentId: Long): Int
    suspend fun setTotalStages(agentId: Long, steps: Int)
    suspend fun hasState(agentId: Long): Boolean
    suspend fun clearMemory(agentId: Long)
    suspend fun saveToMemory(agentId: Long, compositeKey: String, value: String)
    suspend fun getFromMemory(agentId: Long, compositeKey: String): String?
    suspend fun clearByKey(agentId: Long, compositeKey: String)
}
