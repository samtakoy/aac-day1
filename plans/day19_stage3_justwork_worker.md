# Этап 3: JustWorkWorker и AgentSystemPromptMemoryProvider

## Общее описание

Создание универсального worker для использования агентов с произвольными настройками из любого места кода, а также MemoryProvider для системных промптов агентов.

**Цель этапа:** Реализовать механизм создания и настройки агентов вне контекста команд чата (как это делает TalkWorker).

---

## Задачи этапа

### Шаг 3.1: AgentSystemPromptMemoryProvider

**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AgentSystemPromptMemoryProvider.kt`

**Описание:** MemoryProvider для доставки системного промпта агента в контекст LLM.

**Константы:**
```kotlin
companion object {
    const val MEMORY_KEY = "settings"
    const val CATEGORY = "systemPrompt"
}
```

**Интерфейс:** `MemoryProvider`

**Методы:**
```kotlin
class AgentSystemPromptMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository
) : MemoryProvider {
    
    private var agentId: Long? = null
    
    fun bindAgentId(agentId: Long)
    
    override suspend fun getMemoryContext(): List<AContextMessage>
    
    // Внутренние методы
    private suspend fun getSystemPrompt(agentId: Long): String?
    private fun parseSystemPrompt(json: String): String
}
```

**Логика работы `getMemoryContext()`:**
1. Проверяет наличие agentId
2. Извлекает факт из AgentMemoryRepository по ключу (MEMORY_KEY, CATEGORY)
3. Если промпт найден — возвращает `AContextMessage` с ролью SYSTEM
4. Если промпта нет — возвращает пустой список

**Формат хранения:** Plain text системного промпта

**Важно:** 
- Provider НЕ имеет публичных методов `setSystemPrompt`/`getSystemPrompt`
- Настройка происходит через `AgentMemoryRepository.upsertFact` напрямую
- Аналогично подходу в `AgentRulesMemoryProvider`

---

### Шаг 3.2: JustWorkConfig

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/JustWorkConfig.kt`

**Описание:** Параметры для создания агента через JustWorkWorker.

**Структура:**
```kotlin
data class JustWorkConfig(
    val agentName: String,           // Имя агента для идентификации
    val chatId: Long,                // ID чата для нотификаций
    val systemPrompt: String,        // Системный промпт для настройки
    val allowedTools: List<String>,  // Список разрешенных tools
    val defaultModel: () -> ModelSettings,
    val defaultContext: () -> AContext
)
```

**Важно:**
- `onCreateCallback` НЕ входит в JustWorkConfig
- Config содержит только данные для настройки
- Callback определяется в месте вызова `aiAgentFactory.getOrCreate`

---

### Шаг 3.3: JustWorkWorker

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/JustWorkWorker.kt`

**Описание:** Worker для создания и использования агентов с произвольными настройками.

**Интерфейс:** `AWorker`

**Методы:**
```kotlin
class JustWorkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val json: Json
) : AWorker {
    
    suspend fun doWork(
        config: JustWorkConfig,
        userPrompt: String,
        userRole: AContextMessage.Role = AContextMessage.Role.USER,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    )
    
    // Внутренние методы
    private suspend fun processMessage(
        config: JustWorkConfig,
        userPrompt: String,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    )
    
    private fun formatToolResult(raw: String): String
}
```

**Логика работы:**

1. **Создание агента с onCreateCallback:**
   ```kotlin
   val agent = aiAgentFactory.getOrCreate(
       systemName = config.agentName,
       chatId = config.chatId,
       systemPrompt = "",  // ПУСТО! systemPrompt настраивается через onCreateCallback
       defaultModel = config.defaultModel,
       defaultContext = config.defaultContext,
       onCreateCallback = { agentId ->
           // Применяем настройки только при первом создании агента
           // Настройка systemPrompt
           agentMemoryRepository.upsertFact(
               agentId = agentId,
               memoryKey = "settings",
               category = "systemPrompt",
               fact = config.systemPrompt
           )
           
           // Настройка allowedTools
           if (config.allowedTools.isNotEmpty()) {
               val toolsJson = Json.encodeToString(config.allowedTools)
               agentMemoryRepository.upsertFact(
                   agentId = agentId,
                   memoryKey = "settings",
                   category = "tools",
                   fact = toolsJson
               )
           }
       }
   )
   ```

2. **Обработка события:**
   - ToolCallStarted — нотификация в чат
   - ToolCallFinished — нотификация с результатом
   - Прочие события — прокси в onEvent

3. **Обработка результата:**
   - requestDebugInfo — в info сообщения
   - reportMessage — в info сообщения
   - responseText — в бот сообщения

**Отличия от TalkWorker:**
- Не обрабатывает команды чата
- Принимает настройки агента как параметры через JustWorkConfig
- Поддерживает onCreateCallback для первоначальной настройки агента
- Может использоваться из любого места (не только из TalkWorker)

---

### Шаг 3.4: Модификация AgentRepository

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/repository/AgentRepository.kt`

**Описание:** Добавление поддержки onCreateCallback в метод getOrCreateAgent.

**Изменения:**

```kotlin
interface AgentRepository {
    // ... существующие методы
    
    /**
     * Gets or creates an agent with the given system name.
     * 
     * @param systemName System name of the agent
     * @param chatId Chat ID for binding
     * @param systemPrompt System prompt (deprecated, use AgentSystemPromptMemoryProvider)
     * @param defaultModel Default model settings factory
     * @param defaultContext Default context factory
     * @param onCreateCallback Callback invoked ONLY when a new agent is created
     * @return AgentConfig
     */
    suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: ((Long) -> Unit)? = null
    ): AgentConfig
}
```

**Реализация в AgentRepositoryImpl:**

```kotlin
class AgentRepositoryImpl @Inject constructor(
    // ... зависимости
) : AgentRepository {
    
    override suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: ((Long) -> Unit)? = null
    ): AgentConfig {
        // Проверяем наличие агента в БД
        val existing = agentDao.getAgentBySystemName(systemName)
        
        if (existing != null) {
            // Агент уже существует - НЕ вызываем onCreateCallback
            return existing.toDomain()
        }
        
        // Создаем нового агента
        val newAgent = // ... логика создания
        
        // Вставляем в БД
        val agentId = agentDao.insert(newAgentEntity)
        
        // Вызываем callback ТОЛЬКО при создании
        onCreateCallback?.invoke(agentId)
        
        return newAgent.toDomain()
    }
}
```

**Важно:**
- `onCreateCallback` вызывается ТОЛЬКО если агент создан впервые
- Если агент уже существует в БД — callback НЕ вызывается
- Это гарантирует однократную настройку при первом создании

---

### Шаг 3.5: Модификация AIAgentFactory

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgentFactory.kt`

**Описание:** Проброс onCreateCallback в AgentRepository.

**Изменения:**

```kotlin
class AIAgentFactory @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val strategyFactory: StrategyFactory,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator
) {
    suspend fun getOrCreate(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: ((Long) -> Unit)? = null
    ): AIAgent {
        val config = getOrCreateAgentUseCase(
            systemName = systemName,
            chatId = chatId,
            systemPrompt = systemPrompt,
            defaultModel = defaultModel,
            defaultContext = defaultContext,
            onCreateCallback = onCreateCallback  // Пробрасываем в use case
        )
        
        val strategy = strategyFactory.create(config.contextStrategyType)
        val memoryProvider = memoryProviderFactory.create(
            memoryTypes = config.memoryTypes,
            agentId = config.id
        )
        
        return AIAgent(
            config,
            contextRepository,
            llmRequestUseCase,
            strategy,
            memoryProvider,
            toolProvider,
            toolCallOrchestrator
        )
    }
}
```

---

### Шаг 3.6: Модификация GetOrCreateAgentUseCase

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/usecase/GetOrCreateAgentUseCase.kt`

**Описание:** Проброс onCreateCallback в AgentRepository.

**Изменения:**

```kotlin
class GetOrCreateAgentUseCase @Inject constructor(
    private val agentRepository: AgentRepository
) {
    suspend operator fun invoke(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: ((Long) -> Unit)? = null
    ): AgentConfig {
        return agentRepository.getOrCreateAgent(
            systemName = systemName,
            chatId = chatId,
            systemPrompt = systemPrompt,
            defaultModel = defaultModel,
            defaultContext = defaultContext,
            onCreateCallback = onCreateCallback
        )
    }
}
```

---

### Шаг 3.7: MemoryProviderFactory обновление

**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/base/MemoryProviderFactory.kt`

**Описание:** Добавление поддержки AgentSystemPromptMemoryProvider и AgentToolsMemoryProvider.

**Изменения:**

```kotlin
class MemoryProviderFactory @Inject constructor(
    private val userProfileMemoryProvider: UserProfileMemoryProvider,
    private val agentRulesMemoryProvider: AgentRulesMemoryProvider,
    private val toolCallHelperMemoryProvider: ToolCallHelperMemoryProvider,
    private val agentSystemPromptMemoryProvider: AgentSystemPromptMemoryProvider,
    private val agentToolsMemoryProvider: AgentToolsMemoryProvider
) {
    fun create(
        memoryTypes: List<MemoryType>,
        agentId: Long? = null
    ): MemoryProvider {
        val providers = mutableListOf<MemoryProvider>()
        
        // Добавляем системный промпт и tools если есть agentId
        agentId?.let {
            providers.add(agentSystemPromptMemoryProvider.apply { bindAgentId(it) })
            providers.add(agentToolsMemoryProvider.apply { bindAgentId(it) })
        }
        
        // Существующие memory providers по типам
        memoryTypes.forEach { type ->
            when (type) {
                MemoryType.UserProfile -> providers.add(userProfileMemoryProvider)
                MemoryType.Chat -> Unit // Не поддерживается
                MemoryType.ChatGroup -> Unit // Не поддерживается
                MemoryType.AgentRules -> {
                    agentId?.let { 
                        agentRulesMemoryProvider.bindAgentId(it)
                        providers.add(agentRulesMemoryProvider)
                    }
                }
            }
        }
        
        // Всегда добавляем tool call helper
        providers.add(toolCallHelperMemoryProvider)
        
        return CompositeMemoryProvider(providers)
    }
}
```

---

### Шаг 3.8: DI модули

**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/di/MemoryCoreFeatureModule.kt`

**Описание:** Добавление новых MemoryProvider в Dagger модуль.

**Изменения:**

```kotlin
@Module
internal interface MemoryCoreFeatureModule {
    // ... существующие binds
    
    @Binds
    @Singleton
    fun bindsAgentSystemPromptMemoryProvider(impl: AgentSystemPromptMemoryProvider): MemoryProvider
    
    @Binds
    @Singleton
    fun bindsAgentToolsMemoryProvider(impl: AgentToolsMemoryProvider): MemoryProvider
}
```

**Важно:**
- У классов `@Inject constructor` — не нужно создавать `provide` методы
- Только `@Binds` для интерфейсов

---

## Резюме этапа

**Что получим:**
- ✅ AgentSystemPromptMemoryProvider для системных промптов (MEMORY_KEY = "settings")
- ✅ JustWorkConfig для параметров агента (без callback)
- ✅ JustWorkWorker для работы с агентами вне контекста чата
- ✅ Поддержка onCreateCallback в AgentRepository (вызов только при создании)
- ✅ Проброс callback через AIAgentFactory → GetOrCreateAgentUseCase → AgentRepository

**Критерии успеха:**
1. AgentSystemPromptMemoryProvider использует MEMORY_KEY = "settings", CATEGORY = "systemPrompt"
2. JustWorkConfig не содержит onCreateCallback
3. onCreateCallback вызывается ТОЛЬКО при первом создании агента
4. Если агент уже существует в БД — callback НЕ вызывается
5. JustWorkWorker корректно использует JustWorkConfig и onCreateCallback

---

## Зависимости от других этапов

- ✅ Зависит от Этапа 2 (AgentToolsMemoryProvider)
- ⚠️ Этап 4 использует JustWorkWorker для инструментов

---

## План реализации (подробный)

1. Создать AgentSystemPromptMemoryProvider.kt (MEMORY_KEY = "settings")
2. Создать JustWorkConfig.kt (без onCreateCallback)
3. Создать JustWorkWorker.kt
4. Обновить AgentRepository.kt (добавить onCreateCallback параметр)
5. Обновить AgentRepositoryImpl.kt (вызов callback только при создании)
6. Обновить AIAgentFactory.kt (проброс callback)
7. Обновить GetOrCreateAgentUseCase.kt (проброс callback)
8. Обновить MemoryProviderFactory.kt
9. Обновить MemoryCoreFeatureModule.kt (@Binds для новых providers)
10. Собрать проект и проверить компиляцию
11. Протестировать создание агента через JustWorkWorker
