package com.example.day.core.core_features.agent.domain.workers.task.states_config.support.handlers

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.support.SupportState
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.crm.domain.CrmRepository
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId
import android.util.Log
import com.example.day.core.core_features.state_machine.domain.SM_TAG
import dagger.Lazy
import javax.inject.Inject

class SupportInitStateHandler @Inject constructor(
    private val stateConfig: Lazy<SupportStateConfig>,
    private val crmRepository: CrmRepository
) : TaskStateHandler {
    override val stateName: StateId = SupportState.INIT

    override suspend fun buildSystemPrompt(context: StateContext): String = """
Ты — дружелюбный ассистент поддержки пользователей.

[Доступные инструменты]
- get_crm_user_by_chat(${SupMemKeys.CHAT_ID}) — проверить существующего пользователя

[Цель]
1. Поприветствовать пользователя
2. Вызвать get_crm_user_by_chat(${SupMemKeys.CHAT_ID}=${context.chatId}) для проверки наличия пользователя
3. Если пользователь найден — сохранить его имя в ${SupMemKeys.USER_NAME} и перейти в ${SupMemKeys.PLANNING}
4. Если не найден — спросить имя, сохранить его имя в ${SupMemKeys.USER_NAME} и перейти в ${SupMemKeys.PLANNING}

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "${SupMemKeys.ESCALATE_OPERATOR}": "true" в ${SupMemKeys.MEM_UPDATES}.

[Протокол ответа — JSON]
{
  "${SupMemKeys.REPLY_TO_USER}": "...",
  "${SupMemKeys.MEM_UPDATES}": {
    "${SupMemKeys.USER_NAME}": "...",
    "${SupMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupMemKeys.NEXT_STATE}": "${SupMemKeys.PLANNING} или null"
}
Поле ${SupMemKeys.ESCALATE_OPERATOR} помещается внутрь ${SupMemKeys.MEM_UPDATES} как строка "true" (или вовсе опускается).
""".trimIndent()

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Сейчас я поздороваюсь, проверю есть ли пользователь в системе и выясню как к нему обращаться."

    override suspend fun handle(
        context: StateContext,
        userInput: String,
        rawResponse: String
    ): HandlerResult {
        Log.d(SM_TAG, "[INIT] handle: userInput='$userInput'")
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(
                messages = listOf(HandlerResult.Message(
                    "⚠️ Не удалось разобрать ответ ассистента. Попробуйте переформулировать запрос.\n\nRaw: $rawResponse",
                    isInfo = true
                ))
            )
        Log.d(SM_TAG, "[INIT] parsed: reply=${llmResponse.replyToUser}, nextState=${llmResponse.nextState}, memUpd=${llmResponse.memoryUpdates}")

        if (llmResponse.memoryUpdates[SupMemKeys.ESCALATE_OPERATOR] == SupMemKeys.TRUE) {
            return handleEscalation(context, llmResponse.replyToUser, ticketId = null)
        }

        val userName = llmResponse.memoryUpdates[SupMemKeys.USER_NAME]
        var userId = llmResponse.memoryUpdates[SupMemKeys.USER_ID]?.toLongOrNull()
        if (userName != null) {
            val result = crmRepository.createCrmUser(context.chatId, userName)
            userId = result.getOrNull()?.id
            if (result.isFailure || userId == null) {
                return HandlerResult(listOf(HandlerResult.Message("Извините, возникла ошибка при обращении к CRM")))
            }
        }
        val nextState = stateConfig.get().config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportState.INIT, 1) as? SupportStateData.Init
            ?: SupportStateData.Init()
        data = data.copy(
            userId = userId ?: data.userId,
            userName = userName ?: data.userName,
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        val userIdentified = userName?.isNotBlank() == true || userId != null
        Log.d(SM_TAG, "[INIT] userName=$userName, userId=$userId, nextState=${nextState?.value}, userIdentified=$userIdentified")
        return if (nextState == SupportState.PLANNING && userIdentified) {
            context.saveStateData(data)
            context.updateState(SupportState.PLANNING)
            val originalProblem = data.history.firstOrNull { it.role == TaskStateMessage.Role.USER }?.content
            HandlerResult(
                messages = listOf(HandlerResult.Message(llmResponse.replyToUser)),
                llmRequest = HandlerResult.LlmRequest(userPrompt = originalProblem.orEmpty())
            )
        } else {
            context.saveStateData(data)
            HandlerResult(messages = listOf(HandlerResult.Message(llmResponse.replyToUser)))
        }
    }

    override suspend fun handleUserAction(context: StateContext, action: String): HandlerResult {
        return HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда", isInfo = true)))
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
