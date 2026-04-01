package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withTitle
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

class SupportVerificationStateHandler : TaskStateHandler {
    companion object {
        const val ACTION_DONE = "support_proceed_to_done"
        const val ACTION_RETRY = "support_retry_execution"
    }

    override val stateName: StateId = SupportStateConfig.VERIFICATION

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val execData = context.getStateData(SupportStateConfig.EXECUTION, 1) as? SupportStateData.Execution
        val ticketId = execData?.ticketId ?: 0L
        return """
Ты — ассистент поддержки. Проверь, решена ли проблема пользователя.

Тикет #$ticketId

[Цель]
1. Спросить пользователя: решена ли проблема
2. Если решена → перейти в DONE с кратким summary
3. Если не решена → вернуться в EXECUTION

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "escalate_to_operator": "true" в memory_updates.

[Протокол ответа — JSON]
{
  "${SupportMemKeys.REPLY_TO_USER}": "...",
  "${SupportMemKeys.MEM_UPDATES}": {
    "${SupportMemKeys.SUMMARY}": "краткое резюме решения",
    "${SupportMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupportMemKeys.NEXT_STATE}": "${SupportMemKeys.DONE} или ${SupportMemKeys.EXECUTION} или null",
  "${SupportMemKeys.USER_APPROVE}": "true или null"
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Уточню у пользователя — решена ли проблема, с которой он обратился."

    override suspend fun handle(
        context: StateContext,
        userInput: String,
        rawResponse: String
    ): HandlerResult {
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(
                messages = listOf(HandlerResult.Message(
                    "⚠️ Не удалось разобрать ответ ассистента. Попробуйте переформулировать запрос.\n\nRaw: $rawResponse",
                    isInfo = true
                ))
            )

        if (llmResponse.memoryUpdates[SupportMemKeys.ESCALATE_OPERATOR] == SupportMemKeys.TRUE) {
            val ticketId = getCurrentTicketId(context)
            return handleEscalation(context, llmResponse.replyToUser, ticketId)
        }

        val ticketId = getCurrentTicketId(context) ?: 0L
        val summary = llmResponse.memoryUpdates[SupportMemKeys.SUMMARY]
        val nextState = SupportStateConfig.config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportStateConfig.VERIFICATION, 1) as? SupportStateData.Verification
            ?: SupportStateData.Verification(ticketId = ticketId)
        data = data.copy(
            summary = summary ?: data.summary,
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        return when (nextState) {
            SupportStateConfig.DONE -> {
                context.saveStateData(data)
                if (llmResponse.userApprove == SupportMemKeys.TRUE) {
                    handleUserAction(context, ACTION_DONE)
                } else {
                    HandlerResult(
                        messages = listOf(HandlerResult.Message(llmResponse.replyToUser))
                            .withButton(action = ACTION_DONE, title = "Всё решено!")
                    )
                }
            }
            SupportStateConfig.EXECUTION -> {
                context.saveStateData(data)
                handleUserAction(context, ACTION_RETRY)
            }
            else -> {
                context.saveStateData(data)
                HandlerResult(messages = listOf(HandlerResult.Message(llmResponse.replyToUser)))
            }
        }
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return when (action) {
            ACTION_DONE -> {
                context.updateState(SupportStateConfig.DONE)
                HandlerResult(
                    messages = emptyList<HandlerResult.Message>()
                        .withTitle(context.buildStateTransitionInfoMessage(1, SupportStateConfig.DONE)),
                    llmRequest = HandlerResult.LlmRequest()
                )
            }
            ACTION_RETRY -> {
                context.updateState(SupportStateConfig.EXECUTION)
                HandlerResult(
                    messages = emptyList<HandlerResult.Message>()
                        .withTitle(context.buildStateTransitionInfoMessage(1, SupportStateConfig.EXECUTION)),
                    llmRequest = HandlerResult.LlmRequest()
                )
            }
            else -> HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда: $action", isInfo = true)))
        }
    }

    private suspend fun getCurrentTicketId(context: StateContext): Long? {
        return (context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
            ?: (context.getStateData(SupportStateConfig.EXECUTION, 1) as? SupportStateData.Execution)?.ticketId
            ?: (context.getStateData(SupportStateConfig.VERIFICATION, 1) as? SupportStateData.Verification)?.ticketId
    }

    private suspend fun handleEscalation(
        context: StateContext,
        replyToUser: String,
        ticketId: Long?
    ): HandlerResult {
        context.saveStateData(SupportStateData.Done(ticketId = ticketId, isEscalation = true))
        context.updateState(SupportStateConfig.DONE)
        val userPrompt = if (ticketId != null) {
            "Немедленно вызови update_crm_ticket с ticketId=$ticketId и status='operator'.\n" +
            "После вызова инструмента скажи пользователю:\n" +
            "'Окей, я сообщу своим кожанным мешкам о вашем вопросе, и они скоро свяжутся с вами по телефону.'"
        } else {
            "Скажи пользователю: 'Окей, я сообщу своим кожанным мешкам о вашем вопросе, " +
            "и они скоро свяжутся с вами по телефону.'"
        }
        return HandlerResult(
            messages = listOf(HandlerResult.Message(replyToUser)),
            llmRequest = HandlerResult.LlmRequest(userPrompt = userPrompt)
        )
    }
}
