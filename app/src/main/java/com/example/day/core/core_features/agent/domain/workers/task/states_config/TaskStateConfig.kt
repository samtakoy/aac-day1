package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.DoneStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.ExecutionStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.InitStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.PlanningStateHandler
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.VerificationStateHandler
import com.example.day.core.core_features.state_machine.domain.StateConfig
import com.example.day.core.core_features.state_machine.domain.StateInfoProvider
import com.example.day.core.core_features.state_machine.domain.model.StateId

object TaskStateConfig {
    // Standard states constants
    val INIT = StateId("init")
    val PLANNING = StateId("planning")
    val EXECUTION = StateId("execution")
    val VERIFICATION = StateId("verification")
    val DONE = StateId("done")
    val UNKNOWN = StateId("unknown")

    val config = StateConfig(
        states = listOf(INIT, PLANNING, EXECUTION, VERIFICATION, DONE),
        // TODO serializers не используются
        serializers = mapOf(
            INIT to TaskStateData.Init.serializer(),
            PLANNING to TaskStateData.Planning.serializer(),
            EXECUTION to TaskStateData.Execution.serializer(),
            VERIFICATION to TaskStateData.Verification.serializer(),
            DONE to TaskStateData.Done.serializer()
        ),
        // TODO transitions не используются
        transitions = mapOf(
            INIT to listOf(PLANNING),
            PLANNING to listOf(EXECUTION),
            EXECUTION to listOf(VERIFICATION, PLANNING), // retry
            VERIFICATION to listOf(DONE, EXECUTION), // rework
            DONE to emptyList()
        ),
        handlers = mapOf(
            INIT to InitStateHandler(),
            PLANNING to PlanningStateHandler(),
            EXECUTION to ExecutionStateHandler(),
            VERIFICATION to VerificationStateHandler(),
            DONE to DoneStateHandler()
        ),
        initialState = INIT,
        finalStates = listOf(DONE),
        fallbackState = INIT,
        fallbackStateData = TaskStateData.Init(),
        stateInfoProvider = createInfoProvider()
    )

    private fun createInfoProvider(): StateInfoProvider = object : StateInfoProvider {
        override fun getStateDescription(state: StateId?, stepNum: Int, totalSteps: Int): String {
            return when (state) {
                INIT -> "Сбор требований и определение задачи"
                PLANNING -> "Декомпозиция задачи на этапы"
                EXECUTION -> buildString {
                    append("Выполнение этапа $stepNum")
                    if (totalSteps > 0) append(" из $totalSteps")
                }
                VERIFICATION -> buildString {
                    append("Проверка задачи")
                }
                DONE -> "Формирование итогового отчёта"
                else -> "Неизвестно"
            }
        }

    }
}