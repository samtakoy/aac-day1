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

class SupportPlanningStateHandler : TaskStateHandler {
    override val stateName: StateId = SupportStateConfig.PLANNING

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(SupportStateConfig.INIT, 1) as? SupportStateData.Init
        val userName = initData?.userName ?: "пользователь"
        return """
Ты — ассистент поддержки пользователей. Твоя задача — определить проблему и создать тикет.

Данные пользователя:
- Имя: $userName
- chatId: ${context.agentId}

[Доступные инструменты]
- get_crm_user_tickets(chatId) — получить открытые тикеты пользователя
- create_crm_ticket(chatId, title, description) — создать новый тикет

[Цель]
1. Выяснить с какой проблемой пришёл пользователь
2. Вызвать get_crm_user_tickets для проверки существующих тикетов
3. Если есть открытый тикет по этой проблеме — предложить продолжить его
4. Если нет — вызвать create_crm_ticket и получить ticket_id
5. Перейти в EXECUTION с полученным ticket_id

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "escalate_to_operator": "true" в memory_updates.

[Протокол ответа — JSON]
{
  "${SupportMemKeys.REPLY_TO_USER}": "...",
  "${SupportMemKeys.MEM_UPDATES}": {
    "${SupportMemKeys.TICKET_ID}": "123",
    "${SupportMemKeys.TICKET_TITLE}": "...",
    "${SupportMemKeys.TICKET_DESCRIPTION}": "...",
    "${SupportMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupportMemKeys.NEXT_STATE}": "${SupportMemKeys.EXECUTION} или null"
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Проверю открытые тикеты пользователя и определю проблему с которой он пришёл."

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

        val ticketId = llmResponse.memoryUpdates[SupportMemKeys.TICKET_ID]?.toLongOrNull()
        val ticketTitle = llmResponse.memoryUpdates[SupportMemKeys.TICKET_TITLE]
        val ticketDescription = llmResponse.memoryUpdates[SupportMemKeys.TICKET_DESCRIPTION]
        val nextState = SupportStateConfig.config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning
            ?: SupportStateData.Planning()
        data = data.copy(
            ticketId = ticketId ?: data.ticketId,
            ticketTitle = ticketTitle ?: data.ticketTitle,
            ticketDescription = ticketDescription ?: data.ticketDescription,
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        return if (nextState == SupportStateConfig.EXECUTION && ticketId != null) {
            context.saveStateData(data)
            context.updateState(SupportStateConfig.EXECUTION)
            HandlerResult(
                messages = listOf(HandlerResult.Message(llmResponse.replyToUser)),
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            context.saveStateData(data)
            HandlerResult(messages = listOf(HandlerResult.Message(llmResponse.replyToUser)))
        }
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда", isInfo = true)))
    }

    private suspend fun getCurrentTicketId(context: StateContext): Long? {
        return (context.getStateData(SupportStateConfig.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
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
