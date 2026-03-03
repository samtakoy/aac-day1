package com.example.day.features.user_settings.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.day.core.core_features.llm.domain.ModelConst
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.usecase.BindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.CreateUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.DeleteProfileFactUseCase
import com.example.day.core.core_features.memory.domain.usecase.GenerateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetAllProfilesUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetCurrentUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetProfileFactsFlowUseCase
import com.example.day.core.core_features.memory.domain.usecase.UnbindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryUseCase
import com.example.day.core.ui.uikit.components.ltm.LongTermFactUiItem
import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UserSettingsViewModelImpl(
    private val getCurrentProfile: GetCurrentUserProfileUseCase,
    private val getAllProfiles: GetAllProfilesUseCase,
    private val getProfileFactsFlow: GetProfileFactsFlowUseCase,
    private val createProfile: CreateUserProfileUseCase,
    private val bindProfile: BindUserProfileUseCase,
    private val unbindProfile: UnbindUserProfileUseCase,
    private val upsertFact: UpsertFactWithCategoryUseCase,
    private val deleteFact: DeleteProfileFactUseCase,
    private val generateAvatar: GenerateProfileAvatarUseCase,
) : ViewModel(), UserSettingsViewModel {

    private val _state = MutableStateFlow(UserSettingsViewModel.State())
    override fun getStateAsFlow(): StateFlow<UserSettingsViewModel.State> = _state

    private var factsCollectJob: Job? = null

    init {
        viewModelScope.launch { loadProfile() }
    }

    private suspend fun loadProfile() {
        val profile = getCurrentProfile()
        val allProfiles = getAllProfiles().map { it.title }
        _state.update { it.copy(currentProfile = profile, allProfiles = allProfiles.toImmutableList()) }

        factsCollectJob?.cancel()
        if (profile != null) {
            factsCollectJob = viewModelScope.launch {
                getProfileFactsFlow(profile.ltmGroupId).collectLatest { facts ->
                    _state.update { it.copy(profileFacts = facts.toUiItems().toImmutableList()) }
                }
            }
        } else {
            _state.update { it.copy(profileFacts = kotlinx.collections.immutable.persistentListOf()) }
        }
    }

    override fun onEvent(event: UserSettingsViewModel.Event) {
        when (event) {
            UserSettingsViewModel.Event.ResetProfileClick -> viewModelScope.launch {
                unbindProfile().onSuccess {
                    factsCollectJob?.cancel()
                    _state.update {
                        it.copy(
                            currentProfile = null,
                            profileFacts = persistentListOf()
                        )
                    }
                    val allProfiles = getAllProfiles().map { p -> p.title }
                    _state.update { it.copy(allProfiles = allProfiles.toImmutableList()) }
                }
            }

            UserSettingsViewModel.Event.SelectProfileClick ->
                _state.update { it.copy(showSelectDialog = true) }

            UserSettingsViewModel.Event.SelectDialogDismiss ->
                _state.update { it.copy(showSelectDialog = false) }

            is UserSettingsViewModel.Event.ProfileSelected -> viewModelScope.launch {
                bindProfile(event.name).onSuccess {
                    _state.update { it.copy(showSelectDialog = false) }
                    loadProfile()
                }
            }

            UserSettingsViewModel.Event.CreateProfileClick ->
                _state.update { it.copy(showCreateDialog = true, createProfileName = "", createProfileError = null) }

            UserSettingsViewModel.Event.CreateDialogDismiss ->
                _state.update { it.copy(showCreateDialog = false, createProfileName = "", createProfileError = null) }

            is UserSettingsViewModel.Event.CreateProfileNameChanged ->
                _state.update { it.copy(createProfileName = event.name, createProfileError = null) }

            UserSettingsViewModel.Event.CreateProfileConfirm -> viewModelScope.launch {
                val name = _state.value.createProfileName.trim()
                if (name.isBlank()) {
                    _state.update { it.copy(createProfileError = "Введите имя профиля") }
                    return@launch
                }
                createProfile(name).fold(
                    onSuccess = {
                        bindProfile(name)
                        _state.update { it.copy(showCreateDialog = false, createProfileName = "") }
                        loadProfile()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(createProfileError = e.message ?: "Ошибка создания профиля") }
                    }
                )
            }

            // ─── Факты ───

            UserSettingsViewModel.Event.AddFactClick -> _state.update {
                it.copy(
                    editingFact = LongTermFactUiItem("", 0L, "", "", 0L),
                    editFactKey = "",
                    editFactCategory = "",
                    editFactValue = "",
                    isAddingNewFact = true,
                    factEditError = null
                )
            }

            is UserSettingsViewModel.Event.EditFactClick -> _state.update {
                it.copy(
                    editingFact = event.fact,
                    editFactKey = event.fact.memoryKey,
                    editFactCategory = event.fact.category,
                    editFactValue = event.fact.fact,
                    isAddingNewFact = false,
                    factEditError = null
                )
            }

            is UserSettingsViewModel.Event.DeleteFactClick -> viewModelScope.launch {
                val ltmGroupId = _state.value.currentProfile?.ltmGroupId ?: return@launch
                deleteFact(ltmGroupId, event.fact.memoryKey, event.fact.categoryId)
                // Flow автоматически обновит profileFacts
            }

            is UserSettingsViewModel.Event.FactKeyChanged ->
                _state.update { it.copy(editFactKey = event.key, factEditError = null) }

            is UserSettingsViewModel.Event.FactCategoryChanged ->
                _state.update { it.copy(editFactCategory = event.category) }

            is UserSettingsViewModel.Event.FactValueChanged ->
                _state.update { it.copy(editFactValue = event.value, factEditError = null) }

            UserSettingsViewModel.Event.FactEditDismiss ->
                _state.update { it.copy(editingFact = null, factEditError = null) }

            UserSettingsViewModel.Event.FactEditConfirm -> viewModelScope.launch {
                val s = _state.value
                val profile = s.currentProfile ?: return@launch
                val key = s.editFactKey.trim()
                val category = s.editFactCategory.trim().ifBlank { "general" }
                val value = s.editFactValue.trim()
                if (key.isBlank()) {
                    _state.update { it.copy(factEditError = "Ключ не может быть пустым") }
                    return@launch
                }
                if (value.isBlank()) {
                    _state.update { it.copy(factEditError = "Значение не может быть пустым") }
                    return@launch
                }
                upsertFact.invokeByLTMGroup(
                    ltmGroupId = profile.ltmGroupId,
                    memoryKey = key,
                    categoryTitle = category,
                    fact = value
                )
                _state.update { it.copy(editingFact = null, factEditError = null) }
                // Flow автоматически обновит profileFacts
            }

            UserSettingsViewModel.Event.RegenerateAvatar -> {
                viewModelScope.launch { generateAvatar(ModelSettings(ModelConst.DEFAULT_MODEL)) }
            }
        }
    }

    private fun List<LongTermMemoryFact>.toUiItems(): List<LongTermFactUiItem> =
        map { LongTermFactUiItem(it.memoryKey, it.categoryId, it.category, it.fact, it.updatedAt) }

    class Factory @Inject constructor(
        private val getCurrentProfile: GetCurrentUserProfileUseCase,
        private val getAllProfiles: GetAllProfilesUseCase,
        private val getProfileFactsFlow: GetProfileFactsFlowUseCase,
        private val createProfile: CreateUserProfileUseCase,
        private val bindProfile: BindUserProfileUseCase,
        private val unbindProfile: UnbindUserProfileUseCase,
        private val upsertFact: UpsertFactWithCategoryUseCase,
        private val deleteFact: DeleteProfileFactUseCase,
        private val generateAvatar: GenerateProfileAvatarUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserSettingsViewModelImpl(
                getCurrentProfile = getCurrentProfile,
                getAllProfiles = getAllProfiles,
                getProfileFactsFlow = getProfileFactsFlow,
                createProfile = createProfile,
                bindProfile = bindProfile,
                unbindProfile = unbindProfile,
                upsertFact = upsertFact,
                deleteFact = deleteFact,
                generateAvatar = generateAvatar
            ) as T
        }
    }
}
