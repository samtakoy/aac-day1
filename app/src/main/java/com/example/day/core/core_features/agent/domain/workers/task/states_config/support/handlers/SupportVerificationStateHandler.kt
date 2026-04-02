package com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportState
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withTitle
import com.example.day.core.core_features.crm.domain.CrmRepository
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId
import dagger.Lazy
import javax.inject.Inject

class SupportVerificationStateHandler @Inject constructor(
    private val stateConfig: Lazy<SupportStateConfig>,
    private val crmRepository: CrmRepository
): TaskStateHandler {
    companion object {
        const val ACTION_DONE = "support_proceed_to_done"
        const val ACTION_RETRY = "support_retry_execution"
    }

    override val stateName: StateId = SupportState.VERIFICATION

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(SupportState.INIT, 1) as? SupportStateData.Init
        val userName = initData?.userName ?: "пользователь"
        val execData = context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution
        val ticketId = execData?.ticketId ?: 0L
        val summary = execData?.summary ?: "отсутствует"
        return """
Ты — ассистент поддержки. Проверь, решена ли проблема пользователя.

Имя пользователя: $userName
Тикет #$ticketId
Краткое резюме по решению от исполнителя: $summary 

[Цель]
1. Спросить пользователя: решена ли проблема
2. Если пользователь подтверждает решена → перейти в ${SupMemKeys.DONE}
3. Если не решена → вернуться в ${SupMemKeys.EXECUTION}

[Правила общения]
Обращайся к пользователю вежливо и по имени.

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "${SupMemKeys.ESCALATE_OPERATOR}": "true" в ${SupMemKeys.MEM_UPDATES}.

[Протокол ответа — JSON]
{
  "${SupMemKeys.REPLY_TO_USER}": "...",
  "${SupMemKeys.MEM_UPDATES}": {
    "${SupMemKeys.SUMMARY}": "краткое резюме решения",
    "${SupMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupMemKeys.NEXT_STATE}": "${SupMemKeys.DONE} или ${SupMemKeys.EXECUTION} или null",
}
Поле ${SupMemKeys.ESCALATE_OPERATOR} помещается внутрь ${SupMemKeys.MEM_UPDATES} как строка "true" (или вовсе опускается).
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

        if (llmResponse.memoryUpdates[SupMemKeys.ESCALATE_OPERATOR] == SupMemKeys.TRUE) {
            val ticketId = getCurrentTicketId(context)
            return handleEscalation(context, llmResponse.replyToUser, ticketId)
        }

        val ticketId = getCurrentTicketId(context)
        val nextState = stateConfig.get().config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        if (ticketId == null) {
            context.saveStateData(SupportStateData.Init())
            return HandlerResult(
                messages = listOf(
                    HandlerResult.Message("Извините, возникли технические неполадки, попробуйте обратиться позже или позвать оператора"))
            )
        }

        var data = context.getStateData(SupportState.VERIFICATION, 1) as? SupportStateData.Verification
            ?: SupportStateData.Verification(ticketId = ticketId)
        data = data.copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        return when (nextState) {
            SupportState.DONE -> {
                context.saveStateData(data)
                HandlerResult(
                    messages = listOf(HandlerResult.Message(llmResponse.replyToUser))
                        .withButton(action = ACTION_DONE, title = "Всё решено!")
                        .withButton(action = ACTION_RETRY, title = "Продолжить общение")
                )
            }
            SupportState.EXECUTION -> {
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
                val ticketId = getCurrentTicketId(context)
                if (ticketId != null) {
                    val result = crmRepository.updateCrmTicket(ticketId, SupMemKeys.CLOSED)
                    if (result.isFailure) {
                        return HandlerResult(listOf(HandlerResult.Message("Извините, возникла ошибка при закрытии тикета")))
                    }
                }
                context.updateState(SupportState.DONE)
                HandlerResult(
                    messages = emptyList<HandlerResult.Message>()
                        .withTitle(context.buildStateTransitionInfoMessage(1, SupportState.DONE)),
                    llmRequest = HandlerResult.LlmRequest()
                )
            }
            ACTION_RETRY -> {
                context.updateState(SupportState.EXECUTION)
                HandlerResult(
                    messages = emptyList<HandlerResult.Message>()
                        .withTitle(context.buildStateTransitionInfoMessage(1, SupportState.EXECUTION)),
                    llmRequest = HandlerResult.LlmRequest()
                )
            }
            else -> HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда: $action", isInfo = true)))
        }
    }

    private suspend fun getCurrentTicketId(context: StateContext): Long? {
        return (context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
            ?: (context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution)?.ticketId
            ?: (context.getStateData(SupportState.VERIFICATION, 1) as? SupportStateData.Verification)?.ticketId
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
        } else {
            return HandlerResult(listOf(HandlerResult.Message("Извините, не удается связаться с оператором")))
        }
        val userPrompt = "Скажи пользователю: 'Передал Ваш вопрос оператору, скоро с вами свяжутся.'"
        return HandlerResult(
            messages = listOf(HandlerResult.Message(replyToUser)),
            llmRequest = HandlerResult.LlmRequest(userPrompt = userPrompt)
        )
    }
}
