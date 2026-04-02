package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.concrete.AssistantWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.OrchestratorWorker
import com.example.day.core.core_features.agent.domain.utils.ConsumptionCalculator
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
    private val orchestratorWorker: OrchestratorWorker,
    private val chatTools: ChatTools,
    private val consumptionCalculator: ConsumptionCalculator,
    private val json: Json
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

        val worker: AWorker = if (inputText.trimStart().startsWith(OrchestratorWorker.TASK_PREFIX, ignoreCase = true)) {
            orchestratorWorker
        } else {
            assistantWorker
        }

        var lastAnswer: String? = null
        try {
            worker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = { event ->
                    if (event is WorkerEvent.RequestSuccess) {
                        lastAnswer = event.result.choices.firstOrNull()?.message?.content
                    }
                    handleWorkerEvent(event, chat)
                },
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

    private suspend fun handleWorkerEvent(event: WorkerEvent, chat: Chat) {
        when (event) {
            is WorkerEvent.ToolCallStarted -> {
                chatTools.addInfoMessage(chat.id, "MCP tool: ${event.toolName}, ${event.arguments}, id: ${event.toolCallId}")
            }
            is WorkerEvent.ToolCallFinished -> {
                val status = if (event.isError) "error" else "ok"
                chatTools.addInfoMessage(
                    chat.id,
                    "MCP result ($status): ${event.toolName}\n${formatToolResult(event.result)}"
                )
            }
            is WorkerEvent.RequestSuccess -> {
                consumptionCalculator.onWorkerEvent(chat, event)
            }
            else -> Unit
        }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}
