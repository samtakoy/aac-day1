package com.example.day.features.group_choice.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.core_features.chat.domain.usecase.CreateChatGroupUseCase
import com.example.day.core.core_features.chat.domain.usecase.DeleteChatGroupUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatGroupsUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatTypesUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatGroupUseCase
import com.example.day.core.ui.uikit.dialogs.group.GroupEditDialogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GroupChoiceViewModelImpl(
    private val getChatGroupsUseCase: GetChatGroupsUseCase,
    private val createChatGroupUseCase: CreateChatGroupUseCase,
    private val updateChatGroupUseCase: UpdateChatGroupUseCase,
    private val deleteChatGroupUseCase: DeleteChatGroupUseCase,
    private val getChatTypesUseCase: GetChatTypesUseCase
) : ViewModel(), GroupChoiceViewModel {

    private val _state = MutableStateFlow(GroupChoiceViewModel.State())
    override fun getStateAsFlow(): StateFlow<GroupChoiceViewModel.State> = _state

    init {
        viewModelScope.launch {
            val types = getChatTypesUseCase()
            _state.update { it.copy(availableTypes = types) }
        }

        viewModelScope.launch {
            getChatGroupsUseCase().collectLatest { groups ->
                _state.update { current -> current.copy(groups = groups) }
            }
        }
    }

    override fun onEvent(event: GroupChoiceViewModel.Event) {
        when (event) {
            GroupChoiceViewModel.Event.AddGroupClick -> _state.update { current ->
                val firstType = current.availableTypes.firstOrNull() ?: ChatType.SIMPLE_HISTORY
                current.copy(editDialog = GroupEditDialogState(
                    title = "",
                    selectedType = firstType,
                    isCreateMode = true
                ))
            }

            is GroupChoiceViewModel.Event.GroupClick -> _state.update {
                it.copy(selectedGroupId = event.group.id)
            }

            GroupChoiceViewModel.Event.NavigationHandled -> _state.update {
                it.copy(selectedGroupId = null)
            }

            is GroupChoiceViewModel.Event.DialogTitleChanged -> _state.update { current ->
                current.copy(editDialog = current.editDialog?.copy(title = event.title))
            }

            is GroupChoiceViewModel.Event.DialogTypeSelected -> _state.update { current ->
                current.copy(editDialog = current.editDialog?.copy(selectedType = event.type))
            }

            GroupChoiceViewModel.Event.DialogConfirm -> viewModelScope.launch {
                val dialog = _state.value.editDialog
                if (dialog != null && dialog.title.isNotBlank()) {
                    if (dialog.isCreateMode) {
                        createChatGroupUseCase(dialog.title.trim(), dialog.selectedType)
                    } else {
                        val currentGroup = _state.value.groups.find { it.title == dialog.title && it.chatType == dialog.selectedType }
                        if (currentGroup != null) {
                            val updatedGroup = currentGroup.copy(title = dialog.title.trim())
                            updateChatGroupUseCase(updatedGroup)
                        }
                    }
                }
                _state.update { it.copy(editDialog = null) }
            }

            GroupChoiceViewModel.Event.DialogDismiss -> _state.update {
                it.copy(editDialog = null)
            }

            is GroupChoiceViewModel.Event.EditGroupClick -> _state.update {
                it.copy(editDialog = GroupEditDialogState(
                    title = event.group.title,
                    selectedType = event.group.chatType,
                    isCreateMode = false
                ))
            }

            is GroupChoiceViewModel.Event.DeleteGroupClick -> _state.update {
                it.copy(deleteConfirm = event.group)
            }

            GroupChoiceViewModel.Event.DeleteConfirm -> viewModelScope.launch {
                _state.value.deleteConfirm?.let { deleteChatGroupUseCase(it.id) }
                _state.update { it.copy(deleteConfirm = null) }
            }

            GroupChoiceViewModel.Event.DeleteDismiss -> _state.update {
                it.copy(deleteConfirm = null)
            }
        }
    }

    class Factory @Inject constructor(
        private val getChatGroupsUseCase: GetChatGroupsUseCase,
        private val createChatGroupUseCase: CreateChatGroupUseCase,
        private val updateChatGroupUseCase: UpdateChatGroupUseCase,
        private val deleteChatGroupUseCase: DeleteChatGroupUseCase,
        private val getChatTypesUseCase: GetChatTypesUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupChoiceViewModelImpl(
                getChatGroupsUseCase,
                createChatGroupUseCase,
                updateChatGroupUseCase,
                deleteChatGroupUseCase,
                getChatTypesUseCase
            ) as T
        }
    }
}
