# Этап 2: Android Data Layer — BuildConfig, DataStore, Repositories, DI

## Общее описание

Создание data-слоя для PR Handle фичи: хранение настроек (включен ли мониторинг, к какому чату привязан, последний Telegram update_id) и получение Telegram-обновлений.

**Зависимости:** Этап 1 (для тестирования), но код Android можно писать параллельно.

**Что получим:**
- `AppSettings` расширен тремя новыми DataStore ключами для PR Handle
- `PrHandleRepository` для чтения/записи состояния мониторинга
- `TelegramRepository` для опроса Telegram Bot API
- DI модуль, связывающий всё вместе, и новые методы в AppComponent

**Критерии успеха:**
- `PrHandleRepository.setPrHandleState(true, chatId=1)` → `getPrHandleStateFlow()` возвращает `PrHandleState(isEnabled=true, chatId=1)`
- `TelegramRepository.getPrUpdates(0)` успешно возвращает список обновлений из реального Telegram-бота (проверяется вручную в тестовом чате или юнит-тестом с моком)

---

## Задача 2.1: BuildConfig — новые токены

### Файл для изменения

**`app/build.gradle.kts`**

### Описание

По аналогии с существующим `LLM_API_KEY`, добавить два новых поля в `BuildConfig`.

### Детали реализации

В блоке `android { defaultConfig { ... } }` добавить после существующего `buildConfigField("String", "LLM_API_KEY", ...)`:

```kotlin
val telegramBotToken = localProperties.getProperty("TELEGRAM_BOT_TOKEN") ?: ""
buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramBotToken\"")

val telegramChatId = localProperties.getProperty("TELEGRAM_CHAT_ID") ?: ""
buildConfigField("String", "TELEGRAM_CHAT_ID", "\"$telegramChatId\"")
```

### Файл `local.properties` (не коммитить!)

Добавить строки (не коммитить в git, файл уже в .gitignore):
```
TELEGRAM_BOT_TOKEN=123456789:AAABBBCCC...
TELEGRAM_CHAT_ID=-100123456789
```

**Важно:** `TELEGRAM_CHAT_ID` — это Telegram chat_id (НЕ chatId Android приложения). Для личного чата — положительное число. Для групп — отрицательное (начинается с `-100...`).

---

## Задача 2.2: Расширение AppSettings — новые DataStore ключи

### Файл для изменения

**`app/src/main/java/com/example/day/core/app_settings/AppSettings.kt`**

### Текущее состояние файла

Файл содержит `@Singleton class AppSettings @Inject constructor(private val context: Context)`. Хранит `LOCAL_SERVER_URL_KEY`. Использует `preferencesDataStore(name = "app_settings")`.

### Что добавить

#### Новые ключи PreferencesKeys

```kotlin
private val HANDLE_PR_ENABLED_KEY = booleanPreferencesKey("handle_pr_enabled")
private val HANDLE_PR_CHAT_ID_KEY = longPreferencesKey("handle_pr_chat_id")
private val TELEGRAM_LAST_UPDATE_ID_KEY = longPreferencesKey("telegram_last_update_id")
private val HANDLE_PR_MODEL_SETTINGS_KEY = stringPreferencesKey("handle_pr_model_settings")
```

Импорт: `androidx.datastore.preferences.core.*` — все типы ключей уже доступны.

#### Новые методы

```kotlin
// Flow с текущим состоянием HandlePr
val prHandleStateFlow: Flow<PrHandleState> = context.appSettingsDataStore.data.map { prefs ->
    val modelSettingsJson = prefs[HANDLE_PR_MODEL_SETTINGS_KEY]
    val modelSettings = modelSettingsJson
        ?.let { runCatching { Json.decodeFromString<PrModelSettingsDto>(it).toDomain() }.getOrNull() }
    PrHandleState(
        isEnabled = prefs[HANDLE_PR_ENABLED_KEY] ?: false,
        chatId = prefs[HANDLE_PR_CHAT_ID_KEY] ?: -1L,
        modelSettings = modelSettings
    )
}

suspend fun setPrHandleState(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings?) {
    context.appSettingsDataStore.edit { prefs ->
        prefs[HANDLE_PR_ENABLED_KEY] = isEnabled
        prefs[HANDLE_PR_CHAT_ID_KEY] = chatId
        if (modelSettings != null) {
            prefs[HANDLE_PR_MODEL_SETTINGS_KEY] = Json.encodeToString(modelSettings.toDto())
        } else {
            prefs.remove(HANDLE_PR_MODEL_SETTINGS_KEY)
        }
    }
}

suspend fun getLastTelegramUpdateId(): Long {
    return context.appSettingsDataStore.data.first()[TELEGRAM_LAST_UPDATE_ID_KEY] ?: 0L
}

suspend fun saveLastTelegramUpdateId(id: Long) {
    context.appSettingsDataStore.edit { it[TELEGRAM_LAST_UPDATE_ID_KEY] = id }
}
```

**Импорт:** `kotlinx.coroutines.flow.first`, `kotlinx.serialization.json.Json`, `kotlinx.serialization.encodeToString`, `com.example.day.core.core_features.pr_review.domain.model.PrHandleState`, `com.example.day.core.core_features.pr_review.data.PrModelSettingsDto`, `com.example.day.core.core_features.pr_review.data.toDto`, `com.example.day.core.core_features.pr_review.data.toDomain`

`Json` — тот же instance что используется в проекте. Лучше инжектировать его в `AppSettings` через конструктор: `@Inject constructor(private val context: Context, private val json: Json)` — если `Json` уже провайдится в DI (проверить в `AppModule`). Если нет — использовать `Json { ignoreUnknownKeys = true }` как локальный instance.

---

## Задача 2.3: Domain-модели PR Handle фичи

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/model/PrHandleState.kt`**

```kotlin
data class PrHandleState(
    val isEnabled: Boolean,
    val chatId: Long,
    val modelSettings: ModelSettings?  // настройки модели чата, сохранённые при включении
)
```

`ModelSettings` — `com.example.day.core.core_features.llm.domain.model.ModelSettings`

### Файл для создания: DTO для сериализации ModelSettings

**`app/src/main/java/com/example/day/core/core_features/pr_review/data/PrModelSettingsDto.kt`**

`ModelSettings` не помечен `@Serializable` и содержит `ImmutableList`. Для хранения в DataStore (JSON-строка) используем простой DTO:

```kotlin
@Serializable
internal data class PrModelSettingsDto(
    val name: String,
    val maxTokens: Int? = null,
    val maxCompletionTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val seed: Int? = null,
    val reasoningEffort: String? = null,
    val isLocal: Boolean = false
)
```

Два вспомогательных extension-метода (в том же файле или рядом):

```kotlin
internal fun ModelSettings.toDto() = PrModelSettingsDto(
    name = name,
    maxTokens = maxTokens,
    maxCompletionTokens = maxCompletionTokens,
    temperature = temperature,
    topP = topP,
    topK = topK,
    presencePenalty = presencePenalty,
    frequencyPenalty = frequencyPenalty,
    seed = seed,
    reasoningEffort = reasoningEffort,
    isLocal = isLocal
)

internal fun PrModelSettingsDto.toDomain() = ModelSettings(
    name = name,
    maxTokens = maxTokens,
    maxCompletionTokens = maxCompletionTokens,
    temperature = temperature,
    topP = topP,
    topK = topK,
    presencePenalty = presencePenalty,
    frequencyPenalty = frequencyPenalty,
    seed = seed,
    reasoningEffort = reasoningEffort,
    isLocal = isLocal
)
```

`stopSequence` не переносим в DTO (не используется в ревью), `ImmutableList` не сериализуется стандартно.

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/model/TelegramPrEvent.kt`**

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class TelegramPrEvent(
    val updateId: Long,
    val prNumber: Int,
    val repo: String,
    val title: String
)
```

---

## Задача 2.4: PrHandleRepository

### Файл для создания (interface)

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/repository/PrHandleRepository.kt`**

```kotlin
interface PrHandleRepository {
    fun getPrHandleStateFlow(): Flow<PrHandleState>
    suspend fun setPrHandleState(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings?)
    suspend fun getLastTelegramUpdateId(): Long
    suspend fun saveLastTelegramUpdateId(id: Long)
}
```

### Файл для создания (impl)

**`app/src/main/java/com/example/day/core/core_features/pr_review/data/PrHandleRepositoryImpl.kt`**

- Аннотация: `@Singleton`, `internal`
- Инжектирует: `AppSettings`
- Делегирует все методы в соответствующие методы `AppSettings`
- `setPrHandleState(isEnabled, chatId, modelSettings)` → вызывает `appSettings.setPrHandleState(isEnabled, chatId, modelSettings)`

---

## Задача 2.5: TelegramRepository

### Файл для создания (interface)

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/repository/TelegramRepository.kt`**

```kotlin
interface TelegramRepository {
    suspend fun getPrUpdates(offset: Long): Result<List<TelegramPrEvent>>
}
```

### Файл для создания (impl)

**`app/src/main/java/com/example/day/core/core_features/pr_review/data/TelegramRepositoryImpl.kt`**

- Аннотация: `@Singleton`, `internal`
- Инжектирует: `HttpClient` (тот же что используется для LLM/MCP запросов), `BuildConfig` значения (через конструктор или читая `BuildConfig.TELEGRAM_BOT_TOKEN` напрямую)

#### Детали реализации `getPrUpdates(offset: Long)`

1. Выполнить HTTP GET:
   ```
   https://api.telegram.org/bot{BuildConfig.TELEGRAM_BOT_TOKEN}/getUpdates?offset={offset}&limit=10&timeout=0
   ```

2. Ожидаемый формат ответа Telegram:
   ```json
   {
     "ok": true,
     "result": [
       {
         "update_id": 123456,
         "message": {
           "chat": {"id": -100123456789},
           "text": "{\"event\":\"pr_opened\",\"repo\":\"owner/repo\",\"pr_number\":42,\"title\":\"Fix bug\"}"
         }
       }
     ]
   }
   ```

3. Алгоритм парсинга:
   - Десериализовать ответ в `TelegramUpdatesResponse`
   - Фильтровать: только сообщения из чата с `id == BuildConfig.TELEGRAM_CHAT_ID.toLong()`
   - Для каждого прошедшего фильтр: попытаться десериализовать `message.text` как JSON в `TelegramPrEventDto`
   - Проверить `dto.event == "pr_opened"`
   - Преобразовать в `TelegramPrEvent(updateId, prNumber, repo, title)`
   - Возвращает только события типа `pr_opened`

4. При любой HTTP/parse ошибке — вернуть `Result.failure(...)`

#### Вспомогательные data-классы для парсинга Telegram API

Создать внутри файла `TelegramRepositoryImpl.kt` (private / internal):

```kotlin
@Serializable
private data class TelegramUpdatesResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate> = emptyList()
)

@Serializable
private data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null
)

@Serializable
private data class TelegramMessage(
    val chat: TelegramChat,
    val text: String? = null
)

@Serializable
private data class TelegramChat(
    val id: Long
)

@Serializable
private data class TelegramPrEventDto(
    val event: String,
    val repo: String,
    @SerialName("pr_number") val prNumber: Int,
    val title: String
)
```

**Импорты:** `kotlinx.serialization.*`, `io.ktor.client.*`, `io.ktor.client.request.*`, `io.ktor.client.call.*`

---

## Задача 2.6: DI — PrReviewModule и расширение AppComponent

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/di/PrReviewModule.kt`**

```kotlin
@Module
internal interface PrReviewModule {
    @Binds
    @Singleton
    fun bindPrHandleRepository(impl: PrHandleRepositoryImpl): PrHandleRepository

    @Binds
    @Singleton
    fun bindTelegramRepository(impl: TelegramRepositoryImpl): TelegramRepository
}
```

### Файл для изменения

**`app/src/main/java/com/example/day/app/di/AppComponent.kt`**

1. Добавить `PrReviewModule::class` в список modules аннотации `@Component(modules = [...])`

2. Добавить новые методы для экспозиции (понадобятся в `TelegramPollingWorker`):
   ```kotlin
   fun telegramRepository(): TelegramRepository
   fun prHandleRepository(): PrHandleRepository
   ```

**Примечание:** Use cases (`StartPrReviewUseCase`, `GetPrHandleStateUseCase`, `SetPrHandleEnabledUseCase`) добавить в AppComponent в следующих этапах.

---

## Структура новых файлов этапа

```
app/src/main/java/com/example/day/
└── core/core_features/pr_review/
    ├── domain/
    │   ├── model/
    │   │   ├── PrHandleState.kt          (новый: isEnabled, chatId, modelSettings)
    │   │   └── TelegramPrEvent.kt        (новый)
    │   └── repository/
    │       ├── PrHandleRepository.kt     (новый)
    │       └── TelegramRepository.kt     (новый)
    ├── data/
    │   ├── PrHandleRepositoryImpl.kt     (новый)
    │   ├── PrModelSettingsDto.kt         (новый: DTO + toDto/toDomain extensions)
    │   └── TelegramRepositoryImpl.kt     (новый)
    └── di/
        └── PrReviewModule.kt             (новый)

app/build.gradle.kts                      (изменить — добавить BuildConfig поля)
app/src/.../app_settings/AppSettings.kt   (изменить — добавить DataStore ключи и методы)
app/src/.../app/di/AppComponent.kt        (изменить — добавить модуль + методы экспозиции)
```
