package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

class SupportDoneStateHandler : TaskStateHandler {
    override val stateName: StateId = SupportStateConfig.DONE

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val doneData = context.getStateData(SupportStateConfig.DONE, 1) as? SupportStateData.Done
        val verifData = context.getStateData(SupportStateConfig.VERIFICATION, 1) as? SupportStateData.Verification
        val execData = context.getStateData(SupportStateConfig.EXECUTION, 1) as? SupportStateData.Execution
        val planData = context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning
        val ticketId = doneData?.ticketId ?: verifData?.ticketId ?: execData?.ticketId ?: planData?.ticketId ?: 0L
        val isEscalation = doneData?.isEscalation == true
        val instruction = if (isEscalation) {
            "1. Вызови update_crm_ticket(ticketId=$ticketId, status='operator')\n" +
            "2. Скажи пользователю, что оператор скоро с ним свяжется"
        } else {
            val summary = verifData?.summary ?: ""
            "1. Вызови update_crm_ticket(ticketId=$ticketId, status='closed', result='$summary')\n" +
            "2. Поблагодари пользователя и попрощайся\n" +
            "3. Предложи обращаться снова"
        }
        return """
Ты — ассистент поддержки.

[Доступные инструменты]
- update_crm_ticket(ticketId, status, result) — обновить статус тикета

[Инструкции]
$instruction

[Протокол ответа — JSON]
{
  "${SupportMemKeys.REPLY_TO_USER}": "..."
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

        context.clearTaskMemory()

        return HandlerResult(
            messages = listOf(HandlerResult.Message(llmResponse.replyToUser))
        )
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда", isInfo = true)))
    }
}
