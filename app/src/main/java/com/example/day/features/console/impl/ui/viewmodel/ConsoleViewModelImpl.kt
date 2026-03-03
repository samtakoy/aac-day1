package com.example.day.features.console.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreatePlannerStageChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatByIdAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import com.example.day.core.core_features.memory.domain.usecase.GetLongTermMemoryByGroupUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatSettingsUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatTitleUseCase
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel
import com.example.day.core.ui.uikit.chat.bar.model.ChatSendButtonType
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiType
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus
import com.example.day.features.console.impl.ui.components.ChatSettingsUiModel
import com.example.day.features.console.impl.ui.components.LongTermFactItem
import com.example.day.features.console.impl.ui.components.MemoryInspectorUiModel
import com.example.day.features.console.impl.ui.components.ShortTermMemoryItem
import com.example.day.features.console.impl.ui.delegates.AgentsTalkDelegate
import com.example.day.features.console.impl.ui.delegates.LlmTalkDelegate
import com.example.day.features.console.impl.ui.delegates.PlannerTalkDelegate
import com.example.day.features.console.impl.ui.delegates.PlannerUiEvent
import com.example.day.features.console.impl.ui.delegates.TalkDelegate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsoleViewModelImpl(
    private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
    private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
    private val talkDelegate: TalkDelegate,
    private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
    private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    private val updateChatTitleUseCase: UpdateChatTitleUseCase,
    private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
    private val getLtmByGroupUseCase: GetLongTermMemoryByGroupUseCase?,
    private val artifactRepository: ArtifactRepository?,
    private val chatId: Long
) : ViewModel(), ConsoleViewModel {

    private val _state = MutableStateFlow(
        ConsoleViewModel.State(
            chatList = ChatListUiModel(emptyList<ChatMessageUiModel>().toPersistentList()),
            chatBar = ChatBarUiModel(
                inputInitialValue = "",
                buttonType = ChatSendButtonType.ArrowDisabled
            ),
            settings = null
        )
    )

    private var chat: Chat? = null
    private var chatSettings: ChatSettings? = null
    private var currentLtmFacts: List<LongTermMemoryFact> = emptyList()

    private val _expandedStates = MutableStateFlow(persistentMapOf<Long, Boolean>())

    private val _stageCreationState = MutableStateFlow<ConsoleViewModel.StageCreationSuggestion?>(null)

    private val _memoryInspectorState = MutableStateFlow(MemoryInspectorUiModel())

    init {
        // Subscribe to Planner events (stage creation suggestions)
        talkDelegate.getPlannerEvents<PlannerUiEvent>()
            ?.onEach { event ->
                when (event) {
                    is PlannerUiEvent.StageCreationSuggested -> {
                        _stageCreationState.value = ConsoleViewModel.StageCreationSuggestion(
                            stageTitle = event.stageTitle,
                            workingSummary = event.workingSummary
                        )
                    }
                    is PlannerUiEvent.FactSaved -> { /* no-op: LTM flow handles the update */ }
                    is PlannerUiEvent.StageCompleted -> {
                        _state.update { it.copy(isStageCompleted = true) }
                    }
                }
            }
            ?.launchIn(viewModelScope)

        // Subscribe to chat data
        getChatByIdAsFlowUseCase(chatId)
            .onEach { chatData ->
                chat = chatData
                chatData?.settings?.let { settings -> chatSettings = settings }
                refreshMemoryInspector()
            }
            .launchIn(viewModelScope)

        // Subscribe to messages
        getMessagesUseCase(chatId)
            .onEach { messages ->
                val expandedStates = _expandedStates.value
                _state.update { state ->
                    state.copy(
                        chatList = state.chatList.copy(
                            messages = messages.map { msg ->
                                ChatMessageUiModel(
                                    id = msg.id,
                                    text = msg.text,
                                    userType = when (msg.user.type) {
                                        com.example.day.core.core_features.chat.domain.model.UserType.User -> ChatMessageUiType.User
                                        com.example.day.core.core_features.chat.domain.model.UserType.Bot -> ChatMessageUiType.Bot
                                        com.example.day.core.core_features.chat.domain.model.UserType.Info -> ChatMessageUiType.Info
                                    },
                                    status = when (msg.status) {
                                        ChatMessageStatus.Sending -> UiMessageStatus.Sending
                                        ChatMessageStatus.Delivered -> UiMessageStatus.Delivered
                                        ChatMessageStatus.Viewed -> UiMessageStatus.Viewed
                                    },
                                    avatarUrl = msg.user.avatar,
                                    isExpanded = expandedStates[msg.id] ?: false
                                )
                            }.toPersistentList()
                        )
                    )
                }
                refreshMemoryInspector()
            }
            .launchIn(viewModelScope)

        // Subscribe to LTM reactively (only for PLANNER chats where UseCase is provided)
        getLtmByGroupUseCase?.let { ltmUseCase ->
            getChatByIdAsFlowUseCase(chatId)
                .filterNotNull()
                .flatMapLatest { chat -> ltmUseCase(chat.chatGroup.id) }
                .onEach { facts ->
                    currentLtmFacts = facts
                    refreshMemoryInspector()
                }
                .launchIn(viewModelScope)
        }

        // Subscribe to artifacts — marks stage as completed when artifact is saved
        artifactRepository?.getArtifactsForChat(chatId)
            ?.onEach { artifacts ->
                if (artifacts.isNotEmpty()) {
                    _state.update { it.copy(isStageCompleted = true) }
                }
            }
            ?.launchIn(viewModelScope)
    }

    override fun getStateAsFlow(): StateFlow<ConsoleViewModel.State> = _state

    override fun getStageCreationState(): StateFlow<ConsoleViewModel.StageCreationSuggestion?> = _stageCreationState

    override fun getMemoryInspectorState(): StateFlow<MemoryInspectorUiModel> = _memoryInspectorState

    private fun refreshMemoryInspector() {
        val currentChat = chat
        val currentMessages = _state.value.chatList.messages

        val shortTermMessages = currentMessages.map { msg ->
            ShortTermMemoryItem(
                role = if (msg.userType == ChatMessageUiType.User) "user" else "assistant",
                content = msg.text,
                timestamp = msg.id
            )
        }

        val longTermFacts = currentLtmFacts.map { ltm ->
            LongTermFactItem(
                memoryKey = ltm.memoryKey,
                categoryId = ltm.categoryId,
                category = ltm.category,
                fact = ltm.fact,
                updatedAt = ltm.updatedAt
            )
        }

        _memoryInspectorState.update { current ->
            current.copy(
                shortTermMessages = shortTermMessages.toImmutableList(),
                workingMemory = currentChat?.workingSummary,
                longTermFacts = longTermFacts.toImmutableList()
            )
        }
    }

    private fun onInputChanged(text: String) {
        _state.update { state ->
            val prevBar = state.chatBar
            state.copy(
                chatBar = prevBar.copy(
                    inputInitialValue = text,
                    buttonType = resolveButtonType(prevBar, text)
                )
            )
        }
    }

    private fun resolveButtonType(
        prevBar: ChatBarUiModel,
        newText: String
    ): ChatSendButtonType {
        return when {
            prevBar.buttonType == ChatSendButtonType.Loading -> prevBar.buttonType
            newText.isNotEmpty() -> ChatSendButtonType.Arrow
            else -> ChatSendButtonType.ArrowDisabled
        }
    }

    override fun onEvent(event: ConsoleViewModel.Event) {
        when (event) {
            is ConsoleViewModel.Event.InputChanged -> onInputChanged(event.text)
            is ConsoleViewModel.Event.SubmitButtonClick -> {
                sendRequest(_state.value.chatBar.inputInitialValue)
            }
            ConsoleViewModel.Event.OpenSettingsClick -> {
                chatSettings?.let { settings ->
                    _state.update {
                        it.copy(settings = ChatSettingsUiModel("Настройки", chat?.title.orEmpty(), settings))
                    }
                }
            }

            ConsoleViewModel.Event.SettingsCancelClick -> {
                _state.update { it.copy(settings = null) }
            }
            is ConsoleViewModel.Event.SettingsSubmitClick -> {
                chatSettings = event.settings
                _state.update { it.copy(settings = null) }
                viewModelScope.launch {
                    updateChatTitleUseCase(chatId, event.chatTitle)
                    updateChatSettingsUseCase(event.settings)
                }
            }
            is ConsoleViewModel.Event.MessageExpandedChange -> {
                _expandedStates.update { currentStates ->
                    currentStates.toPersistentMap().put(event.messageId, event.isExpanded)
                }
                _state.update { state ->
                    val updatedMessages = state.chatList.messages.map { msg ->
                        if (msg.id == event.messageId) msg.copy(isExpanded = event.isExpanded) else msg
                    }.toPersistentList()
                    state.copy(chatList = state.chatList.copy(messages = updatedMessages))
                }
            }

            ConsoleViewModel.Event.ConfirmStageCreation -> {
                val currentState = _stageCreationState.value ?: return
                val mainChat = chat ?: return
                viewModelScope.launch {
                    try {
                        createPlannerStageChatUseCase(
                            parentChatId = mainChat.id,
                            stageTitle = currentState.stageTitle,
                            workingSummary = currentState.workingSummary
                        )
                    } catch (e: Exception) {
                        // Keep state so user can retry
                        return@launch
                    }
                    _stageCreationState.value = null
                }
            }

            ConsoleViewModel.Event.DeclineStageCreation -> {
                _stageCreationState.value = null
            }

            ConsoleViewModel.Event.OpenMemoryInspector -> {
                refreshMemoryInspector()
            }

            ConsoleViewModel.Event.ToggleMemoryInspector -> { /* tab-based UI — no-op */ }
        }
    }

    private fun sendRequest(inputText: String) {
        val currentChat = chat ?: return
        changeSendBar("", ChatSendButtonType.ArrowDisabled)
        launchCatching(
            onError = {
                restoreSendButton()
                clearUnviewedUseCase.invoke(chatId)
            }
        ) {
            clearUnviewedUseCase.invoke(chatId)
            talkDelegate.tryAddUserMessage(currentChat, inputText) {
                restoreSendButton()
            }
        }
    }

    private fun changeSendBar(text: String, buttonType: ChatSendButtonType) {
        _state.update { state ->
            state.copy(chatBar = state.chatBar.copy(inputInitialValue = text, buttonType = buttonType))
        }
    }

    private fun restoreSendButton() {
        _state.update { state ->
            state.copy(
                chatBar = state.chatBar.copy(
                    buttonType = if (state.chatBar.inputInitialValue.isNotEmpty()) ChatSendButtonType.Arrow else ChatSendButtonType.ArrowDisabled
                )
            )
        }
    }

    private fun launchCatching(
        onError: (suspend (Throwable) -> Unit)? = null,
        onFinally: (suspend () -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError?.invoke(e)
            } finally {
                onFinally?.invoke()
            }
        }
    }

    class Factory @Inject constructor(
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val talkDelegate: LlmTalkDelegate,
        private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
        private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
        private val updateChatTitleUseCase: UpdateChatTitleUseCase,
        private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val id = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                getMessagesUseCase,
                clearUnviewedUseCase,
                talkDelegate,
                getChatByIdAsFlowUseCase,
                updateChatSettingsUseCase,
                updateChatTitleUseCase,
                createPlannerStageChatUseCase,
                getLtmByGroupUseCase = null,
                artifactRepository = null,
                id
            ) as T
        }
    }

    class AgentFactory @Inject constructor(
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val talkDelegate: AgentsTalkDelegate,
        private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
        private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
        private val updateChatTitleUseCase: UpdateChatTitleUseCase,
        private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                getMessagesUseCase,
                clearUnviewedUseCase,
                talkDelegate,
                getChatByIdAsFlowUseCase,
                updateChatSettingsUseCase,
                updateChatTitleUseCase,
                createPlannerStageChatUseCase,
                getLtmByGroupUseCase = null,
                artifactRepository = null,
                chatId = chatId
            ) as T
        }
    }

    class PlannerFactory @Inject constructor(
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val talkDelegate: PlannerTalkDelegate,
        private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
        private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
        private val updateChatTitleUseCase: UpdateChatTitleUseCase,
        private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
        private val getLtmByGroupUseCase: GetLongTermMemoryByGroupUseCase,
        private val artifactRepository: ArtifactRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                getMessagesUseCase,
                clearUnviewedUseCase,
                talkDelegate,
                getChatByIdAsFlowUseCase,
                updateChatSettingsUseCase,
                updateChatTitleUseCase,
                createPlannerStageChatUseCase,
                getLtmByGroupUseCase = getLtmByGroupUseCase,
                artifactRepository = artifactRepository,
                chatId = chatId
            ) as T
        }
    }

    companion object {
        val CHAT_ID_KEY = object : CreationExtras.Key<Long> {}
    }
}
