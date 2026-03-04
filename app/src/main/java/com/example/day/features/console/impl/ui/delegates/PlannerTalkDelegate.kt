package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.PlannerWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Делегат для PLANNER-типа групп чатов.
 * В отличие от AgentsTalkDelegate, не требует команд (@@talk, @@simple).
 * Все сообщения отправляются напрямую в PlannerWorker для обработки
 * с учётом трёхуровневой архитектуры памяти.
 */
internal class PlannerTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val plannerWorker: PlannerWorker,
    private val chatTools: ChatTools
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
            status = ChatMessageStatus.Viewed
        )

        onSuccess.invoke()

        // Отправляем сообщение напрямую в PlannerWorker без проверки команд
        plannerWorker.doWork(
            userPrompt = inputText,
            chat = chat,
            onEvent = { event ->
                handleWorkerEvent(event, chat.id)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? {
        return plannerEventsFlow as? SharedFlow<T>
    }

    private suspend fun handleWorkerEvent(event: WorkerEvent, chatId: Long) {
        when (event) {
            is WorkerEvent.StageCreationSuggested -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.StageCreationSuggested(
                        stageTitle = event.stageTitle,
                        workingSummary = event.workingSummary
                    )
                )
            }
            is WorkerEvent.StageCompleted -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.StageCompleted(
                        chatId = event.chatId,
                        artifactContent = event.artifactContent
                    )
                )
            }
            is WorkerEvent.FactSaved -> {
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
            else -> { /* Ignore other events */ }
        }
    }
}
