package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support.SupportDoneStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support.SupportExecutionStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support.SupportInitStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support.SupportPlanningStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support.SupportVerificationStateHandler
import com.example.day.core.core_features.state_machine.domain.StateConfig
import com.example.day.core.core_features.state_machine.domain.StateInfoProvider
import com.example.day.core.core_features.state_machine.domain.model.StateId

object SupportStateConfig {
    val INIT = StateId("init")
    val PLANNING = StateId("planning")
    val EXECUTION = StateId("execution")
    val VERIFICATION = StateId("verification")
    val DONE = StateId("done")

    val config = StateConfig(
        states = listOf(INIT, PLANNING, EXECUTION, VERIFICATION, DONE),
        serializers = mapOf(
            INIT to SupportStateData.Init.serializer(),
            PLANNING to SupportStateData.Planning.serializer(),
            EXECUTION to SupportStateData.Execution.serializer(),
            VERIFICATION to SupportStateData.Verification.serializer(),
            DONE to SupportStateData.Done.serializer()
        ),
        transitions = mapOf(
            INIT to listOf(PLANNING),
            PLANNING to listOf(EXECUTION),
            EXECUTION to listOf(VERIFICATION, PLANNING),
            VERIFICATION to listOf(DONE, EXECUTION),
            DONE to listOf(INIT)
        ),
        handlers = mapOf(
            INIT to SupportInitStateHandler(),
            PLANNING to SupportPlanningStateHandler(),
            EXECUTION to SupportExecutionStateHandler(),
            VERIFICATION to SupportVerificationStateHandler(),
            DONE to SupportDoneStateHandler()
        ),
        initialState = INIT,
        finalStates = emptyList(),
        fallbackState = INIT,
        fallbackStateData = SupportStateData.Init(),
        stateInfoProvider = createInfoProvider()
    )

    private fun createInfoProvider(): StateInfoProvider = object : StateInfoProvider {
        override fun getStateDescription(state: StateId?, stepNum: Int, totalSteps: Int): String {
            return when (state) {
                INIT -> "Идентификация пользователя"
                PLANNING -> "Определение проблемы и создание тикета"
                EXECUTION -> "Решение проблемы пользователя"
                VERIFICATION -> "Подтверждение решения"
                DONE -> "Завершение обращения"
                else -> "Неизвестно"
            }
        }
    }
}
