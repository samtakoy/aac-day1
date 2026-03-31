package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.concrete.AssistantWorker
import com.example.day.core.core_features.agent.domain.utils.ConsumptionCalculator
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Делегат для ASSISTANT-типа групп чатов.
 * Обрабатывает сообщения через [AssistantWorker]:
 * - Обычные сообщения → агент с MCP-инструментами (git, файлы, github)
 * - @@help <вопрос> → агент с MCP + RAG-контекст из кодовой базы
 */
internal class AssistantTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val assistantWorker: AssistantWorker,
    private val chatTools: ChatTools,
    private val consumptionCalculator: ConsumptionCalculator,
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ): String? {
        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )
        onSuccess()

        var lastAnswer: String? = null
        try {
            assistantWorker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = { event ->
                    if (event is WorkerEvent.RequestSuccess) {
                        lastAnswer = event.result.choices.firstOrNull()?.message?.content
                    }
                    consumptionCalculator.onWorkerEvent(chat, event)
                }
            )
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
        }
        return lastAnswer
    }

    override suspend fun tryHandleAction(chat: Chat, messageId: Long, action: String) {
        // ASSISTANT-чат не использует action-кнопки
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? = null
}
