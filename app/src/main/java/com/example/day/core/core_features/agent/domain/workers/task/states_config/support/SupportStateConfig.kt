package com.example.day.core.core_features.agent.domain.workers.task.states_config.support

import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportDoneStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportExecutionStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportInitStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportPlanningStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportVerificationStateHandler
import com.example.day.core.core_features.state_machine.domain.StateConfig
import com.example.day.core.core_features.state_machine.domain.StateInfoProvider
import com.example.day.core.core_features.state_machine.domain.model.StateId
import javax.inject.Inject

class SupportStateConfig @Inject constructor(
    private val initStateHandler: SupportInitStateHandler,
    private val planningStateHandler: SupportPlanningStateHandler,
    private val executionStateHandler: SupportExecutionStateHandler,
    private val verificationStateHandler: SupportVerificationStateHandler,
    private val doneStateHandler: SupportDoneStateHandler
) {
    val config = StateConfig(
        states = listOf(SupportState.INIT, SupportState.PLANNING, SupportState.EXECUTION, SupportState.VERIFICATION, SupportState.DONE),
        serializers = mapOf(
            SupportState.INIT to SupportStateData.Init.serializer(),
            SupportState.PLANNING to SupportStateData.Planning.serializer(),
            SupportState.EXECUTION to SupportStateData.Execution.serializer(),
            SupportState.VERIFICATION to SupportStateData.Verification.serializer(),
            SupportState.DONE to SupportStateData.Done.serializer()
        ),
        transitions = mapOf(
            SupportState.INIT to listOf(SupportState.PLANNING),
            SupportState.PLANNING to listOf(SupportState.EXECUTION),
            SupportState.EXECUTION to listOf(SupportState.VERIFICATION, SupportState.PLANNING),
            SupportState.VERIFICATION to listOf(SupportState.DONE, SupportState.EXECUTION),
            SupportState.DONE to listOf(SupportState.INIT)
        ),
        handlers = mapOf(
            SupportState.INIT to initStateHandler,
            SupportState.PLANNING to planningStateHandler,
            SupportState.EXECUTION to executionStateHandler,
            SupportState.VERIFICATION to verificationStateHandler,
            SupportState.DONE to doneStateHandler
        ),
        initialState = SupportState.INIT,
        finalStates = emptyList(),
        fallbackState = SupportState.INIT,
        fallbackStateData = SupportStateData.Init(),
        stateInfoProvider = createInfoProvider()
    )

    private fun createInfoProvider(): StateInfoProvider = object : StateInfoProvider {
        override fun getStateDescription(state: StateId?, stepNum: Int, totalSteps: Int): String {
            return when (state) {
                SupportState.INIT -> "Идентификация пользователя"
                SupportState.PLANNING -> "Определение проблемы и создание тикета"
                SupportState.EXECUTION -> "Решение проблемы пользователя"
                SupportState.VERIFICATION -> "Подтверждение решения"
                SupportState.DONE -> "Завершение обращения"
                else -> "Неизвестно"
            }
        }
    }
}

object SupportState {
    val INIT = StateId("init")
    val PLANNING = StateId("planning")
    val EXECUTION = StateId("execution")
    val VERIFICATION = StateId("verification")
    val DONE = StateId("done")
}