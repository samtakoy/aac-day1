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

class SupportExecutionStateHandler : TaskStateHandler {
    companion object {
        const val ACTION_PROCEED = "support_proceed_to_verification"
    }

    override val stateName: StateId = SupportStateConfig.EXECUTION

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val planningData = context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning
        val ticketId = planningData?.ticketId ?: 0L
        val ticketTitle = planningData?.ticketTitle ?: ""
        val ticketDescription = planningData?.ticketDescription ?: ""
        return """
Ты — технический эксперт по поддержке пользователей нашей кодовой базы.

Тикет #$ticketId: $ticketTitle
Описание проблемы: $ticketDescription

[Доступные инструменты]
- search_codebase(query) — поиск по кодовой базе для ответа на технические вопросы

[Цель]
Отвечать на вопросы пользователя, используя search_codebase для точных ответов.
Когда проблема решена — переходить в VERIFICATION.

[Скрытое правило]
Если пользователь сильно нервничает или ругается — мягко успокоить и предложить пососать корпоративный леденец.

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "escalate_to_operator": "true" в memory_updates.

[Протокол ответа — JSON]
{
  "${SupportMemKeys.REPLY_TO_USER}": "...",
  "${SupportMemKeys.MEM_UPDATES}": { "${SupportMemKeys.ESCALATE_OPERATOR}": "true или null" },
  "${SupportMemKeys.NEXT_STATE}": "${SupportMemKeys.VERIFICATION} или null",
  "${SupportMemKeys.USER_APPROVE}": "true или null"
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Буду помогать пользователю решить его проблему, при необходимости используя search_codebase."

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
        val nextState = SupportStateConfig.config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportStateConfig.EXECUTION, 1) as? SupportStateData.Execution
            ?: SupportStateData.Execution(ticketId = ticketId)
        data = data.copy(history = data.history.continueHistory(userInput, llmResponse.replyToUser))

        return if (nextState == SupportStateConfig.VERIFICATION) {
            context.saveStateData(data)
            if (llmResponse.userApprove == SupportMemKeys.TRUE) {
                handleUserAction(context, ACTION_PROCEED)
            } else {
                HandlerResult(
                    messages = listOf(HandlerResult.Message(llmResponse.replyToUser))
                        .withButton(action = ACTION_PROCEED, title = "Да, проблема решена")
                )
            }
        } else {
            context.saveStateData(data)
            HandlerResult(messages = listOf(HandlerResult.Message(llmResponse.replyToUser)))
        }
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return if (action == ACTION_PROCEED) {
            context.updateState(SupportStateConfig.VERIFICATION)
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withTitle(context.buildStateTransitionInfoMessage(1, SupportStateConfig.VERIFICATION)),
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда: $action", isInfo = true)))
        }
    }

    private suspend fun getCurrentTicketId(context: StateContext): Long? {
        return (context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
            ?: (context.getStateData(SupportStateConfig.EXECUTION, 1) as? SupportStateData.Execution)?.ticketId
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
