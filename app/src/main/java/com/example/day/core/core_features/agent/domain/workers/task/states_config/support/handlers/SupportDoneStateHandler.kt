package com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportState
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers.SupportVerificationStateHandler.Companion.ACTION_DONE
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId
import javax.inject.Inject

class SupportDoneStateHandler @Inject constructor(): TaskStateHandler {

    companion object {
        const val ACTION_1 = "1"
        const val ACTION_2 = "2"
        const val ACTION_3 = "3"
        const val ACTION_4 = "4"
        const val ACTION_5 = "5"
    }

    override val stateName: StateId = SupportState.DONE

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(SupportState.INIT, 1) as? SupportStateData.Init
        val userName = initData?.userName ?: "пользователь"
        val doneData = context.getStateData(SupportState.DONE, 1) as? SupportStateData.Done
        // val verifData = context.getStateData(SupportState.VERIFICATION, 1) as? SupportStateData.Verification
        // val execData = context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution
        // val planData = context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning
        val isEscalation = doneData?.isEscalation == true
        val instruction = if (isEscalation) {
            "Скажи пользователю, что оператор скоро с ним свяжется"
        } else {
            "1. Поблагодари пользователя за обращение, предложи обращаться снова и попрощайся\n" +
            "2. Попроси оценить качество обслуживания"
        }
        return """
Ты — ассистент поддержки.

Имя пользователя: $userName

[Правила общения]
Обращайся к пользователю вежливо и по имени.

[Инструкции]
$instruction

[Протокол ответа — JSON]
{
  "${SupMemKeys.REPLY_TO_USER}": "..."
}
Поле next_state не нужно — переход в INIT происходит автоматически.
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Закрою тикет и попрощаюсь с пользователем."

    override suspend fun handle(
        context: StateContext,
        userInput: String,
        rawResponse: String
    ): HandlerResult {
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(
                messages = listOf(HandlerResult.Message(
                    "⚠️ Не удалось разобрать ответ ассистента.\n\nRaw: $rawResponse",
                    isInfo = true
                ))
            )

        return HandlerResult(
            messages = listOf(
                HandlerResult.Message(llmResponse.replyToUser)
            )
                .withButton(action = ACTION_1, title = "1 - плохо")
                .withButton(action = ACTION_2, title = "2")
                .withButton(action = ACTION_3, title = "3")
                .withButton(action = ACTION_4, title = "4")
                .withButton(action = ACTION_5, title = "5 - очень хорошо")
        )
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return when (action) {
            ACTION_1 -> {
                context.clearTaskMemory()
                HandlerResult(messages = listOf(HandlerResult.Message(":(", isInfo = true)))
            }
            ACTION_2 -> {
                context.clearTaskMemory()
                HandlerResult(messages = listOf(HandlerResult.Message(":/", isInfo = true)))
            }
            ACTION_3 -> {
                context.clearTaskMemory()
                HandlerResult(messages = listOf(HandlerResult.Message(":|", isInfo = true)))
            }
            ACTION_4 -> {
                context.clearTaskMemory()
                HandlerResult(messages = listOf(HandlerResult.Message(":)", isInfo = true)))
            }
            ACTION_5 -> {
                context.clearTaskMemory()
                HandlerResult(messages = listOf(HandlerResult.Message(":D", isInfo = true)))
            }
            else -> {
                HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда", isInfo = true)))
            }
        }
    }
}
