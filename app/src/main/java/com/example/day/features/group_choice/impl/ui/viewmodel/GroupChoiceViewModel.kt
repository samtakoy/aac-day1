package com.example.day.features.group_choice.impl.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.ui.uikit.dialogs.group.GroupEditDialogState
import kotlinx.coroutines.flow.StateFlow

@Immutable
interface GroupChoiceViewModel {
    fun getStateAsFlow(): StateFlow<State>
    fun onEvent(event: Event)

    sealed interface Event {
        object AddGroupClick : Event
        class GroupClick(val group: ChatGroup) : Event
        class EditGroupClick(val group: ChatGroup) : Event
        class DeleteGroupClick(val group: ChatGroup) : Event
        class DialogTitleChanged(val title: String) : Event
        class DialogTypeSelected(val type: ChatType) : Event
        object DialogConfirm : Event
        object DialogDismiss : Event
        object DeleteConfirm : Event
        object DeleteDismiss : Event
        object NavigationHandled : Event
    }

    data class State(
        val groups: List<ChatGroup> = emptyList(),
        val availableTypes: List<ChatType> = enumValues<ChatType>().toList(),
        // Заменяет 7 полей диалогов на 2:
        // - editDialog: null = диалог закрыт, non-null = диалог открыт
        // - deleteConfirm: null = диалог удаления закрыт, non-null = группа для удаления
        val editDialog: GroupEditDialogState? = null,
        val deleteConfirm: ChatGroup? = null,
        val selectedGroupId: Long? = null
    )
}
