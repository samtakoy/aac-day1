# План: UserSettings Feature + Profile UI

## Постановка задачи

Добавить UI для управления профилями пользователя (`UserProfile`), который:
- Открывается как модальный экран из иконки профиля в Toolbar чатов
- Позволяет создавать/выбирать/сбрасывать профиль
- Показывает факты LTM текущего профиля с возможностью редактировать/добавлять/удалять
- Отображает ASCII-аватар профиля и предлагает сгенерировать его через LLM

Также:
- Добавить команду `@@talk(profile --list)` в TalkWorker

---

## Анализ существующей архитектуры

### Паттерн Feature (на примере group_choice)
```
features/group_choice/
├── api/GroupChoiceFeatureEntry.kt          — интерфейс entry-point (@Composable EntryPoint)
└── impl/
    ├── GroupChoiceFeatureEntryImpl.kt      — реализация (retain {} + DaggerComponent + viewModel)
    ├── di/
    │   ├── GroupChoiceFeatureScope.kt      — @scope аннотация
    │   ├── GroupChoiceFeatureComponent.kt  — @Component(dependencies=[Deps], modules=[Module])
    │   ├── GroupChoiceFeatureModule.kt     — @Provides для Factory
    │   ├── GroupChoiceFeatureDeps.kt       — интерфейс зависимостей из AppComponent
    │   └── GroupChoiceFeatureApiModule.kt  — @Binds EntryImpl -> Entry
    └── ui/
        ├── GroupChoiceScreen.kt
        ├── components/GroupsGrid.kt
        └── viewmodel/
            ├── GroupChoiceViewModel.kt     — интерфейс (State, Event, getStateAsFlow, onEvent)
            └── GroupChoiceViewModelImpl.kt — реализация + Factory
```

AppComponent имплементирует `FeatureEntryProvider` (возвращает entry-point) и `*FeatureDeps`.

### ChatsScreen получает entry-points из LocalAppComponent напрямую:
```kotlin
val agentsChatEntry = appComponent.getAgentsConsoleFeatureEntry()
```
→ Аналогично добавим `getUserSettingsFeatureEntry()`.

### Существующие Domain-компоненты (все уже созданы)
- `UserProfile(id, title, ltmGroupId, textAvatar: String?)` — domain model ✅
- `UserProfileRepository` — createProfile, getByName, getById, deleteByName, bindToUser, unbindFromUser, getProfileForUser, updateTextAvatar ✅
- `UserProfileRepositoryImpl` ✅
- `UserProfileDao` ✅
- Use cases: GetCurrentUserProfileUseCase, GetFactsByUserProfileUseCase, CreateUserProfileUseCase, BindUserProfileUseCase, UnbindUserProfileUseCase, UpdateProfileAvatarUseCase, UpsertFactForProfileUseCase ✅
- `LongTermMemoryRepository.deleteFact(ltmGroupId, memoryKey)` ✅
- `LongTermMemoryRepository.getFactsByGroupFlow(ltmGroupId): Flow<List<LongTermMemoryFact>>` ✅
- `LlmRequestUseCase.exec(modelSettings, systemPrompt, messages, promptText)` ✅

### Что отсутствует
- `UserProfileRepository.getAllProfiles()` — нет
- `ProfileCommandHandler` — нет `--list` команды
- `GetAllProfilesUseCase` — нет
- `DeleteProfileFactUseCase` — нет (repository есть)
- `GetProfileFactsFlowUseCase` — нет (реактивная версия для UI)
- `GenerateProfileAvatarUseCase` — нет (для генерации аватара из UI без Chat-контекста)
- Весь feature `user_settings` — нет
- Профиль-иконка в ChatsScreen — нет

### Подход к генерации аватара из UI
В ProfileCommandHandler аватар генерируется через `AIAgentFactory.getOrCreate(chat)`,
которому нужен `Chat` объект. В UserSettings нет конкретного чата.
**Решение:** `GenerateProfileAvatarUseCase` инжектит `LlmRequestUseCase` напрямую +
`ChatRepository.getOrCreateDefaultUsers()` → получить первый чат через ChatRepository,
либо использовать `ModelSettings` из первого доступного чата.
Если ни одного чата нет — возвращать `Result.failure` с понятным сообщением.

---

## Файлы для создания/изменения

### Изменяемые файлы
| Файл | Что меняется |
|------|-------------|
| `memory/domain/repository/UserProfileRepository.kt` | + `getAllProfiles(): List<UserProfile>` |
| `memory/data/repository/UserProfileRepositoryImpl.kt` | + реализация `getAllProfiles()` |
| `memory/data/local/dao/UserProfileDao.kt` | + `@Query getAllProfiles()` |
| `agent/domain/workers/innercommand/handler/ProfileCommandHandler.kt` | + `--list` case |
| `console/impl/ui/components/MemoryInspectorView.kt` | Использует новый shared компонент |
| `core/feature_entries/FeatureEntryProvider.kt` | + `getUserSettingsFeatureEntry()` |
| `app/di/AppComponent.kt` | + UserSettingsFeatureDeps, UserSettingsFeatureApiModule |
| `features/chats/impl/ui/ChatsScreen.kt` | + ProfileIconButton в TopAppBar + ModalBottomSheet |

### Создаваемые файлы

**New Use Cases:**
- `memory/domain/usecase/GetAllProfilesUseCase.kt`
- `memory/domain/usecase/DeleteProfileFactUseCase.kt`
- `memory/domain/usecase/GetProfileFactsFlowUseCase.kt`
- `memory/domain/usecase/GenerateProfileAvatarUseCase.kt`

**Shared UIKit component (перемещение из console feature):**
- `core/ui/uikit/components/ltm/LongTermFactUiItem.kt` — UI-модель факта
- `core/ui/uikit/components/ltm/LongTermFactsListView.kt` — Composable со списком фактов

**Feature user_settings:**
- `features/user_settings/api/UserSettingsFeatureEntry.kt`
- `features/user_settings/impl/UserSettingsFeatureEntryImpl.kt`
- `features/user_settings/impl/di/UserSettingsFeatureScope.kt`
- `features/user_settings/impl/di/UserSettingsFeatureComponent.kt`
- `features/user_settings/impl/di/UserSettingsFeatureModule.kt`
- `features/user_settings/impl/di/UserSettingsFeatureDeps.kt`
- `features/user_settings/impl/di/UserSettingsFeatureApiModule.kt`
- `features/user_settings/impl/ui/UserSettingsScreen.kt`
- `features/user_settings/impl/ui/components/SelectProfileDialog.kt`
- `features/user_settings/impl/ui/components/CreateProfileDialog.kt`
- `features/user_settings/impl/ui/components/FactEditDialog.kt`
- `features/user_settings/impl/ui/viewmodel/UserSettingsViewModel.kt`
- `features/user_settings/impl/ui/viewmodel/UserSettingsViewModelImpl.kt`

---

## Пошаговый план

---

### ШАГ 0: `--list` команда в ProfileCommandHandler + репозиторий

#### 0.1 UserProfileDao — добавить `getAllProfiles()`
- [x] `UserProfileDao.kt`: добавить `@Query("SELECT * FROM user_profiles ORDER BY title") suspend fun getAllProfiles(): List<UserProfileEntity>`

#### 0.2 UserProfileRepository — добавить `getAllProfiles()`
- [x] `UserProfileRepository.kt`: добавить `suspend fun getAllProfiles(): List<UserProfile>`
- [x] `UserProfileRepositoryImpl.kt`: реализовать через `userProfileDao.getAllProfiles().map { it.toDomain() }`

#### 0.3 ProfileCommandHandler — добавить `--list`
- [x] `ProfileCommandHandler.kt`: добавить `"list" in paramsMap -> handleList()` в when-блоке
- [x] Добавить `private suspend fun handleList()`:
  ```kotlin
  val all = userProfileRepository.getAllProfiles() // нужно инжектировать репозиторий или создать use case
  val current = getCurrentProfile()
  val text = buildString {
      if (all.isEmpty()) { appendLine("Профилей нет. Создайте: @@talk(profile --create NAME)"); return@buildString }
      appendLine("Профили (${all.size}):")
      all.forEach { profile ->
          val marker = if (profile.id == current?.id) " ✓" else ""
          appendLine("  • ${profile.title}$marker")
      }
  }
  return CommandResult.Success(text.trim())
  ```
- [x] Обновить docstring (добавить `--list` в список команд)
- [x] Инжектировать `UserProfileRepository` в конструктор (или создать GetAllProfilesUseCase и инжектировать его)

> **Примечание:** Используем `UserProfileRepository` напрямую в ProfileCommandHandler (он уже domain-уровень), или лучше создадим `GetAllProfilesUseCase` для соблюдения паттерна — см. ШАГ 1.

---

### ШАГ 1: Новые Use Cases

#### 1.1 GetAllProfilesUseCase
- [x] Создать `memory/domain/usecase/GetAllProfilesUseCase.kt`:
  ```kotlin
  class GetAllProfilesUseCase @Inject constructor(
      private val repository: UserProfileRepository
  ) {
      suspend operator fun invoke(): List<UserProfile> = repository.getAllProfiles()
  }
  ```

#### 1.2 GetProfileFactsFlowUseCase
- [x] Создать `memory/domain/usecase/GetProfileFactsFlowUseCase.kt`:
  ```kotlin
  class GetProfileFactsFlowUseCase @Inject constructor(
      private val memoryRepository: LongTermMemoryRepository
  ) {
      operator fun invoke(ltmGroupId: Long): Flow<List<LongTermMemoryFact>> =
          memoryRepository.getFactsByGroupFlow(ltmGroupId)
  }
  ```

#### 1.3 DeleteProfileFactUseCase
- [x] Создать `memory/domain/usecase/DeleteProfileFactUseCase.kt`:
  ```kotlin
  class DeleteProfileFactUseCase @Inject constructor(
      private val repository: LongTermMemoryRepository
  ) {
      suspend operator fun invoke(ltmGroupId: Long, memoryKey: String) =
          repository.deleteFact(ltmGroupId, memoryKey)
  }
  ```

#### 1.4 GenerateProfileAvatarUseCase
- [x] Создать `memory/domain/usecase/GenerateProfileAvatarUseCase.kt`:
  ```kotlin
  /**
   * Генерирует ASCII/emoji аватар профиля через LLM.
   * Для получения ModelSettings использует первый доступный чат из ChatRepository.
   * Возвращает Result.failure если нет ни одного чата с настройками.
   */
  class GenerateProfileAvatarUseCase @Inject constructor(
      private val llmRequestUseCase: LlmRequestUseCase,
      private val chatRepository: ChatRepository,
      private val getCurrentProfile: GetCurrentUserProfileUseCase,
      private val getFacts: GetFactsByUserProfileUseCase,
      private val updateAvatar: UpdateProfileAvatarUseCase
  ) {
      suspend operator fun invoke(): Result<String> {
          val profile = getCurrentProfile()
              ?: return Result.failure(IllegalStateException("Профиль не выбран"))
          val facts = getFacts()
          val chatSettings = chatRepository.getAnyChatSettings() // нужно добавить или использовать иной способ
              ?: return Result.failure(IllegalStateException("Нет доступных чатов с настройками"))
          val prompt = buildAvatarPrompt(facts)
          return llmRequestUseCase.exec(
              modelSettings = chatSettings.model,
              systemPrompt = null,
              messages = emptyList(),
              promptText = prompt
          ).map { result ->
              val avatar = result.responseText
              updateAvatar.update(avatar)
              avatar
          }
      }
  }
  ```

  > **Уточнение по `getAnyChatSettings()`:** добавить в `ChatRepository` метод для получения настроек любого существующего чата, либо в GenerateProfileAvatarUseCase использовать `chatRepository.getOrCreateDefaultUsers()` и поиск через чат-репозиторий. Если нет готового метода — создать `GetDefaultModelSettingsUseCase` или получить settings через список чатов.

#### 1.5 Завершить ШАГ 0: Обновить ProfileCommandHandler
- [x] Инжектировать `GetAllProfilesUseCase` в `ProfileCommandHandler`
- [x] Добавить `"list" in paramsMap -> handleList()` в when
- [x] Реализовать `handleList()`

---

### ШАГ 2: Shared UIKit компонент для LTM фактов

**Цель:** Выделить `LongTermContent` из `MemoryInspectorView.kt` в переиспользуемый компонент `core/ui/uikit/components/ltm/`, расширив его поддержкой edit/delete/add.

#### 2.1 Создать UI-модель
- [x] Создать `core/ui/uikit/components/ltm/LongTermFactUiItem.kt`:
  ```kotlin
  package com.example.day.core.ui.uikit.components.ltm

  data class LongTermFactUiItem(
      val memoryKey: String,
      val category: String,
      val fact: String,
      val updatedAt: Long
  )
  ```

#### 2.2 Создать переиспользуемый Composable
- [x] Создать `core/ui/uikit/components/ltm/LongTermFactsListView.kt`:
  ```kotlin
  /**
   * Отображает список LTM-фактов, сгруппированных по категориям.
   * @param facts список фактов
   * @param onEdit null если редактирование не поддерживается
   * @param onDelete null если удаление не поддерживается
   * @param onAdd null если добавление не поддерживается
   */
  @Composable
  fun LongTermFactsListView(
      facts: ImmutableList<LongTermFactUiItem>,
      modifier: Modifier = Modifier,
      showPromptPreview: Boolean = false,
      onEdit: ((LongTermFactUiItem) -> Unit)? = null,
      onDelete: ((LongTermFactUiItem) -> Unit)? = null,
      onAdd: (() -> Unit)? = null
  )
  ```
  - Перенести логику из `LongTermContent` + `LtmPromptPreview`
  - Добавить иконки редактирования/удаления для каждого факта (если onEdit/onDelete != null)
  - Кнопку "Добавить факт" (если onAdd != null)

#### 2.3 Обновить MemoryInspectorView
- [x] `MemoryInspectorView.kt`: заменить `LongTermContent(uiModel.longTermFacts)` на `LongTermFactsListView(items, showPromptPreview = true)`
- [x] Маппинг `LongTermFactItem` → `LongTermFactUiItem` (поля одинаковые)
- [x] Убрать `private fun LongTermContent(...)` и `private fun LtmPromptPreview(...)` (теперь в shared)
- [x] Оставить `LongTermFactItem` в console (или убрать, если нигде не используется кроме MemoryInspectorView)

---

### ШАГ 3: DI инфраструктура feature user_settings

#### 3.1 Scope
- [x] Создать `features/user_settings/impl/di/UserSettingsFeatureScope.kt`:
  ```kotlin
  @Scope
  @Retention(AnnotationRetention.RUNTIME)
  annotation class UserSettingsFeatureScope
  ```

#### 3.2 Deps интерфейс
- [x] Создать `features/user_settings/impl/di/UserSettingsFeatureDeps.kt`:
  ```kotlin
  interface UserSettingsFeatureDeps {
      val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
      val getAllProfilesUseCase: GetAllProfilesUseCase
      val getProfileFactsFlowUseCase: GetProfileFactsFlowUseCase
      val createUserProfileUseCase: CreateUserProfileUseCase
      val bindUserProfileUseCase: BindUserProfileUseCase
      val unbindUserProfileUseCase: UnbindUserProfileUseCase
      val upsertFactForProfileUseCase: UpsertFactForProfileUseCase
      val deleteProfileFactUseCase: DeleteProfileFactUseCase
      val updateProfileAvatarUseCase: UpdateProfileAvatarUseCase
      val generateProfileAvatarUseCase: GenerateProfileAvatarUseCase
  }
  ```

#### 3.3 Feature Module
- [x] Создать `features/user_settings/impl/di/UserSettingsFeatureModule.kt`:
  ```kotlin
  @Module
  class UserSettingsFeatureModule {
      @Provides
      @UserSettingsFeatureScope
      fun provideViewModelFactory(/* all use cases */): UserSettingsViewModelImpl.Factory =
          UserSettingsViewModelImpl.Factory(/* all use cases */)
  }
  ```

#### 3.4 Feature Component
- [x] Создать `features/user_settings/impl/di/UserSettingsFeatureComponent.kt`:
  ```kotlin
  @UserSettingsFeatureScope
  @Component(dependencies = [UserSettingsFeatureDeps::class], modules = [UserSettingsFeatureModule::class])
  internal interface UserSettingsFeatureComponent {
      @Component.Factory
      interface Factory {
          fun create(deps: UserSettingsFeatureDeps): UserSettingsFeatureComponent
      }
      fun getViewModelFactory(): UserSettingsViewModelImpl.Factory
  }
  ```

#### 3.5 API Entry Interface
- [x] Создать `features/user_settings/api/UserSettingsFeatureEntry.kt`:
  ```kotlin
  interface UserSettingsFeatureEntry {
      @Composable
      fun EntryPoint(modifier: Modifier = Modifier, onDismiss: () -> Unit)
  }
  ```

#### 3.6 Feature Entry Impl
- [x] Создать `features/user_settings/impl/UserSettingsFeatureEntryImpl.kt`:
  ```kotlin
  class UserSettingsFeatureEntryImpl @Inject constructor() : UserSettingsFeatureEntry {
      @Composable
      override fun EntryPoint(modifier: Modifier, onDismiss: () -> Unit) {
          val appComponent = LocalAppComponent.current
          val featureComponent: UserSettingsFeatureComponent = retain {
              DaggerUserSettingsFeatureComponent.factory().create(appComponent)
          }
          val viewModel: UserSettingsViewModelImpl = viewModel(
              factory = featureComponent.getViewModelFactory()
          )
          UserSettingsScreen(viewModel = viewModel, onDismiss = onDismiss, modifier = modifier)
      }
  }
  ```

#### 3.7 API Module (для AppComponent)
- [x] Создать `features/user_settings/impl/di/UserSettingsFeatureApiModule.kt`:
  ```kotlin
  @Module
  abstract class UserSettingsFeatureApiModule {
      @Binds
      @Singleton
      abstract fun bindUserSettingsEntry(impl: UserSettingsFeatureEntryImpl): UserSettingsFeatureEntry
  }
  ```

---

### ШАГ 4: UserSettingsViewModel (State, Event, Interface, Impl)

#### 4.1 ViewModel Interface
- [x] Создать `features/user_settings/impl/ui/viewmodel/UserSettingsViewModel.kt`:

  **State:**
  ```kotlin
  data class State(
      val currentProfile: UserProfile? = null,
      val profileFacts: ImmutableList<LongTermFactUiItem> = persistentListOf(),
      val allProfiles: ImmutableList<String> = persistentListOf(),  // названия для выбора
      val isAvatarGenerating: Boolean = false,
      val avatarGenerateError: String? = null,
      // Диалоги
      val showSelectDialog: Boolean = false,
      val showCreateDialog: Boolean = false,
      val createProfileName: String = "",
      val createProfileError: String? = null,
      // Редактирование факта
      val editingFact: LongTermFactUiItem? = null,   // null = диалог закрыт, иначе редактируем
      val editFactKey: String = "",
      val editFactCategory: String = "",
      val editFactValue: String = "",
      val isAddingNewFact: Boolean = false,           // true если создаём новый факт
  )
  ```

  **Events:**
  ```kotlin
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
      // Аватар
      data object GenerateAvatarClick : Event
      data object DismissAvatarError : Event
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
  ```

#### 4.2 ViewModel Implementation
- [x] Создать `features/user_settings/impl/ui/viewmodel/UserSettingsViewModelImpl.kt`:

  Ключевые аспекты реализации:

  **init:**
  ```kotlin
  init {
      viewModelScope.launch { loadProfile() }
  }

  private suspend fun loadProfile() {
      val profile = getCurrentUserProfileUseCase()
      val allProfiles = getAllProfilesUseCase().map { it.title }
      _state.update { it.copy(currentProfile = profile, allProfiles = allProfiles.toImmutableList()) }

      if (profile != null) {
          // Собираем факты реактивно
          viewModelScope.launch {
              getProfileFactsFlowUseCase(profile.ltmGroupId).collect { facts ->
                  _state.update { it.copy(profileFacts = facts.toUiItems().toImmutableList()) }
              }
          }
      }
  }
  ```

  **onEvent(ProfileSelected):**
  ```kotlin
  is Event.ProfileSelected -> viewModelScope.launch {
      bindUserProfileUseCase(event.name).fold(
          onSuccess = {
              _state.update { it.copy(showSelectDialog = false) }
              loadProfile()
          },
          onFailure = { /* show error */ }
      )
  }
  ```

  **onEvent(ResetProfileClick):**
  ```kotlin
  Event.ResetProfileClick -> viewModelScope.launch {
      unbindUserProfileUseCase().fold(
          onSuccess = {
              _state.update { it.copy(currentProfile = null, profileFacts = persistentListOf()) }
          },
          onFailure = { /* error */ }
      )
  }
  ```

  **onEvent(CreateProfileConfirm):**
  ```kotlin
  Event.CreateProfileConfirm -> viewModelScope.launch {
      val name = _state.value.createProfileName.trim()
      if (name.isBlank()) {
          _state.update { it.copy(createProfileError = "Введите имя профиля") }
          return@launch
      }
      createUserProfileUseCase(name).fold(
          onSuccess = {
              bindUserProfileUseCase(name)  // сразу привязать к пользователю
              _state.update { it.copy(showCreateDialog = false, createProfileName = "") }
              loadProfile()
          },
          onFailure = { _state.update { s -> s.copy(createProfileError = it.message) } }
      )
  }
  ```

  **onEvent(GenerateAvatarClick):**
  ```kotlin
  Event.GenerateAvatarClick -> viewModelScope.launch {
      _state.update { it.copy(isAvatarGenerating = true, avatarGenerateError = null) }
      generateProfileAvatarUseCase().fold(
          onSuccess = {
              // аватар уже сохранён в UseCase, перезагружаем профиль
              loadProfile()
          },
          onFailure = {
              _state.update { it.copy(avatarGenerateError = it.message) }
          }
      )
      _state.update { it.copy(isAvatarGenerating = false) }
  }
  ```

  **Факты (Edit/Delete/Add):**
  ```kotlin
  is Event.EditFactClick -> _state.update { it.copy(
      editingFact = event.fact,
      editFactKey = event.fact.memoryKey,
      editFactCategory = event.fact.category,
      editFactValue = event.fact.fact,
      isAddingNewFact = false
  ) }

  Event.AddFactClick -> _state.update { it.copy(
      editingFact = LongTermFactUiItem("", "", "", 0L), // пустышка
      editFactKey = "",
      editFactCategory = "",
      editFactValue = "",
      isAddingNewFact = true
  ) }

  is Event.DeleteFactClick -> viewModelScope.launch {
      val profile = _state.value.currentProfile ?: return@launch
      deleteProfileFactUseCase(profile.ltmGroupId, event.fact.memoryKey)
      // Flow автоматически обновит список
  }

  Event.FactEditConfirm -> viewModelScope.launch {
      val profile = _state.value.currentProfile ?: return@launch
      val s = _state.value
      val raw = "${s.editFactKey}:${s.editFactCategory}:${s.editFactValue}"
      upsertFactForProfileUseCase(raw).fold(
          onSuccess = { _state.update { it.copy(editingFact = null) } },
          onFailure = { /* show error */ }
      )
  }
  ```

  > **Маппинг:** `fun LongTermMemoryFact.toUiItem()` = `LongTermFactUiItem(memoryKey, category, fact, updatedAt)`

  **Factory:**
  ```kotlin
  class Factory @Inject constructor(/* все use cases */) : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
          UserSettingsViewModelImpl(/* all */) as T
  }
  ```

---

### ШАГ 5: UserSettingsScreen + Диалоги

#### 5.1 SelectProfileDialog
- [x] Создать `features/user_settings/impl/ui/components/SelectProfileDialog.kt`:
  - AlertDialog со списком профилей (LazyColumn с RadioButton или простым текстом)
  - При выборе — `onEvent(Event.ProfileSelected(name))`
  - Кнопка "Отмена"

#### 5.2 CreateProfileDialog
- [x] Создать `features/user_settings/impl/ui/components/CreateProfileDialog.kt`:
  - AlertDialog с TextField для имени
  - Кнопка "Создать" (активна если имя не пустое) + "Отмена"
  - Показывает ошибку `state.createProfileError`

#### 5.3 FactEditDialog
- [x] Создать `features/user_settings/impl/ui/components/FactEditDialog.kt`:
  - AlertDialog с тремя TextField: Ключ, Категория, Значение
  - Заголовок: "Редактировать факт" или "Добавить факт"
  - Кнопки "Сохранить" / "Отмена"
  - Кнопка "Сохранить" активна если ключ и значение не пустые

#### 5.4 UserSettingsScreen
- [x] Создать `features/user_settings/impl/ui/UserSettingsScreen.kt`:

  ```
  ┌──────────────────────────────────────┐
  │  Настройки пользователя         [×]  │  ← onDismiss handle-bar
  ├──────────────────────────────────────┤
  │  Текущий профиль:                    │
  │  [Имя профиля или "не выбран"]       │
  │                                      │
  │  [Выбрать]  [Добавить]  [Сбросить]  │
  │                                      │
  │  ┌───────────────────────────────┐   │
  │  │   ASCII АВАТАР               │   │  ← если есть
  │  │   (monospace Text)           │   │
  │  └───────────────────────────────┘   │
  │  [Сгенерировать аватар]              │  ← кнопка / индикатор загрузки
  │                                      │
  │  Факты профиля:                      │
  │  LongTermFactsListView(              │
  │    facts = state.profileFacts,       │
  │    onEdit = {...},                   │
  │    onDelete = {...},                 │
  │    onAdd = {...}                     │
  │  )                                   │
  └──────────────────────────────────────┘
  ```

  Компоненты:
  - Верхняя часть — Column с профилем и кнопками
  - Если `state.currentProfile?.textAvatar != null` → отображаем в монопространственном шрифте
  - Кнопка "Сгенерировать аватар" с CircularProgressIndicator когда `isAvatarGenerating`
  - `LongTermFactsListView` с колбэками edit/delete/add
  - Три диалога (управляются через State)

---

### ШАГ 6: Профиль-иконка в ChatsScreen + ModalBottomSheet

#### 6.1 Обновить ChatsScreen
- [x] `ChatsScreen.kt` — в `ChatsScreenInternal`:
  - Добавить `var showUserSettings by remember { mutableStateOf(false) }`
  - Добавить `val userSettingsEntry = appComponent.getUserSettingsFeatureEntry()`
  - В TopAppBar добавить `actions = { IconButton(onClick = { showUserSettings = true }) { Icon(Icons.Default.Person, ...) } }`
  - После Column добавить:
    ```kotlin
    if (showUserSettings) {
        ModalBottomSheet(onDismissRequest = { showUserSettings = false }) {
            userSettingsEntry.EntryPoint(
                modifier = Modifier.fillMaxWidth(),
                onDismiss = { showUserSettings = false }
            )
        }
    }
    ```

---

### ШАГ 7: Регистрация в AppComponent + FeatureEntryProvider

#### 7.1 FeatureEntryProvider
- [x] `FeatureEntryProvider.kt`: добавить `fun getUserSettingsFeatureEntry(): UserSettingsFeatureEntry`

#### 7.2 AppComponent
- [x] `AppComponent.kt`:
  - Добавить `UserSettingsFeatureApiModule::class` в modules
  - Имплементировать `UserSettingsFeatureDeps`

---

### ШАГ 8: Проверка генерации аватара (ChatRepository)

- [x] Проверить, есть ли в `ChatRepository` метод для получения настроек любого чата
- [x] Если нет — добавить `suspend fun getFirstAvailableChatSettings(): ChatSettings?` или использовать `getOrCreateDefaultUsers()` + поиск
- [x] Убедиться, что `GenerateProfileAvatarUseCase` корректно работает

---

### ШАГ 9: Финальная проверка и компиляция

- [x] Проверить, что все импорты корректны
- [x] Проверить, что `AppComponent` правильно предоставляет все зависимости для `UserSettingsFeatureDeps`
- [x] Проверить, что Room компилируется (нет несовместимых версий схемы)
- [x] Проверить, что `UpsertFactForProfileUseCase` работает корректно из ViewModel

---

## Итог после реализации

**Команды для проверки в чате:**
```
@@talk(profile --list)              → список всех профилей с галочкой текущего
```

**UI проверка:**
1. Нажать иконку профиля в Toolbar → открылся ModalBottomSheet
2. Нажать "Добавить" → диалог → ввести имя → профиль создан и привязан
3. Нажать "Выбрать" → список профилей → выбрать → привязан
4. Нажать "Сбросить" → профиль отвязан
5. При наличии профиля → видны факты LTM
6. Редактирование/удаление/добавление фактов работает
7. "Сгенерировать аватар" → загрузка → ASCII аватар сохранён и отображается
