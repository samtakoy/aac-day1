package com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportState
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.crm.domain.CrmRepository
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId
import dagger.Lazy
import javax.inject.Inject

class SupportPlanningStateHandler @Inject constructor(
    private val stateConfig: Lazy<SupportStateConfig>,
    private val crmRepository: CrmRepository
): TaskStateHandler {
    override val stateName: StateId = SupportState.PLANNING

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(SupportState.INIT, 1) as? SupportStateData.Init
        val userName = initData?.userName ?: "пользователь"
        return """
Ты — ассистент поддержки пользователей. Твоя задача — определить проблему с которой пришел пользователь и создать тикет.

Данные пользователя:
- Имя: $userName
- ${SupMemKeys.CHAT_ID}: ${context.chatId}

[Доступные инструменты]
- get_crm_user_tickets(${SupMemKeys.CHAT_ID}) — получить открытые тикеты пользователя

[Цель]
1. Выяснить с какой проблемой пришёл пользователь
2. Вызвать get_crm_user_tickets для проверки существующих тикетов
3. Если есть открытый тикет по этой проблеме — предложить продолжить его
4. Если пользователь хочет продолжить разговор по уже открытому тикету перейти в ${SupMemKeys.EXECUTION} с полученным ${SupMemKeys.TICKET_ID}
5. Если тикета по проблеме не заведено, перейти в ${SupMemKeys.EXECUTION} с заполненными "${SupMemKeys.TICKET_TITLE}" и "${SupMemKeys.TICKET_DESCRIPTION}"

[Правила общения]
Обращайся к пользователю вежливо и по имени.

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "${SupMemKeys.ESCALATE_OPERATOR}": "true" в ${SupMemKeys.MEM_UPDATES}.

[Протокол ответа — JSON]
{
  "${SupMemKeys.REPLY_TO_USER}": "...",
  "${SupMemKeys.MEM_UPDATES}": {
    "${SupMemKeys.TICKET_ID}": "id тикета или null",
    "${SupMemKeys.TICKET_TITLE}": "...",
    "${SupMemKeys.TICKET_DESCRIPTION}": "...",
    "${SupMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupMemKeys.NEXT_STATE}": "${SupMemKeys.EXECUTION} или null"
}
Поле ${SupMemKeys.ESCALATE_OPERATOR} помещается внутрь ${SupMemKeys.MEM_UPDATES} как строка "true" (или вовсе опускается).
Поле ${SupMemKeys.TICKET_ID} заполняй только если пользователь хочет продолжить уже открытый тикет (id из инструмента).
Поля ${SupMemKeys.TICKET_TITLE} и ${SupMemKeys.TICKET_DESCRIPTION} заполняй при создании нового тикета.
Правило для ${SupMemKeys.NEXT_STATE}: установи "${SupMemKeys.EXECUTION}" если заполнены ticket_title+ticket_description ИЛИ ticket_id. Иначе null.
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

        if (llmResponse.memoryUpdates[SupMemKeys.ESCALATE_OPERATOR] == SupMemKeys.TRUE) {
            val ticketId = getCurrentTicketId(context)
            return handleEscalation(context, llmResponse.replyToUser, ticketId)
        }

        val ticketId = llmResponse.memoryUpdates[SupMemKeys.TICKET_ID]?.toLongOrNull()
        val ticketTitle = llmResponse.memoryUpdates[SupMemKeys.TICKET_TITLE]
        val ticketDescription = llmResponse.memoryUpdates[SupMemKeys.TICKET_DESCRIPTION]
        val nextState = stateConfig.get().config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )
        var resolvedTicketId = ticketId
        val isDataValid = if (ticketId != null) {
            val checkResult = crmRepository.checkOpenedTicket(context.chatId, ticketId)
            checkResult.getOrDefault(false)
        } else if (ticketTitle?.isNotBlank() == true && ticketDescription?.isNotBlank() == true) {
            val createResult = crmRepository.createCrmTicket(context.chatId, ticketTitle, ticketDescription)
            resolvedTicketId = createResult.getOrNull()?.id
            if (createResult.isFailure || resolvedTicketId == null) {
                return HandlerResult(listOf(HandlerResult.Message("Извините, возникла ошибка при обращении к CRM")))
            }
            true
        } else {
            false
        }

        var data = context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning
            ?: SupportStateData.Planning()
        data = data.copy(
            ticketId = resolvedTicketId ?: data.ticketId,
            ticketTitle = ticketTitle ?: data.ticketTitle,
            ticketDescription = ticketDescription ?: data.ticketDescription,
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        return if (nextState == SupportState.EXECUTION && isDataValid) {
            context.saveStateData(data)
            context.updateState(SupportState.EXECUTION)
            val originalProblem = data.ticketDescription ?: userInput
            HandlerResult(
                messages = listOf(HandlerResult.Message(llmResponse.replyToUser)),
                llmRequest = HandlerResult.LlmRequest(userPrompt = originalProblem)
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
        return (context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
    }

    private suspend fun handleEscalation(
        context: StateContext,
        replyToUser: String,
        ticketId: Long?
    ): HandlerResult {
        context.saveStateData(SupportStateData.Done(ticketId = ticketId, isEscalation = true))
        context.updateState(SupportState.DONE)
        if (ticketId != null) {
            val result = crmRepository.updateCrmTicket(ticketId, SupMemKeys.OPERATOR)
            if (result.isFailure) {
                return HandlerResult(listOf(HandlerResult.Message("Извините, не удается связаться с оператором")))
            }
        }
        return HandlerResult(
            messages = listOf(
                HandlerResult.Message(replyToUser),
                HandlerResult.Message("Передал Ваш вопрос оператору, скоро с вами свяжутся.")
            )
                .withButton(action = SupportDoneStateHandler.ACTION_1, title = "1 - плохо")
                .withButton(action = SupportDoneStateHandler.ACTION_2, title = "2")
                .withButton(action = SupportDoneStateHandler.ACTION_3, title = "3")
                .withButton(action = SupportDoneStateHandler.ACTION_4, title = "4")
                .withButton(action = SupportDoneStateHandler.ACTION_5, title = "5 - очень хорошо")
        )
    }
}
