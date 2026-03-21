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
    
    /**
     * Returns the current stage creation suggestion.
     * Null means no dialog should be shown.
     * Non-null means show the stage creation dialog with these details.
     */
    fun getStageCreationState(): StateFlow<StageCreationSuggestion?>
    
    /**
     * Returns the current user confirmation request.
     * Null means no dialog should be shown.
     * Non-null means show the confirmation dialog with these details.
     */
    fun getUserConfirmationState(): StateFlow<UserConfirmationState?>
    
    /**
     * Returns the Memory Inspector UI model showing all three memory layers.
     */
    fun getMemoryInspectorState(): StateFlow<MemoryInspectorUiModel>
    
    fun onEvent(event: Event)

    data class State(
        val chatList: ChatListUiModel,
        val chatBar: ChatBarUiModel,
        val settings: ChatSettingsUiModel?,
        val isStageCompleted: Boolean = false
    )

    /**
     * Data class for stage creation suggestion (PLANNER groups only)
     * Null = hidden, non-null = show dialog
     */
    @Immutable
    data class StageCreationSuggestion(
        val stageTitle: String,
        val workingSummary: String
    )

    /**
     * Data class for user confirmation dialog.
     * Null = hidden, non-null = show confirmation dialog.
     */
    @Immutable
    data class UserConfirmationState(
        val id: String,
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

        // Stage Creation events (PLANNER groups)
        object ConfirmStageCreation : Event
        object DeclineStageCreation : Event

        // User Confirmation events
        object ConfirmUserConfirmation : Event
        object DeclineUserConfirmation : Event

        // Memory Inspector
        object OpenMemoryInspector : Event
        object ToggleMemoryInspector : Event

        // Button click in chat message
        class ChatButtonClick(val messageId: Long, val action: String) : Event
    }
}
