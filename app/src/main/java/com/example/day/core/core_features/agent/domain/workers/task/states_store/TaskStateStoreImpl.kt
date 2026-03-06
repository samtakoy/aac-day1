package com.example.day.core.core_features.agent.domain.workers.task.states_store

import android.util.Log
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.state_machine.domain.StateConfig
import com.example.day.core.core_features.state_machine.domain.StateData
import com.example.day.core.core_features.state_machine.domain.StateStore
import com.example.day.core.core_features.state_machine.domain.deserialize
import com.example.day.core.core_features.state_machine.domain.model.StateId
import com.example.day.core.core_features.state_machine.domain.serialize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskStateStoreImpl @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
    private val stateConfig: StateConfig
) : StateStore {

    companion object {
        private const val STATE_MEMORY_KEY = "task_state"
        private const val STATE_KEY = "state"
        private const val CUR_STEP_KEY = "step"
        private const val TOTAL_STEPS_KEY = "total_steps"
        private const val INIT_DATA_KEY = "init:data"
        private const val PLAN_DATA_KEY = "planning:data"
        private const val EXE_DATA_KEY = "execution:data:"
        private const val VERIF_DATA_KEY = "verification:data"
        private const val DONE_DATA_KEY = "done:data"
        /*
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
            isLenient = true
        }*/
    }

    // In-memory cache for fast access
    private val keyToFact = mutableMapOf<String, String>()
    private val mutex = Mutex()

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
        Log.e("ktor", "updateState to: ${newState.value}")
        Log.e("ktor", "${Exception().stackTraceToString()}")
    }

    override suspend fun getStateData(
        agentId: Long,
        stateId: StateId,
        step: Int
    ): StateData? {
        val src = getFromMemory(agentId, getStateDataKey(stateId, step)) ?: return null
        return stateConfig.deserialize(src)
    }

    override suspend fun updateStateData(
        agentId: Long,
        step: Int,
        data: StateData
    ) {
        saveToMemory(agentId, getStateDataKey(data.state, step), stateConfig.serialize(data))
        Log.e("ktor", "updateStateData to: ${data.state}, getStateDataKey(data.state, step), $data")
    }

    override suspend fun getCurrentStage(agentId: Long): Int {
        return getFromMemory(agentId, CUR_STEP_KEY)?.toIntOrNull() ?: 0
    }

    override suspend fun setCurrentStage(agentId: Long, step: Int) {
        saveToMemory(agentId, CUR_STEP_KEY, step.toString())
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
        keyToFact[compositeKey] = value
    }

    override suspend fun getFromMemory(agentId  : Long, compositeKey: String): String? = mutex.withLock {
        if (keyToFact.containsKey(compositeKey)) {
            return@withLock keyToFact[compositeKey]
        }
        return agentMemoryRepository.getFact(
            agentId = agentId,
            memoryKey = STATE_MEMORY_KEY,
            category = compositeKey
        )?.fact?.also {
            keyToFact[compositeKey] = it
        }
    }

    override suspend fun clearByKey(agentId: Long, compositeKey: String) {
        mutex.withLock {
            agentMemoryRepository.deleteFact(
                agentId = agentId,
                memoryKey = STATE_MEMORY_KEY,
                category = compositeKey
            )
            keyToFact.remove(compositeKey)
        }
    }

    private suspend fun clearState(agentId: Long) {
        mutex.withLock {
            agentMemoryRepository.deleteFacts(
                agentId = agentId,
                memoryKey = STATE_MEMORY_KEY
            )
            keyToFact.clear()
        }
    }

    private fun getStateDataKey(stateId: StateId, step: Int): String {
        return when (stateId) {
            TaskStateConfig.INIT -> INIT_DATA_KEY
            TaskStateConfig.PLANNING -> PLAN_DATA_KEY
            TaskStateConfig.EXECUTION -> EXE_DATA_KEY + step
            TaskStateConfig.VERIFICATION -> VERIF_DATA_KEY
            TaskStateConfig.DONE -> DONE_DATA_KEY
            else -> ""
        }
    }
}