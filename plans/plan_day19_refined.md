# День 19: Композиция MCP-инструментов — Уточненный план реализации

## Продуктовая задача

Создание автоматического пайплайна из нескольких MCP-инструментов для исследования файлов из GitHub.

**Цель:** Реализовать цепочку инструментов:
1. Первый инструмент получает данные (список файлов)
2. Второй обрабатывает (выбирает файл по описанию)
3. Третий сохраняет результат (анализ файла)

**Критерии успеха:**
- Автоматическое выполнение цепочки
- Корректность передачи данных между инструментами

---

## Архитектурное решение

### Используемые компоненты

- **LocalMcpService** — локальные MCP-инструменты (аналогично setReminderTool)
- **McpServer** — GitHub MCP-сервер с инструментами
- **JustWorkWorker** — новый worker для создания агентов с произвольными настройками
- **AgentMemoryRepository** — хранение настроек агентов (tools, systemPrompt)

---

## Новые сущности

### 1. Таблицы Room (Data Layer)

#### GitFileCacheEntity
**Назначение:** Кеширование списка файлов GitHub для избежания лишних запросов к API.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long (PK) | Уникальный идентификатор |
| fileListJson | String | JSON массив путей к файлам |
| createdAt | Long | Timestamp создания записи |
| expiresAt | Long | Timestamp истечения кеша |

**Таблица:** `git_file_cache`

#### FileAnalysisEntity
**Назначение:** Кеширование результатов анализа файлов.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long (PK) | Уникальный идентификатор |
| filePath | String (UNIQUE) | Полный путь к файлу |
| content | String | Результат анализа |
| createdAt | Long | Timestamp создания |

**Таблица:** `file_analysis`

---

### 2. DAO интерфейсы (Data Layer)

#### GitFileCacheDao
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/`

**Методы:**
- `suspend fun getCachedFileList(): GitFileCacheEntity?`
- `suspend fun insertCache(fileListJson: String, expiresAt: Long)`
- `suspend fun clearCache()`
- `fun getCachedFileListAsFlow(): Flow<GitFileCacheEntity?>`

#### FileAnalysisDao
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/`

**Методы:**
- `suspend fun getAnalysis(filePath: String): FileAnalysisEntity?`
- `suspend fun insertAnalysis(filePath: String, content: String)`
- `suspend fun deleteAnalysis(filePath: String)`
- `fun getAllAnalysesAsFlow(): Flow<List<FileAnalysisEntity>>`

---

### 3. Domain модели

#### GitFileCache
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/model/`

```
data class GitFileCache(
    val id: Long,
    val fileListJson: String,
    val createdAt: Long,
    val expiresAt: Long
)
```

#### FileAnalysis
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/model/`

```
data class FileAnalysis(
    val id: Long,
    val filePath: String,
    val content: String,
    val createdAt: Long
)
```

---

### 4. MemoryProvider (Domain Layer)

#### AgentToolsMemoryProvider
**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/`

**Назначение:** Управление списком разрешенных MCP-инструментов для агента.

**Константы:**
- `MEMORY_KEY = "settings"`
- `CATEGORY = "tools"`

**Методы:**
- `fun bindAgentId(agentId: Long)`
- `suspend fun getMemoryContext(): List<AContextMessage>` — возвращает системное сообщение со списком tools

**Логика:**
- Если в памяти агента есть запись с категорией "tools" — агент может использовать только указанные инструменты
- Если записи нет — агент использует все доступные инструменты (текущее поведение)

**Формат хранения:** JSON массив строк `["tool_name_1", "tool_name_2"]`

---

#### AgentSystemPromptMemoryProvider
**Расположение:** `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/`

**Назначение:** Доставка системного промпта агента в контекст LLM.

**Константы:**
- `MEMORY_KEY = "settings"`
- `CATEGORY = "systemPrompt"`

**Методы:**
- `fun bindAgentId(agentId: Long)`
- `suspend fun getMemoryContext(): List<AContextMessage>`

**Логика:**
- Извлекает системный промпт из AgentMemoryRepository по ключу (MEMORY_KEY, CATEGORY)
- Возвращает как AContextMessage с ролью SYSTEM

**Важно:** Provider НЕ имеет публичных методов set/get — настройка происходит через AgentMemoryRepository напрямую.

**Формат хранения:** Plain text системного промпта

---

### 5. CommandHandler (Domain Layer)

#### AgentToolsCommandHandler
**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/innercommand/handler/`

**Назначение:** Обработка команд управления доступом к tools для TalkWorker.

**commandName:** `"agent"`

**Поддерживаемые команды:**

| Команда | Описание |
|---------|----------|
| `--addtool tool_name` | Добавить инструмент в список разрешенных |
| `--listtools` | Вывести список разрешенных инструментов |
| `--cleartools` | Очистить список разрешенных инструментов |

**Зависимости:**
- `AIAgentFactory` — для получения агента
- `AgentMemoryRepository` — для хранения списка tools
- `Json` — для сериализации списка

**Аналогия:** Работает полностью аналогично `AgentCommandHandler` (--addrule/--listrules/--clearrules)

---

### 6. JustWorkWorker (Domain Layer)

#### JustWorkConfig
**Назначение:** Параметры для создания агента через JustWorkWorker.

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

---

#### JustWorkWorker
**Расположение:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/`

**Назначение:** Worker для создания и использования агентов с произвольными настройками из любого места (не только из команд чата).

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
}
```

**Логика работы:**
```kotlin
val agent = aiAgentFactory.getOrCreate(
    systemName = config.agentName,
    chatId = config.chatId,
    systemPrompt = "",  // ПУСТО! systemPrompt настраивается через onCreateCallback
    defaultModel = config.defaultModel,
    defaultContext = config.defaultContext,
    onCreateCallback = { agentId ->
        // Применяем настройки только при первом создании агента
        agentMemoryRepository.upsertFact(
            agentId = agentId,
            memoryKey = "settings",
            category = "systemPrompt",
            fact = config.systemPrompt
        )
        agentMemoryRepository.upsertFact(
            agentId = agentId,
            memoryKey = "settings",
            category = "tools",
            fact = Json.encodeToString(config.allowedTools)
        )
    }
)
```

**Отличия от TalkWorker:**
- Не обрабатывает команды чата
- Принимает настройки агента как параметры через JustWorkConfig
- Поддерживает onCreateCallback для первоначальной настройки агента
- Может использоваться из любого места (не только из TalkWorker)

---

### 7. LocalMcpService расширения (Data Layer)

#### InvestigateGitFileTool
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/`

**Назначение:** Исследование файла GitHub по описанию пользователя.

**Параметры:**
- `file_request_message: String` (required) — описание файла от пользователя

**Возвращаемый ответ:**
```json
{
  "content": "результат работы или null",
  "error": "текст ошибки"
}
```

**Логика работы:**
1. Создает JustWorkConfig для агента "git_file_investigator"
2. Использует JustWorkWorker для создания/получения агента
3. Агент использует инструменты:
   - `get_git_file_list` — получение списка файлов
   - `get_file_analysis` — получение анализа файла
4. Возвращает результат пользователю

**Системный промпт агента (из config):**
```
Тебе доступны инструменты:
- get_git_file_list для получения списка файлов
- get_file_analysis для получения анализа по файлу

Действуй строго последовательно и прямолинейно:
1. Получи список файлов с помощью get_git_file_list
2. Найди в списке файл (включая полный путь) наиболее подходящий под описание пользователя
3. Используй get_file_analysis для получения анализа по файлу
4. Скажи пользователю полное имя файла и текст полученного анализа
```

---

#### GetFileAnalysisTool
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/`

**Назначение:** Получение анализа по полному имени файла (с кешированием).

**Параметры:**
- `file_full_path: String` (required) — полный путь к файлу

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "file_full_path": "file_full_path",
  "content": "текстовый анализ файла",
  "error_text": "текст ошибки"
}
```

**Логика работы:**
1. Проверяет наличие в FileAnalysisEntity
2. Если найдено — возвращает из кеша
3. Если не найдено:
   - Скачивает файл содержимое (через GitHub API)
   - Ищет MCP tool с именем `analyze_code_content`
   - Выполняет tool для file_full_path
   - Сохраняет результат в FileAnalysisEntity
   - Возвращает результат

---

#### AnalyzeCodeContentTool
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/`

**Назначение:** Анализ содержимого файла и возврат обобщенной выжимки.

**Параметры:**
- `content: String` (required) — содержимое файла для анализа

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "analysis_result": "результат работы или null",
  "error": "текст ошибки"
}
```

**Логика работы:**
1. Создает JustWorkConfig для агента "content_analyzer"
2. Использует JustWorkWorker для создания/получения агента
3. Передает системный промпт с задачей анализа

**Системный промпт агента (из config):**
```
Ты Kotlin Senior Developer, с многолетним опытом разработки и построения больших, но понятных и расширяемых систем; фанат Clean Architecture, SOLID, Design Patterns и best coding practicles.
Твоя задача проанализировать текст, который тебе принес пользователь. Выдать какое-то резюме: короткое описание содержимого, что хорошо, что плохо, рекомендации.
```

---

### 8. McpServer расширения

#### get_git_file_list tool
**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/`

**Назначение:** Получение списка всех полных имен файлов из Git.

**Параметры:** Нет (owner и repo берутся из env)

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "content": [], // список полных имен файлов (/путь/имя.расширение)
  "error": "текст ошибки"
}
```

**Логика работы:**
1. Запрашивает список всех файлов у GitHub API
2. Возвращает список путей

---

#### reset_git_file_list_cache tool
**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/`

**Назначение:** Сброс кеша файлов Git.

**Параметры:** Нет

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "content": [],
  "error": "текст ошибки"
}
```

---

### 9. UseCase (Domain Layer)

#### GetGitFileListUseCase
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/usecase/`

**Назначение:** Логика получения списка файлов с кешированием.

**Методы:**
- `suspend fun execute(): Result<List<String>>`
- `suspend fun clearCache(): Result<Unit>`

**Зависимости:**
- `McpTools` — для вызова remote tool
- `GitFileCacheRepository` — для работы с кешем

---

#### GetFileAnalysisUseCase
**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/usecase/`

**Назначение:** Логика получения анализа файла с кешированием.

**Методы:**
- `suspend fun execute(filePath: String): Result<FileAnalysis>`

**Зависимости:**
- `FileAnalysisRepository` — для работы с кешем
- `McpTools` — для вызова analyze_code_content

---

## Схема взаимодействия компонентов

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Input                                │
│            "@@talk Найди и изучи файл FileName с гитхаба"        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         TalkWorker                               │
│  - Обрабатывает команду @@talk                                   │
│  - Использует агента "talk_agent"                                │
│  - McpToolProvider фильтрует tools по настройкам агента          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    investigate_git_file                          │
│                   (LocalMcpService tool)                         │
│  - Принимает file_request_message                                │
│  - Использует JustWorkWorker для создания агента                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    JustWorkWorker                                │
│  - Создает JustWorkConfig для "git_file_investigator"            │
│  - Вызывает aiAgentFactory.getOrCreate с onCreateCallback        │
│  - onCreateCallback настраивает systemPrompt и tools             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 git_file_investigator agent                      │
│  - Использует get_git_file_list                                  │
│  - Использует get_file_analysis                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│   get_git_file_list     │     │   get_file_analysis     │
│    (McpServer tool)     │     │  (LocalMcpService tool) │
│  - GitHub API           │     │  - Проверяет кеш        │
│  - Возвращает список    │     │  - Вызывает             │
│                         │     │    analyze_code_content │
└─────────────────────────┘     └─────────────────────────┘
                                            │
                                            ▼
                                  ┌─────────────────────────┐
                                  │  analyze_code_content   │
                                  │  (LocalMcpService tool) │
                                  │  - content_analyzer     │
                                  │    агент                │
                                  └─────────────────────────┘
```

---

## Этапы реализации

| Этап | Название | Краткое описание |
|------|----------|------------------|
| 1 | Инфраструктура данных | Room сущности, DAO, мапперы |
| 2 | Управление доступом к tools | AgentToolsMemoryProvider, AgentToolsCommandHandler, McpToolProvider |
| 3 | JustWorkWorker и AgentSystemPromptMemoryProvider | Worker для агентов с настройками, MemoryProvider для промптов |
| 4 | LocalMcpService расширения | InvestigateGitFileTool, GetFileAnalysisTool, AnalyzeCodeContentTool |
| 5 | McpServer расширения | get_git_file_list, reset_git_file_list_cache |
| 6 | Интеграция и тестирование | Настройка промптов, тестирование пайплайна |

---

## Файлы этапов

Для каждого этапа создан отдельный файл в директории `plans/`:

| Файл | Этап | Статус |
|------|------|--------|
| `plans/day19_stage1_data_infrastructure.md` | Инфраструктура данных | ✅ Создан |
| `plans/day19_stage2_tools_access.md` | Управление доступом к tools | ✅ Создан |
| `plans/day19_stage3_justwork_worker.md` | JustWorkWorker и AgentSystemPromptMemoryProvider | ✅ Создан |
| `plans/day19_stage4_local_mcp_tools.md` | LocalMcpService расширения | ✅ Создан |
| `plans/day19_stage5_mcp_server_tools.md` | McpServer расширения | ✅ Создан |
| `plans/day19_stage6_integration.md` | Интеграция и тестирование | ✅ Создан |

Каждый файл этапа содержит:
- Общее описание этапа
- Детальное описание каждой задачи (сущности, методы, параметры)
- Резюме этапа (что получим, критерии успеха)
- Зависимости от других этапов
- Подробный план реализации (пошагово)
