package com.example.day.core.core_features.agent.domain.workers.task.states_store

import android.util.Log
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.state_machine.domain.StateConfig
import com.example.day.core.core_features.state_machine.domain.StateData
import com.example.day.core.core_features.state_machine.domain.StateStore
import com.example.day.core.core_features.state_machine.domain.deserialize
import com.example.day.core.core_features.state_machine.domain.model.StateId
import com.example.day.core.core_features.state_machine.domain.serialize
import com.example.day.core.core_features.state_machine.domain.SM_TAG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StateStoreImpl constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
    private val stateConfig: StateConfig
) : StateStore {

    companion object {
        private const val STATE_MEMORY_KEY = "task_state"
        private const val STATE_KEY = "state"
        private const val CUR_STEP_KEY = "step"
        private const val TOTAL_STEPS_KEY = "total_steps"
    }

    // In-memory cache for fast access
    private val keyToFact = mutableMapOf<String, String>()
    private val mutex = Mutex()

    override fun getStateConfig(): StateConfig {
        return stateConfig
    }

    /**
     * TODO из-за отделения id состояния от его data-представления возможны гонки.
     *  Методов [getStateId] и [updateState] и [STATE_KEY] быть не должно, только целостный data-state
     * */
    override suspend fun getStateId(agentId: Long): StateId? {
        return getFromMemory(agentId, STATE_KEY)?.let {
            stateConfig.toValidStateOrNull(it)
        }
    }

    override suspend fun updateState(
        agentId: Long,
        newState: StateId
    ) {
        saveToMemory(agentId = agentId, STATE_KEY, newState.value)
        Log.d(SM_TAG, "[$agentId] updateState → ${newState.value}")
    }

    override suspend fun getStateData(
        agentId: Long,
        stateId: StateId,
        step: Int
    ): StateData? {
        val src = getFromMemory(agentId, getStateDataKey(stateId, step)) ?: return null.also {
            Log.d(SM_TAG, "[$agentId] getStateData(${stateId.value}, step=$step) → null (key=${getStateDataKey(stateId, step)})")
        }
        return stateConfig.deserialize(src).also {
            Log.d(SM_TAG, "[$agentId] getStateData(${stateId.value}, step=$step) → $it")
        }
    }

    override suspend fun updateStateData(
        agentId: Long,
        step: Int,
        data: StateData
    ) {
        saveToMemory(agentId, getStateDataKey(data.state, step), stateConfig.serialize(data))
        Log.d(SM_TAG, "[$agentId] updateStateData(${data.state}, step=$step) → $data")
    }

    override suspend fun getCurrentStage(agentId: Long): Int {
        return getFromMemory(agentId, CUR_STEP_KEY)?.toIntOrNull() ?: 0
    }

    override suspend fun setCurrentStage(agentId: Long, step: Int) {
        saveToMemory(agentId, CUR_STEP_KEY, step.toString())
        Log.d(SM_TAG, "[$agentId] setCurrentStage → $step")
    }

    override suspend fun getTotalStages(agentId: Long): Int {
        return getFromMemory(agentId, TOTAL_STEPS_KEY)?.toIntOrNull() ?: 0
    }

    override suspend fun setTotalStages(agentId: Long, steps: Int) {
        saveToMemory(agentId, TOTAL_STEPS_KEY, steps.toString())
    }

    override suspend fun hasState(agentId: Long): Boolean {
        return getStateId(agentId) != null
    }

    //
    override suspend fun clearMemory(agentId: Long) {
        clearState(agentId)
        // история сообщений агента
        agentContextRepository.clearAgentContext(agentId)
    }

    override suspend fun saveToMemory(agentId: Long, compositeKey: String, value: String) = mutex.withLock {
        agentMemoryRepository.upsertFact(
            agentId = agentId,
            memoryKey = STATE_MEMORY_KEY,
            category = compositeKey,
            fact = value
        )
        keyToFact[cacheKey(agentId, compositeKey)] = value
    }

    override suspend fun getFromMemory(agentId: Long, compositeKey: String): String? = mutex.withLock {
        val key = cacheKey(agentId, compositeKey)
        if (keyToFact.containsKey(key)) {
            return@withLock keyToFact[key]
        }
        return agentMemoryRepository.getFact(
            agentId = agentId,
            memoryKey = STATE_MEMORY_KEY,
            category = compositeKey
        )?.fact?.also {
            keyToFact[key] = it
        }
    }

    override suspend fun clearByKey(agentId: Long, compositeKey: String) {
        mutex.withLock {
            agentMemoryRepository.deleteFact(
                agentId = agentId,
                memoryKey = STATE_MEMORY_KEY,
                category = compositeKey
            )
            keyToFact.remove(cacheKey(agentId, compositeKey))
        }
    }

    private suspend fun clearState(agentId: Long) {
        mutex.withLock {
            agentMemoryRepository.deleteFacts(
                agentId = agentId,
                memoryKey = STATE_MEMORY_KEY
            )
            keyToFact.keys.removeAll { it.startsWith("$agentId:") }
        }
    }

    private fun cacheKey(agentId: Long, compositeKey: String) = "$agentId:$compositeKey"

    private fun getStateDataKey(stateId: StateId, step: Int): String {
        // вообще тут :data тут не обязательно, потому что это все просто id категории
        return "state_${stateId.value}_$step:data"
    }
}