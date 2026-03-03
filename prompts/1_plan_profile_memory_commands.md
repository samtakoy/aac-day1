# ПЛАН РЕАЛИЗАЦИИ: Profile Commands + Agent Memory Commands

## Постановка задачи

Реализовать в `TalkWorker` обработку трёх групп команд:

### 1. Команды профиля пользователя (`@@talk(profile ...)`)
```
@@talk(profile --create NAME)         // Создать профиль с именем NAME
@@talk(profile --unbind)              // Разорвать связь пользователя с профилем
@@talk(profile --remove NAME)         // Удалить профиль по имени
@@talk(profile --bind NAME)           // Привязать профиль NAME к текущему пользователю
@@talk(profile --show_facts)          // Показать факты привязанного профиля
@@talk(profile --add_fact cat:text) // Добавить факт в профиль
@@talk(profile --avatar reset)        // Сбросить текстовый аватар
@@talk(profile --avatar generate)     // Сгенерировать аватар через LLM (30x30 псевдографика)
@@talk(profile --avatar show)         // Показать текущий аватар
```

### 2. Команды памяти агента (`@@talk(agent_memory ...)`)
```
@@talk(agent_memory --add MemoryType)    // Добавить тип памяти агенту
@@talk(agent_memory --remove MemoryType) // Удалить тип памяти у агента
@@talk(agent_memory --list)              // Список активных типов памяти агента
```

### 3. Завершить `UserProfileMemoryProvider`
- Получать факты из профиля привязанного к текущему пользователю
- Возвращать `emptyList()` если профиль не привязан

---

## Архитектурный анализ (результаты исследования кода)

### Что уже есть и работает ✅
- `UserProfileEntity` в DB (entities) — но **баг**: `textAvatar: Long` вместо `String?`
- `LTMGroupToUserProfileEntity` в DB (entities) — DAO отсутствует
- `UserToProfileEntity` в DB (entities) — DAO отсутствует
- `LTMGroupEntity`, `LongTermMemoryEntity`, `LTMCategoryEntity` — полностью рабочие
- `UpsertFactWithCategoryUseCase.invokeByLTMGroup(ltmGroupId, key, cat, fact)` — готов
- `LongTermMemoryRepository.getFactsByGroup(ltmGroupId)` — готов
- `AgentRepository.addMemoryType / removeMemoryType / updateAgentMemories` — готов
- `UserDao.getUserByType(type: Int)` — получить пользователя по типу (UserType.User.ordinal == 0)
- `UserProfileMemoryProvider` — заглушка с TODO
- `MemoryType` enum: `UserProfile("uProfile")`, `Chat("chat")`, `ChatGroup("chatGroup")`
- `AgentToMemoryTypeEntity` + `AgentMemoryDao` — полностью рабочие
- `CommandHandler` паттерн — `commandName: String`, `handle(params, chat): CommandResult`
- `CommandHandlerModule` — `@Binds @IntoSet` для регистрации handlers
- `AIAgentFactory.getOrCreate(name, isCommon, chat)` — для вызова LLM из хэндлера
- `ChatDatabase` версия 6, **без fallbackToDestructiveMigration** → добавить при bump версии

### Ключевые архитектурные решения

**Получение LTM group для профиля:**
`UserProfileEntity.memoryGroupId: Long` — прямой FK к `LTMGroupEntity`.
→ Использовать напрямую: `profile.memoryGroupId` → `LongTermMemoryRepository.getFactsByGroup(profile.memoryGroupId)`
→ При создании профиля: `ltmGroupRepository.createGroup()` → получить `ltmGroupId` → создать `UserProfileEntity(memoryGroupId = ltmGroupId)`

**Получение текущего пользователя:**
`UserDao.getUserByType(UserType.User.ordinal)` — единственный пользователь типа User в системе.
→ Получать через `UserRepository` или напрямую (создать use case)

**Привязка профиля к пользователю:**
`UserToProfileEntity(userId, profileId)` — PK по `userId`, UPSERT/INSERT OR REPLACE

**Генерация аватара:**
`AIAgentFactory.getOrCreate("talk_agent", false, chat)` → `agent.process(chat.settings, prompt, null)` → текст 30x30

---

## Файлы для изменения (с указанием изменений)

| Файл | Изменение |
|------|-----------|
| `memory/data/local/model/user/UserProfileEntity.kt` | `textAvatar: Long` → `textAvatar: String?` |
| `chat/data/local/ChatDatabase.kt` | version 6→7, добавить UserProfileDao, добавить fallbackToDestructiveMigration |
| `memory/domain/provider/UserProfileMemoryProvider.kt` | Полная реализация |
| `agent/di/CommandHandlerModule.kt` | Добавить биндинги для ProfileCommandHandler и AgentMemoryCommandHandler |
| `chat/di/ChatCoreFeatureModule.kt` | Добавить provideUserProfileDao |
| `memory/di/MemoryCoreFeatureModule.kt` | Добавить биндинги UserProfileRepository |

---

## Файлы для создания

### Domain layer — Memory module
| Файл | Описание |
|------|----------|
| `memory/domain/model/UserProfile.kt` | Domain model профиля |
| `memory/domain/repository/UserProfileRepository.kt` | Репозиторий интерфейс |
| `memory/domain/usecase/CreateUserProfileUseCase.kt` | Создать профиль + LTMGroup |
| `memory/domain/usecase/RemoveUserProfileUseCase.kt` | Удалить профиль по имени |
| `memory/domain/usecase/GetUserProfileByNameUseCase.kt` | Найти профиль по имени |
| `memory/domain/usecase/GetCurrentUserProfileUseCase.kt` | Профиль привязанный к User |
| `memory/domain/usecase/BindUserProfileUseCase.kt` | Привязать профиль к User |
| `memory/domain/usecase/UnbindUserProfileUseCase.kt` | Отвязать профиль от User |
| `memory/domain/usecase/GetFactsByUserProfileUseCase.kt` | Факты из памяти профиля |
| `memory/domain/usecase/UpsertFactForProfileUseCase.kt` | Добавить факт в профиль |
| `memory/domain/usecase/UpdateProfileAvatarUseCase.kt` | Обновить textAvatar профиля |

### Data layer — Memory module
| Файл | Описание |
|------|----------|
| `memory/data/local/dao/UserProfileDao.kt` | DAO для UserProfile + UserToProfile |
| `memory/data/UserProfileRepositoryImpl.kt` | Реализация репозитория |

### Agent — Command Handlers
| Файл | Описание |
|------|----------|
| `agent/domain/workers/innercommand/handler/ProfileCommandHandler.kt` | Все profile субкоманды |
| `agent/domain/workers/innercommand/handler/AgentMemoryCommandHandler.kt` | agent_memory --add/remove/list |

---

## Шаги выполнения

### ШАГ 1: Исправить UserProfileEntity (textAvatar тип) ✅
- [x] Изменить тип поля `textAvatar` с `Long` на `String?` в `UserProfileEntity.kt`
- [x] В `ChatDatabase.kt`:
  - [x] Поднять версию с 6 до 7
  - [x] Добавить `abstract fun userProfileDao(): UserProfileDao`
  - [x] Добавить в builder: `.fallbackToDestructiveMigration()`

---

### ШАГ 2: Создать UserProfile Domain Model ✅
**Файл:** `memory/domain/model/UserProfile.kt`
```kotlin
data class UserProfile(
    val id: Long,
    val title: String,
    val ltmGroupId: Long,     // = UserProfileEntity.memoryGroupId
    val textAvatar: String?
)
```

---

### ШАГ 3: Создать UserProfileRepository (domain interface) ✅
**Файл:** `memory/domain/repository/UserProfileRepository.kt`
```kotlin
interface UserProfileRepository {
    suspend fun createProfile(name: String): UserProfile         // создаёт LTMGroup + UserProfileEntity
    suspend fun getByName(name: String): UserProfile?
    suspend fun getById(id: Long): UserProfile?
    suspend fun deleteByName(name: String)
    suspend fun bindToUser(userId: Long, profileId: Long)        // upsert в UserToProfileEntity
    suspend fun unbindFromUser(userId: Long)                     // удалить из UserToProfileEntity
    suspend fun getProfileForUser(userId: Long): UserProfile?    // join UserToProfile + UserProfile
    suspend fun updateTextAvatar(profileId: Long, avatar: String?)
}
```

---

### ШАГ 4: Создать UserProfileDao ✅
**Файл:** `memory/data/local/dao/UserProfileDao.kt`
```kotlin
@Dao
internal interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(entity: UserProfileEntity): Long

    @Query("SELECT * FROM user_profiles WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserProfileEntity?

    @Query("DELETE FROM user_profiles WHERE title = :title")
    suspend fun deleteByTitle(title: String)

    @Query("UPDATE user_profiles SET text_avatar = :avatar WHERE id = :id")
    suspend fun updateTextAvatar(id: Long, avatar: String?)

    // UserToProfile operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserToProfile(link: UserToProfileEntity)

    @Query("DELETE FROM user_to_profile WHERE user_id = :userId")
    suspend fun deleteUserToProfile(userId: Long)

    @Query("""
        SELECT up.* FROM user_profiles up
        INNER JOIN user_to_profile utp ON utp.profile_id = up.id
        WHERE utp.user_id = :userId LIMIT 1
    """)
    suspend fun getProfileForUser(userId: Long): UserProfileEntity?
}
```

---

### ШАГ 5: Создать UserProfileRepositoryImpl ✅
**Файл:** `memory/data/UserProfileRepositoryImpl.kt`
- Реализует `UserProfileRepository`
- Инъекция: `UserProfileDao`, `LTMGroupRepository`
- `createProfile(name)`:
  1. `ltmGroupRepository.createGroup()` → `ltmGroupId`
  2. `userProfileDao.insertProfile(UserProfileEntity(title=name, memoryGroupId=ltmGroupId, textAvatar=null))`
  3. Вернуть `UserProfile(id, name, ltmGroupId, null)`
- Маппинг `UserProfileEntity` → `UserProfile` (маппер или extension)

---

### ШАГ 6: Создать Use Cases ✅

#### 6.1 CreateUserProfileUseCase
```kotlin
class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(name: String): Result<UserProfile>
    // Проверить что имя не пустое, вернуть Result
}
```

#### 6.2 RemoveUserProfileUseCase
```kotlin
class RemoveUserProfileUseCase @Inject constructor(private val repository: UserProfileRepository) {
    suspend operator fun invoke(name: String)
    // repository.deleteByName(name)
}
```

#### 6.3 GetUserProfileByNameUseCase
```kotlin
class GetUserProfileByNameUseCase @Inject constructor(private val repository: UserProfileRepository) {
    suspend operator fun invoke(name: String): UserProfile?
}
```

#### 6.4 GetCurrentUserProfileUseCase
```kotlin
class GetCurrentUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
    private val userRepository: UserRepository  // или UserDao через domain boundary
) {
    // userDao.getUserByType(UserType.User.ordinal) → userId
    // repository.getProfileForUser(userId)
    suspend operator fun invoke(): UserProfile?
}
```
**Примечание:** Если `UserRepository` не существует как domain interface — создать его с методом `getUserByType(type: UserType): User?`, биндить через ChatCoreFeatureModule.

#### 6.5 BindUserProfileUseCase
```kotlin
class BindUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
    private val getCurrentUser: GetCurrentUserUseCase  // возвращает User типа UserType.User
) {
    suspend operator fun invoke(profileName: String): Result<Unit>
    // Найти профиль по имени → bindToUser(userId, profileId)
}
```

#### 6.6 UnbindUserProfileUseCase
```kotlin
class UnbindUserProfileUseCase @Inject constructor(...) {
    suspend operator fun invoke()
    // getCurrentUser() → unbindFromUser(userId)
}
```

#### 6.7 GetFactsByUserProfileUseCase
```kotlin
class GetFactsByUserProfileUseCase @Inject constructor(
    private val memoryRepository: LongTermMemoryRepository,
    private val getCurrentProfile: GetCurrentUserProfileUseCase
) {
    suspend operator fun invoke(): List<LongTermMemoryFact>
    // getCurrentProfile() → profile.ltmGroupId → memoryRepository.getFactsByGroup(ltmGroupId)
}
```

#### 6.8 UpsertFactForProfileUseCase
```kotlin
class UpsertFactForProfileUseCase @Inject constructor(
    private val upsertFact: UpsertFactWithCategoryUseCase,
    private val getCurrentProfile: GetCurrentUserProfileUseCase
) {
    // Парсить строку формата "key:category:text"
    // getCurrentProfile() → profile.ltmGroupId
    // upsertFact.invokeByLTMGroup(ltmGroupId, key, category, text)
    suspend operator fun invoke(rawFact: String): Result<Unit>
}
```

#### 6.9 UpdateProfileAvatarUseCase
```kotlin
class UpdateProfileAvatarUseCase @Inject constructor(
    private val repository: UserProfileRepository,
    private val getCurrentProfile: GetCurrentUserProfileUseCase
) {
    suspend fun reset(): Result<Unit>        // updateTextAvatar(id, null)
    suspend fun update(avatar: String): Result<Unit>   // updateTextAvatar(id, avatar)
}
```

---

### ШАГ 7: Реализовать UserProfileMemoryProvider ✅
**Файл:** `memory/domain/provider/UserProfileMemoryProvider.kt`

```kotlin
class UserProfileMemoryProvider @Inject constructor(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val getFactsByUserProfileUseCase: GetFactsByUserProfileUseCase
) : MemoryProvider {

    override suspend fun getMemoryContext(): List<AContextMessage> {
        val profile = getCurrentUserProfileUseCase() ?: return emptyList()
        val facts = getFactsByUserProfileUseCase()
        if (facts.isEmpty()) return emptyList()

        val content = buildString {
            appendLine("## Профиль пользователя: ${profile.title}")
            facts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                appendLine("### $category")
                categoryFacts.forEach { fact ->
                    appendLine("- [${fact.memoryKey}] ${fact.fact}")
                }
            }
        }
        return listOf(AContextMessage.System(content))
    }
}
```
**Примечание:** Проверить тип `AContextMessage.System` — возможно другое имя конструктора.

---

### ШАГ 8: Создать ProfileCommandHandler ✅
**Файл:** `agent/domain/workers/innercommand/handler/ProfileCommandHandler.kt`

```kotlin
class ProfileCommandHandler @Inject constructor(
    private val createProfile: CreateUserProfileUseCase,
    private val removeProfile: RemoveUserProfileUseCase,
    private val bindProfile: BindUserProfileUseCase,
    private val unbindProfile: UnbindUserProfileUseCase,
    private val getCurrentProfile: GetCurrentUserProfileUseCase,
    private val getProfileByName: GetUserProfileByNameUseCase,
    private val getFactsByProfile: GetFactsByUserProfileUseCase,
    private val upsertFact: UpsertFactForProfileUseCase,
    private val updateAvatar: UpdateProfileAvatarUseCase,
    private val aiAgentFactory: AIAgentFactory,
    private val getFactsForAvatar: GetFactsByUserProfileUseCase  // reuse
) : CommandHandler {

    override val commandName = "profile"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        return when {
            "create" in paramsMap -> handleCreate(paramsMap["create"])
            "remove" in paramsMap -> handleRemove(paramsMap["remove"])
            "bind" in paramsMap   -> handleBind(paramsMap["bind"])
            "unbind" in paramsMap -> handleUnbind()
            "show_facts" in paramsMap -> handleShowFacts()
            "add_fact" in paramsMap -> handleAddFact(paramsMap["add_fact"])
            "avatar" in paramsMap -> handleAvatar(paramsMap["avatar"], chat)
            else -> CommandResult.Error("Неизвестная команда profile. Доступные: --create --remove --bind --unbind --show_facts --add_fact --avatar")
        }
    }
    // ... приватные handleXxx методы
}
```

**Парсинг параметров:**
`InnerCommandParser` парсит `@@talk(profile --create NAME)` как:
- `params = [("create", "NAME")]`
- `params = [("unbind", null)]`
- `params = [("avatar", "generate")]`
- `params = [("add_fact", "category:text")]`

**handleAvatar** разбирает sub-value: `"reset"`, `"generate"`, `"show"`

**handleShowFacts** — форматирует факты в текст, возвращает `CommandResult.Success(text)`

**handleAvatar("generate"):**
1. `getFactsByProfile()` → список фактов
2. Построить prompt для генерации аватара 30x30
3. `aiAgentFactory.getOrCreate("talk_agent", false, chat)`
4. `agent.process(chat.settings, prompt, null).getOrThrow().responseText`
5. `updateAvatar.update(avatarText)`
6. Вернуть `CommandResult.Success(avatarText)`

---

### ШАГ 9: Создать AgentMemoryCommandHandler ✅
**Файл:** `agent/domain/workers/innercommand/handler/AgentMemoryCommandHandler.kt`

```kotlin
class AgentMemoryCommandHandler @Inject constructor(
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "agent_memory"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val agent = agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            isCommon = false,
            chatSettings = chat.settings
        )
        return when {
            "add" in paramsMap -> handleAdd(agent.id, paramsMap["add"])
            "remove" in paramsMap -> handleRemove(agent.id, paramsMap["remove"])
            "list" in paramsMap -> handleList(agent.id)
            else -> CommandResult.Error("Неизвестная команда agent_memory. Доступные: --add --remove --list")
        }
    }

    private suspend fun handleAdd(agentId: Long, typeName: String?): CommandResult {
        val memoryType = MemoryType.entries.find { it.dbName == typeName || it.name == typeName }
            ?: return CommandResult.Error("Неизвестный MemoryType: $typeName. Доступные: ${MemoryType.entries.map { it.name }}")
        agentRepository.addMemoryType(agentId, memoryType)
        return CommandResult.Success("Тип памяти ${memoryType.name} добавлен агенту")
    }

    private suspend fun handleRemove(agentId: Long, typeName: String?): CommandResult {
        val memoryType = MemoryType.entries.find { it.dbName == typeName || it.name == typeName }
            ?: return CommandResult.Error("Неизвестный MemoryType: $typeName")
        agentRepository.removeMemoryType(agentId, memoryType)
        return CommandResult.Success("Тип памяти ${memoryType.name} удалён у агента")
    }

    private suspend fun handleList(agentId: Long): CommandResult {
        // agentRepository должен иметь getMemoryTypes(agentId): List<MemoryType>
        // Если метода нет — добавить в AgentRepository интерфейс + impl
        val types = agentRepository.getMemoryTypes(agentId)
        val text = if (types.isEmpty()) "Типы памяти не настроены"
                   else types.joinToString("\n") { "• ${it.name} (${it.dbName})" }
        return CommandResult.Success(text)
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
```

**Важно:** Проверить наличие метода `getMemoryTypes(agentId)` в `AgentRepository`.
Если отсутствует — добавить в интерфейс + `AgentRepositoryImpl`.

---

### ШАГ 10: Обновить DI ✅

#### 10.1 CommandHandlerModule.kt
```kotlin
@Binds @IntoSet
abstract fun bindProfileHandler(handler: ProfileCommandHandler): CommandHandler

@Binds @IntoSet
abstract fun bindAgentMemoryHandler(handler: AgentMemoryCommandHandler): CommandHandler
```

#### 10.2 ChatCoreFeatureModule.kt
```kotlin
@Provides
internal fun provideUserProfileDao(db: ChatDatabase): UserProfileDao = db.userProfileDao()
```

#### 10.3 MemoryCoreFeatureModule.kt (или аналогичный)
```kotlin
@Binds
abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository
```

#### 10.4 UserProfileMemoryProvider
Убедиться что `UserProfileMemoryProvider` корректно инъектируется в тех местах где создаётся `CompositeMemoryProvider` для агента с типом `MemoryType.UserProfile`.

---

### ШАГ 11: Проверка AgentRepository.getMemoryTypes ✅
- [x] Открыть `AgentRepository.kt` и `AgentRepositoryImpl.kt`
- [x] Метод отсутствовал — добавлен:
  - В интерфейс: `suspend fun getMemoryTypes(agentId: Long): List<MemoryType>`
  - В impl: `agentMemoryDao.getMemoryTypesForAgent(agentId)` → маппинг String → MemoryType через `MemoryType.entries.find`

---

### ШАГ 12: Проверка и тестирование команд

- [ ] `@@talk(profile --create TestUser)` → профиль создан
- [ ] `@@talk(profile --bind TestUser)` → профиль привязан к пользователю
- [ ] `@@talk(profile --add_fact skills:Kotlin)` → факт добавлен
- [ ] `@@talk(profile --show_facts)` → показаны факты
- [ ] `@@talk(profile --avatar generate)` → аватар сгенерирован и сохранён
- [ ] `@@talk(profile --avatar show)` → аватар показан
- [ ] `@@talk(profile --avatar reset)` → аватар сброшен
- [ ] `@@talk(profile --unbind)` → профиль отвязан
- [ ] `@@talk(profile --remove TestUser)` → профиль удалён
- [ ] `@@talk(agent_memory --list)` → список типов памяти
- [ ] `@@talk(agent_memory --add UserProfile)` → тип добавлен
- [ ] `@@talk(agent_memory --remove UserProfile)` → тип удалён
- [ ] `UserProfileMemoryProvider.getMemoryContext()` возвращает факты когда профиль привязан
- [ ] `UserProfileMemoryProvider.getMemoryContext()` возвращает emptyList() без профиля

---

## Важные детали реализации

### Парсинг `--add_fact key:category:text`
```kotlin
private fun parseFact(raw: String?): Triple<String, String, String>? {
    val parts = raw?.split(":") ?: return null
    if (parts.size < 3) return null
    return Triple(parts[0], parts[1], parts.drop(2).joinToString(":"))
}
```

### Получение userId из системы
```kotlin
// UserType.User.ordinal == 0 (первый в enum)
val userEntity = userDao.getUserByType(UserType.User.ordinal)
    ?: return CommandResult.Error("Пользователь не найден")
```

### Prompt для генерации аватара 30x30
```
Создай текстовый ASCII-аватар размером ровно 30 строк по 30 символов.
Аватар должен отражать личность пользователя на основе его фактов:
<список фактов>
Если фактов нет — создай нейтральный безликий аватар.
Возвращай ТОЛЬКО текст аватара, без пояснений.
```

### Маппер UserProfileEntity → UserProfile
```kotlin
internal fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    title = title,
    ltmGroupId = memoryGroupId,
    textAvatar = textAvatar
)
```

---

## Порядок выполнения (оптимальный)
1. ШАГ 1 (исправить Entity + DB)
2. ШАГ 2-3 (domain model + repository interface)
3. ШАГ 4 (DAO)
4. ШАГ 5 (RepositoryImpl)
5. ШАГ 10.2-10.3 (DI для repository)
6. ШАГ 6 (Use Cases по порядку: GetCurrentUserProfile → остальные)
7. ШАГ 7 (UserProfileMemoryProvider)
8. ШАГ 8 (ProfileCommandHandler)
9. ШАГ 9 (AgentMemoryCommandHandler)
10. ШАГ 11 (проверка AgentRepository)
11. ШАГ 10.1, 10.4 (остальной DI)
12. ШАГ 12 (тестирование)
