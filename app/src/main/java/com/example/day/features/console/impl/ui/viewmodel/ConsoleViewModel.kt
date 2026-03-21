package com.example.day.features.console.impl.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel
import com.example.day.features.console.impl.ui.components.ChatSettingsUiModel
import com.example.day.features.console.impl.ui.components.MemoryInspectorUiModel
import kotlinx.coroutines.flow.StateFlow

@Immutable
internal interface ConsoleViewModel {
    fun getStateAsFlow(): StateFlow<State>
    fun getStageCreationState(): StateFlow<StageCreationSuggestion?>
    fun getUserConfirmationState(): StateFlow<UserConfirmationState?>
    fun getMemoryInspectorState(): StateFlow<MemoryInspectorUiModel>
    fun onEvent(event: Event)

    data class State(
        val chatList: ChatListUiModel,
        val chatBar: ChatBarUiModel,
        val settings: ChatSettingsUiModel?,
        val isStageCompleted: Boolean = false
    )

    @Immutable
    data class StageCreationSuggestion(
        val stageTitle: String,
        val workingSummary: String
    )

    @Immutable
    data class UserConfirmationState(
        val id: String,
        val runId: String,
        val title: String,
        val message: String,
        val actionLabel: String
    )

    sealed interface Event {
        object SubmitButtonClick : Event
        class InputChanged(val text: String) : Event
        object OpenSettingsClick : Event
        class SettingsSubmitClick(val chatTitle: String, val settings: ChatSettings) : Event
        object SettingsCancelClick : Event
        class MessageExpandedChange(val messageId: Long, val isExpanded: Boolean) : Event
        object ConfirmStageCreation : Event
        object DeclineStageCreation : Event
        object ConfirmUserConfirmation : Event
        object DeclineUserConfirmation : Event
        object OpenMemoryInspector : Event
        object ToggleMemoryInspector : Event
        class ChatButtonClick(val messageId: Long, val action: String) : Event
    }
}
