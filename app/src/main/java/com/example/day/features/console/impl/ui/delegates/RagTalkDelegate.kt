package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.concrete.RagWorker
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.memory.domain.provider.rag.TestQueries
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Делегат для RAG_CONTEXT-типа групп чатов.
 * Все сообщения отправляются напрямую в [RagWorker]:
 * - агент обогащает промпт контекстом из RAG-сервера
 * - история сжимается через ContextSummaryStrategy (4 сообщения + саммаризация при 6)
 *
 * Специальные команды:
 * - "@@debuginfo" — выводит info-сообщение с текущим состоянием TaskState и Short History
 * - "@@testqueries" — запускает автоматизированный тест по списку TestQueries
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
    ): String? {
        if (inputText.trim() == "@@debuginfo") {
            onSuccess()
            val info = ragWorker.getDebugInfo()
            chatTools.addInfoMessage(chat.id, info)
            return null
        }

        if (inputText.trim() == "@@testqueries") {
            onSuccess()
            handleTestQueries(chat)
            return null
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

        var lastAnswer: String? = null
        try {
            ragWorker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = { event ->
                    if (event is WorkerEvent.RequestSuccess) {
                        lastAnswer = event.result.choices.firstOrNull()?.message?.content
                    }
                }
            )
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
        }
        return lastAnswer
    }

    private suspend fun handleTestQueries(chat: Chat) {
        val total = TestQueries.list.size
        chatTools.addInfoMessage(chat.id, "🔄 Запускаю тест: $total вопросов...")

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<Pair<String, String>>()

        TestQueries.list.forEachIndexed { i, question ->
            val answer = tryAddUserMessage(chat, question, onSuccess = {})
            if (answer == null) {
                chatTools.addInfoMessage(chat.id, "❌ Ошибка на вопросе #${i + 1}. Тест остановлен. Отчёт не отправлен.")
                return
            }
            results.add(question to answer)
        }

        val executionTimeMs = System.currentTimeMillis() - startTime
        ragWorker.saveTestResults(results, executionTimeMs, chat)
            .onSuccess { report ->
                chatTools.addInfoMessage(chat.id, "✅ Тест завершён: ${results.size} вопросов за ${executionTimeMs}мс\nОтчёт: $report")
            }
            .onFailure { error ->
                chatTools.addInfoMessage(chat.id, "⚠️ Тест прошёл, но отчёт не сохранён: ${error.message}")
            }
    }

    override suspend fun tryHandleAction(chat: Chat, messageId: Long, action: String) {
        // RAG-чат не использует action-кнопки
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? = null
}
