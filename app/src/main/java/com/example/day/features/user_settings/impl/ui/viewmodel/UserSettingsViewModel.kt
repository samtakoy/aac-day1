package com.example.day.features.user_settings.impl.ui.viewmodel

import com.example.day.core.core_features.memory.domain.model.UserProfile
import com.example.day.core.ui.uikit.components.ltm.LongTermFactUiItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

interface UserSettingsViewModel {

    data class State(
        // TODO домайн моделька
        val currentProfile: UserProfile? = null,
        val profileFacts: ImmutableList<LongTermFactUiItem> = persistentListOf(),
        val allProfiles: ImmutableList<String> = persistentListOf(),
        // Диалог выбора профиля
        val showSelectDialog: Boolean = false,
        // Диалог создания профиля
        val showCreateDialog: Boolean = false,
        val createProfileName: String = "",
        val createProfileError: String? = null,
        // Диалог редактирования/добавления факта
        val editingFact: LongTermFactUiItem? = null,
        val editFactKey: String = "",
        val editFactCategory: String = "",
        val editFactValue: String = "",
        val isAddingNewFact: Boolean = false,
        val factEditError: String? = null
    )

    sealed interface Event {
        // Профиль
        data object ResetProfileClick : Event
        data object SelectProfileClick : Event
        data object CreateProfileClick : Event
        data class ProfileSelected(val name: String) : Event
        data object SelectDialogDismiss : Event
        data class CreateProfileNameChanged(val name: String) : Event
        data object CreateProfileConfirm : Event
        data object CreateDialogDismiss : Event
        data object RegenerateAvatar : Event
        // Факты
        data class EditFactClick(val fact: LongTermFactUiItem) : Event
        data class DeleteFactClick(val fact: LongTermFactUiItem) : Event
        data object AddFactClick : Event
        data class FactKeyChanged(val key: String) : Event
        data class FactCategoryChanged(val category: String) : Event
        data class FactValueChanged(val value: String) : Event
        data object FactEditConfirm : Event
        data object FactEditDismiss : Event
    }

    fun getStateAsFlow(): StateFlow<State>
    fun onEvent(event: Event)
}
