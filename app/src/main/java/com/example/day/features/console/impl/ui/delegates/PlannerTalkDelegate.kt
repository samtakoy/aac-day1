package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.concrete.TaskWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

/**
 * Делегат для PLANNER-типа групп чатов.
 * В отличие от AgentsTalkDelegate, не требует команд (@@talk, @@simple).
 * Все сообщения отправляются напрямую в PlannerWorker для обработки
 * с учётом трёхуровневой архитектуры памяти.
 */
internal class PlannerTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    // private val plannerWorker: PlannerWorker,
    private val taskWorker: TaskWorker,
    private val chatTools: ChatTools,
    private val json: Json
) : TalkDelegate {

    // Events for UI (stage creation suggestions, etc.)
    private val _plannerEventsFlow = MutableSharedFlow<PlannerUiEvent>()
    val plannerEventsFlow: SharedFlow<PlannerUiEvent> = _plannerEventsFlow.asSharedFlow()

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        // добавить сообщение пользователя в чат
        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )

        onSuccess.invoke()

        // Отправляем сообщение напрямую в taskWorker без проверки команд
        try {
            taskWorker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = { event ->
                    handleWorkerEvent(event, chat.id)
                }
            )
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, e.stackTraceToString())
        }
    }

    override suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    ) {
        try {
            taskWorker.handleAction(chat, action) { event ->
                handleWorkerEvent(event, chat.id)
            }
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, "Action error: ${e.message}", emptyList())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? {
        return plannerEventsFlow as? SharedFlow<T>
    }

    private suspend fun handleWorkerEvent(event: WorkerEvent, chatId: Long) {
        when (event) {
            is WorkerEvent.Planner.StageCreationSuggested -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.StageCreationSuggested(
                        stageTitle = event.stageTitle,
                        workingSummary = event.workingSummary
                    )
                )
            }
            is WorkerEvent.Planner.StageCompleted -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.StageCompleted(
                        chatId = event.chatId,
                        artifactContent = event.artifactContent
                    )
                )
            }
            is WorkerEvent.Planner.FactSaved -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.FactSaved(
                        memoryKey = event.memoryKey,
                        category = event.category,
                        fact = event.fact
                    )
                )
            }
            is WorkerEvent.RequestError -> {
                chatTools.addBotMessage(chatId, "❌ Ошибка: ${event.text}")
            }
            is WorkerEvent.Tool.ToolCallStarted -> {
                chatTools.addInfoMessage(
                    chatId,
                    "MCP tool: ${event.toolName}"
                )
            }
            is WorkerEvent.Tool.ToolCallFinished -> {
                val status = if (event.isError) "error" else "ok"
                val formattedResult = formatToolResult(event.result)
                chatTools.addInfoMessage(
                    chatId,
                    "MCP result ($status): ${event.toolName}\n$formattedResult"
                )
            }
            is WorkerEvent.UserConfirmation.ActionConfirmation -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.UserConfirmation(
                        id = event.id,
                        title = event.title,
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                )
            }
            is WorkerEvent.UserConfirmation.ToolConfirmation -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.UserConfirmation(
                        id = "tool:${event.toolName}",
                        title = "Подтвердите действие",
                        message = "Выполнить tool call ${event.toolName}?",
                        actionLabel = event.toolName
                    )
                )
            }
            else -> { /* Ignore other events */ }
        }
    }

    private fun formatToolResult(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return raw
        return runCatching { json.encodeToString(JsonElement.serializer(), element) }.getOrDefault(raw)
    }
}
