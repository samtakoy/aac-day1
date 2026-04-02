package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * Context for task state machine execution.
 * Holds all necessary data for current task state.
 */
data class StateContext(
    val chatId: Long,
    val agentId: Long,
    val store: StateStore
) {
    suspend fun getState(): StateId? = store.getStateId(agentId)
    suspend fun updateState(state: StateId) = store.updateState(agentId, state)

    // Planning artifact helpers
    suspend fun getCurStepNum(): Int = store.getCurrentStage(agentId)
    suspend fun getCurStepIdx(): Int = store.getCurrentStage(agentId) - 1
    suspend fun setCurStepNum(step: Int) = store.setCurrentStage(agentId, step)
    suspend fun getTotalSteps(): Int = store.getTotalStages(agentId)
    suspend fun setTotalSteps(steps: Int) = store.setTotalStages(agentId, steps)

    // TODO - этот дефолтный параметр - источник багов - убрать (и вообще откзаться от него как и от отдельной переменной стейта)
    suspend fun saveStateData(data: StateData, step: Int = 1) {
        store.updateStateData(agentId, step, data)
    }

    suspend fun getStateData(state: StateId, step: Int): StateData? {
        return store.getStateData(agentId, state, step)
    }

    suspend fun clearTaskMemory() = store.clearMemory(agentId)

    suspend fun getStateDescription(): String {
        return store.getStateConfig().stateInfoProvider.getStateDescription(
            state = getState(),
            stepNum = getCurStepNum(),
            totalSteps = getTotalSteps()
        )
    }
}