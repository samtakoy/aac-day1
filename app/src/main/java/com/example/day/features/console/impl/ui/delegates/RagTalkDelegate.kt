package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.concrete.RagWorker
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Делегат для RAG_CONTEXT-типа групп чатов.
 * Все сообщения отправляются напрямую в [RagWorker]:
 * - агент обогащает промпт контекстом из RAG-сервера
 * - история сжимается через ContextSummaryStrategy (4 сообщения + саммаризация при 6)
 *
 * Специальная команда:
 * - "debuginfo" — выводит info-сообщение с текущим состоянием TaskState и Short History
 *   (полноценно заработает после Этапа 3, сейчас выводит заглушку)
 */
internal class RagTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val ragWorker: RagWorker,
    private val chatTools: ChatTools,
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        if (inputText.trim().equals("debuginfo", ignoreCase = true)) {
            onSuccess()
            val info = ragWorker.getDebugInfo()
            chatTools.addInfoMessage(chat.id, info)
            return
        }

        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )
        onSuccess()

        try {
            ragWorker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = null
            )
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
        }
    }

    override suspend fun tryHandleAction(chat: Chat, messageId: Long, action: String) {
        // RAG-чат не использует action-кнопки
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? = null
}
