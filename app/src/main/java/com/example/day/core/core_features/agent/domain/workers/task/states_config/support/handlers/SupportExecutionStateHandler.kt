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
import android.util.Log
import com.example.day.core.core_features.state_machine.domain.SM_TAG
import dagger.Lazy
import javax.inject.Inject

class SupportExecutionStateHandler @Inject constructor(
    private val stateConfig: Lazy<SupportStateConfig>,
    private val crmRepository: CrmRepository
): TaskStateHandler {
    companion object {
        const val ACTION_PROCEED = "support_proceed_to_verification"
    }

    override val stateName: StateId = SupportState.EXECUTION

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(SupportState.INIT, 1) as? SupportStateData.Init
        val userName = initData?.userName ?: "пользователь"
        val planningData = context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning
        val ticketId = planningData?.ticketId ?: 0L
        val ticketTitle = planningData?.ticketTitle ?: ""
        val ticketDescription = planningData?.ticketDescription ?: ""
        return """
Ты — технический эксперт по поддержке пользователей нашей кодовой базы.

Имя пользователя: $userName
Тикет #$ticketId: $ticketTitle
Описание проблемы: $ticketDescription

[Доступные инструменты]
- get_git_file_list(pattern) — список файлов проекта
- search_codebase(query) — поиск по кодовой базе
- get_file_content(file_path) — содержимое файла

[Порядок работы]
1. Используй search_codebase или get_git_file_list чтобы найти подходящие файлы
2. Для каждого найденного файла вызови get_file_content и прочитай его содержимое
3. Составь ответ на основе прочитанного содержимого
4. Не повторяй один и тот же запрос дважды
Лимит: не более 6 tool-вызовов суммарно.

[Цель]
Отвечать на вопросы пользователя, используя инструменты для точных ответов.
Когда проблема решена — переходить в ${SupMemKeys.VERIFICATION}.

[Запрет]
- НЕЛЬЗЯ называть класс или метод, если ты не прочитал его с помощью инструментов
- Если после прочтения файлов ответа нет — скажи об этом честно
- Не делай более 6 tool-вызовов подряд без ответа пользователю

[Правила общения]
Обращайся к пользователю вежливо и по имени.

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "${SupMemKeys.ESCALATE_OPERATOR}": "true" в ${SupMemKeys.MEM_UPDATES}.
Если ты понимаешь, что невозможно за ограниченное число поисковых запросов решить проблему пользователя - верни "${SupMemKeys.ESCALATE_OPERATOR}": "true" в ${SupMemKeys.MEM_UPDATES}.

[Протокол ответа — JSON]
{
  "${SupMemKeys.REPLY_TO_USER}": "...",
  "${SupMemKeys.MEM_UPDATES}": { "${SupMemKeys.ESCALATE_OPERATOR}": "true или null" },
  "${SupMemKeys.NEXT_STATE}": "${SupMemKeys.VERIFICATION} или null",
  "${SupMemKeys.SUMMARY}": "краткое резюме решения",
}
Поле ${SupMemKeys.ESCALATE_OPERATOR} помещается внутрь ${SupMemKeys.MEM_UPDATES} как строка "true" (или вовсе опускается).
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Буду помогать пользователю решить его проблему, при необходимости используя инструменты поиска по кодовой базе."

    override suspend fun handle(
        context: StateContext,
        userInput: String,
        rawResponse: String
    ): HandlerResult {
        Log.d(SM_TAG, "[EXECUTION] handle: userInput='$userInput'")
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(
                messages = listOf(HandlerResult.Message(
                    "⚠️ Не удалось разобрать ответ ассистента. Попробуйте переформулировать запрос.\n\nRaw: $rawResponse",
                    isInfo = true
                ))
            )
        Log.d(SM_TAG, "[EXECUTION] parsed: reply='${llmResponse.replyToUser}', nextState=${llmResponse.nextState}, summary=${llmResponse.memoryUpdates[SupMemKeys.SUMMARY]}, memUpd=${llmResponse.memoryUpdates}")

        if (llmResponse.memoryUpdates[SupMemKeys.ESCALATE_OPERATOR] == SupMemKeys.TRUE) {
            val ticketId = getCurrentTicketId(context)
            return handleEscalation(context, llmResponse.replyToUser, ticketId)
        }

        val ticketId = getCurrentTicketId(context) ?: 0L
        val nextState = stateConfig.get().config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution
            ?: SupportStateData.Execution(ticketId = ticketId)
        data = data.copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser),
            summary = llmResponse.memoryUpdates[SupMemKeys.SUMMARY]
        )

        Log.d(SM_TAG, "[EXECUTION] nextState=${nextState?.value}, replyEmpty=${llmResponse.replyToUser.isBlank()}")
        return if (nextState == SupportState.VERIFICATION) {
            context.saveStateData(data)
            if (llmResponse.userApprove == SupMemKeys.TRUE) {
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
            context.updateState(SupportState.VERIFICATION)
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withTitle(context.buildStateTransitionInfoMessage(1, SupportState.VERIFICATION)),
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда: $action", isInfo = true)))
        }
    }

    private suspend fun getCurrentTicketId(context: StateContext): Long? {
        return (context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
            ?: (context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution)?.ticketId
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
            return HandlerResult(listOf(HandlerResult.Message("Извините, что-то пошло не так")))
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
