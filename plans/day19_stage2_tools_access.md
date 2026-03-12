# Этап 2: Управление доступом к tools

## Общее описание

Реализация механизма ограничения доступа агентов к MCP-инструментам на основе настроек в памяти агента.

**Цель этапа:** Добавить возможность управлять списком разрешенных инструментов для каждого агента через команды чата.

---

## Задачи этапа

### Шаг 2.1: AgentToolsMemoryProvider

**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AgentToolsMemoryProvider.kt`

**Описание:** MemoryProvider для доставки списка разрешенных tools агента в контекст LLM.

**Константы:**
```kotlin
companion object {
    const val MEMORY_KEY = "settings"
    const val CATEGORY = "tools"
}
```

**Интерфейс:** `MemoryProvider`

**Методы:**
```kotlin
class AgentToolsMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository
) : MemoryProvider {
    
    private var agentId: Long? = null
    
    fun bindAgentId(agentId: Long)
    
    override suspend fun getMemoryContext(): List<AContextMessage>
    
    // Внутренние методы
    private suspend fun getTools(agentId: Long): List<String>
    private fun parseTools(json: String): List<String>
}
```

**Логика работы `getMemoryContext()`:**
1. Проверяет наличие agentId
2. Извлекает факт из AgentMemoryRepository по ключу (MEMORY_KEY, CATEGORY)
3. Если факт найден и категория совпадает — парсит JSON массив
4. Возвращает системное сообщение формата:
   ```
   Доступные инструменты:
   - tool_name_1
   - tool_name_2
   ...
   ```
5. Если инструментов нет — возвращает пустой список

**Формат хранения:** JSON массив строк `["tool_name_1", "tool_name_2"]`

**Важно:**
- Provider НЕ имеет публичных методов set/get — настройка происходит через AgentMemoryRepository напрямую
- Аналогично подходу в AgentRulesMemoryProvider

---

### Шаг 2.2: AgentToolsCommandHandler

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/innercommand/handler/AgentToolsCommandHandler.kt`

**Описание:** Обработчик команд для управления доступом к tools.

**Интерфейс:** `CommandHandler`

**commandName:** `"agent"`

**Поддерживаемые команды:**

| Команда | Описание |
|---------|----------|
| `--addtool tool_name` | Добавить инструмент в список разрешенных |
| `--listtools` | Вывести список разрешенных инструментов |
| `--cleartools` | Очистить список разрешенных инструментов |

**Методы:**
```kotlin
class AgentToolsCommandHandler @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val agentMemoryRepository: AgentMemoryRepository
) : CommandHandler {
    
    override val commandName: String
    
    override suspend fun handle(
        params: List<Pair<String, String?>>,
        chat: Chat
    ): CommandResult
    
    // Внутренние методы
    private suspend fun handleAddTool(toolName: String?, chat: Chat): CommandResult
    private suspend fun handleListTools(chat: Chat): CommandResult
    private suspend fun handleClearTools(chat: Chat): CommandResult
    private suspend fun getCurrentTools(agentId: Long): List<String>
}
```

**Логика работы:**

#### handleAddTool
1. Проверяет наличие toolName
2. Получает текущего агента через aiAgentFactory
3. Извлекает текущий список tools
4. Добавляет новый tool (если еще не добавлен)
5. Сохраняет обновленный список как JSON

#### handleListTools
1. Получает агента
2. Извлекает текущий список tools
3. Форматирует вывод:
   ```
   Доступные инструменты (2):
   1. tool_name_1
   2. tool_name_2
   ```

#### handleClearTools
1. Получает агента
2. Вызывает `agentMemoryRepository.deleteFact(agentId, MEMORY_KEY, CATEGORY)`

---

### Шаг 2.3: Модификация McpToolProvider

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/data/tools/McpToolProvider.kt`

**Описание:** Добавление фильтрации инструментов на основе настроек агента.

**Изменения:**

1. **Добавить зависимость:**
   ```kotlin
   private val agentToolsMemoryProvider: AgentToolsMemoryProvider
   ```

2. **Модифицировать метод `getTools()`:**
   ```kotlin
   override suspend fun getTools(): List<ModelRequest.Tool> {
       val servers = getEnabledServers()
       if (servers.isEmpty()) return emptyList()
       
       // Получаем список разрешенных tools для текущего агента
       val allowedTools = agentToolsMemoryProvider.getAllowedTools()
       
       toolToServer.clear()
       val collected = mutableListOf<ModelRequest.Tool>()
       
       servers.forEach { server ->
           val tools = getConnectedTools(server.id)
           tools.forEach { tool ->
               // Проверка 1: tool в глобальном списке разрешенных
               if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(tool.name)) return@forEach
               
               // Проверка 2: tool в списке разрешенных для агента (если задан)
               if (allowedTools.isNotEmpty() && !allowedTools.contains(tool.name)) return@forEach
               
               if (toolToServer.containsKey(tool.name)) return@forEach
               toolToServer[tool.name] = server.id
               collected.add(tool.toModelRequestTool())
           }
       }
       return collected
   }
   ```

3. **Добавить метод:**
   ```kotlin
   private suspend fun getAllowedTools(): List<String> {
       // Извлекаем из AgentMemoryRepository
       // Возвращаем пустой список если нет настроек (все tools разрешены)
   }
   ```

**Логика фильтрации:**
- Если в памяти агента нет записи с категорией "tools" — все инструменты доступны
- Если запись есть — доступны только указанные инструменты
- Глобальный список `McpToolNames.ALLOWED_TOOL_NAMES` проверяется всегда

---

### Шаг 2.4: Регистрация CommandHandler

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/di/CommandHandlerModule.kt`

**Описание:** Добавление нового обработчика в Dagger модуль.

**Изменения:**
```kotlin
@Module
internal interface CommandHandlerModule {
    // ... существующие binds
    
    @Binds
    @Singleton
    fun bindsAgentToolsCommandHandler(impl: AgentToolsCommandHandler): CommandHandler
}
```

---

### Шаг 2.5: Обновление TalkWorker

**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TalkWorker.kt`

**Описание:** Добавление поддержки новых команд в документацию.

**Изменения:**
Добавить в KDoc класса описание новых команд:
```kotlin
/**
 * ...
 * - @@talk(agent --addrule "текст") - добавить новое правило диалога
 * - @@talk(agent --listrules) - вывести список всех правил диалога
 * - @@talk(agent --clearrules) - удалить все правила диалога
 * - @@talk(agent --addtool tool_name) - добавить инструмент в список разрешенных
 * - @@talk(agent --listtools) - вывести список разрешенных инструментов
 * - @@talk(agent --cleartools) - очистить список разрешенных инструментов
 * - @@talk <text> - send text to LLM
 */
```

---

## Резюме этапа

**Что получим:**
- ✅ AgentToolsMemoryProvider для управления доступом к tools
- ✅ AgentToolsCommandHandler для команд чата
- ✅ McpToolProvider с фильтрацией по настройкам агента
- ✅ Возможность управлять tools через команды @@talk(agent --addtool/--listtools/--cleartools)

**Критерии успеха:**
1. Команды `@@talk(agent --addtool tool_name)` работают корректно
2. Команда `@@talk(agent --listtools)` показывает текущий список
3. Команда `@@talk(agent --cleartools)` очищает список
4. McpToolProvider возвращает только разрешенные инструменты
5. Если tools не настроены — работают все доступные инструменты

---

## Зависимости от других этапов

- ✅ Зависит от Этапа 1 (нет, не зависит)
- ⚠️ Этап 3 использует AgentToolsMemoryProvider для настройки агентов

---

## План реализации (подробный)

1. Создать AgentToolsMemoryProvider.kt
2. Создать AgentToolsCommandHandler.kt
3. Обновить McpToolProvider.kt (добавить фильтрацию)
4. Обновить CommandHandlerModule.kt (добавить binds)
5. Обновить TalkWorker.kt (добавить документацию)
6. Протестировать команды в приложении
7. Проверить фильтрацию инструментов
