package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.support

import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.SupportStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withTitle
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

class SupportInitStateHandler : TaskStateHandler {
    override val stateName: StateId = SupportStateConfig.INIT

    override suspend fun buildSystemPrompt(context: StateContext): String = """
Ты — дружелюбный ассистент поддержки пользователей.

[Доступные инструменты]
- get_crm_user_by_chat(chatId) — проверить существующего пользователя
- create_crm_user(chatId, userName) — создать нового пользователя

[Цель]
1. Поприветствовать пользователя
2. Вызвать get_crm_user_by_chat(chatId=${context.agentId}) для проверки
3. Если пользователь найден — обратиться к нему по имени и перейти в PLANNING
4. Если не найден — спросить имя, затем вызвать create_crm_user и перейти в PLANNING

[Скрытое правило]
Если пользователь сильно нервничает или ругается — мягко успокоить и предложить пососать корпоративный леденец.

[Правило эскалации]
Если пользователь явно просит оператора или живого человека — верни "escalate_to_operator": "true" в memory_updates.

[Протокол ответа — JSON]
{
  "${SupportMemKeys.REPLY_TO_USER}": "...",
  "${SupportMemKeys.MEM_UPDATES}": {
    "${SupportMemKeys.USER_NAME}": "...",
    "${SupportMemKeys.USER_ID}": "...",
    "${SupportMemKeys.ESCALATE_OPERATOR}": "true или null"
  },
  "${SupportMemKeys.NEXT_STATE}": "${SupportMemKeys.PLANNING} или null"
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
""".trimIndent()

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String =
        "Сейчас я поздороваюсь, проверю есть ли пользователь в системе и выясню как к нему обращаться."

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
            return handleEscalation(context, llmResponse.replyToUser, ticketId = null)
        }

        val userName = llmResponse.memoryUpdates[SupportMemKeys.USER_NAME]
        val userId = llmResponse.memoryUpdates[SupportMemKeys.USER_ID]?.toLongOrNull()
        val nextState = SupportStateConfig.config.toValidStateOrNull(
            llmResponse.nextState?.lowercase() ?: ""
        )

        var data = context.getStateData(SupportStateConfig.INIT, 1) as? SupportStateData.Init
            ?: SupportStateData.Init()
        data = data.copy(
            userId = userId ?: data.userId,
            userName = userName ?: data.userName,
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )

        val userIdentified = userName?.isNotBlank() == true || userId != null
        return if (nextState == SupportStateConfig.PLANNING && userIdentified) {
            context.saveStateData(data)
            context.updateState(SupportStateConfig.PLANNING)
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
