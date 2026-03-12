# Этап 4: LocalMcpService расширения

## Общее описание

Добавление новых локальных MCP-инструментов для исследования файлов GitHub:
1. InvestigateGitFileTool — поиск и анализ файла по описанию
2. GetFileAnalysisTool — получение анализа файла с кешированием
3. AnalyzeCodeContentTool — анализ содержимого файла агентом

**Цель этапа:** Реализовать инструменты для пайплайна исследования файлов.

---

## Задачи этапа

### Шаг 4.1: InvestigateGitFileTool

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/InvestigateGitFileTool.kt`

**Описание:** Локальный MCP-инструмент для исследования файла GitHub по описанию пользователя.

**Интерфейс:** `LocalMcpTool`

**Константы:**
```kotlin
companion object {
    const val TOOL_NAME = "investigate_git_file"
    const val AGENT_NAME = "git_file_investigator"
}
```

**Параметры (inputSchema):**
```json
{
  "type": "object",
  "properties": {
    "file_request_message": {
      "type": "string",
      "description": "Описание файла для поиска и анализа"
    }
  },
  "required": ["file_request_message"]
}
```

**Возвращаемый ответ:**
```json
{
  "content": "результат работы или null",
  "error": "текст ошибки"
}
```

**Методы:**
```kotlin
internal class InvestigateGitFileTool @Inject constructor(
    private val justWorkWorker: JustWorkWorker,
    private val chatTools: ChatTools
) : LocalMcpTool {
    
    override val name: String
    override val description: String
    override val inputSchema: JsonObject
    
    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String>
    
    // Внутренние методы
    private fun buildSystemPrompt(): String
}
```

**Логика работы:**

1. **Извлечение параметров:**
   ```kotlin
   val fileRequestMessage = arguments["file_request_message"]?.jsonPrimitive?.content
       ?: return Result.failure(IllegalArgumentException("file_request_message is required"))
   ```

2. **Создание JustWorkConfig:**
   ```kotlin
   val config = JustWorkConfig(
       agentName = AGENT_NAME,
       chatId = context?.chatId ?: error("chatId required"),
       systemPrompt = buildSystemPrompt(),
       allowedTools = listOf(
           McpToolNames.GET_GIT_FILE_LIST,
           McpToolNames.GET_FILE_ANALYSIS
       ),
       defaultModel = { /* модель по умолчанию */ },
       defaultContext = { AContextDefaultFactory.createFull() }
   )
   ```

3. **Системный промпт:**
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

4. **Вызов JustWorkWorker:**
   ```kotlin
   justWorkWorker.doWork(
       config = config,
       userPrompt = "Мне нужен результат анализа файла. $fileRequestMessage",
       onEvent = { event -> /* обработка событий */ }
   )
   ```

5. **Настройка агента (внутри JustWorkWorker):**
   - `aiAgentFactory.getOrCreate` вызывается с `onCreateCallback`
   - Callback срабатывает ТОЛЬКО при первом создании агента
   - Сохраняются systemPrompt и allowedTools в AgentMemoryRepository

---

### Шаг 4.2: GetFileAnalysisTool

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/GetFileAnalysisTool.kt`

**Описание:** Локальный MCP-инструмент для получения анализа файла с кешированием.

**Интерфейс:** `LocalMcpTool`

**Константы:**
```kotlin
companion object {
    const val TOOL_NAME = "get_file_analysis"
}
```

**Параметры (inputSchema):**
```json
{
  "type": "object",
  "properties": {
    "file_full_path": {
      "type": "string",
      "description": "Полный путь к файлу для анализа"
    }
  },
  "required": ["file_full_path"]
}
```

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "file_full_path": "/path/to/file.kt",
  "content": "текстовый анализ файла",
  "error_text": "текст ошибки"
}
```

**Методы:**
```kotlin
internal class GetFileAnalysisTool @Inject constructor(
    private val fileAnalysisRepository: FileAnalysisRepository,
    private val mcpTools: McpTools,
    private val githubApiClient: GitHubApiClient
) : LocalMcpTool {
    
    override val name: String
    override val description: String
    override val inputSchema: JsonObject
    
    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String>
    
    // Внутренние методы
    private suspend fun getCachedAnalysis(filePath: String): String?
    private suspend fun analyzeFile(filePath: String): String
    private suspend fun downloadFileContent(filePath: String): String
}
```

**Логика работы:**

1. **Проверка кеша:**
   ```kotlin
   val filePath = arguments["file_full_path"]?.jsonPrimitive?.content
       ?: return Result.failure(IllegalArgumentException("file_full_path is required"))
   
   val cached = getCachedAnalysis(filePath)
   if (cached != null) {
       return Result.success(buildJsonObject {
           put("status", "ok")
           put("file_full_path", filePath)
           put("content", cached)
       }.toString())
   }
   ```

2. **Анализ файла:**
   ```kotlin
   private suspend fun analyzeFile(filePath: String): String {
       // Скачиваем содержимое
       val content = downloadFileContent(filePath)
       
       // Вызываем MCP tool analyze_code_content
       val analysisResult = mcpTools.callTool(
           serverId = "local",  // или другой ID
           toolName = McpToolNames.ANALYZE_CODE_CONTENT,
           arguments = buildJsonObject {
               put("content", content)
           }
       ).getOrNull() ?: error("Analysis failed")
       
       // Сохраняем в кеш
       fileAnalysisRepository.saveAnalysis(filePath, analysisResult)
       
       return analysisResult
   }
   ```

---

### Шаг 4.3: AnalyzeCodeContentTool

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/AnalyzeCodeContentTool.kt`

**Описание:** Локальный MCP-инструмент для анализа содержимого файла агентом.

**Интерфейс:** `LocalMcpTool`

**Константы:**
```kotlin
companion object {
    const val TOOL_NAME = "analyze_code_content"
    const val AGENT_NAME = "content_analyzer"
}
```

**Параметры (inputSchema):**
```json
{
  "type": "object",
  "properties": {
    "content": {
      "type": "string",
      "description": "Содержимое файла для анализа"
    }
  },
  "required": ["content"]
}
```

**Возвращаемый ответ:**
```json
{
  "status": "ok",
  "analysis_result": "результат работы или null",
  "error": "текст ошибки"
}
```

**Методы:**
```kotlin
internal class AnalyzeCodeContentTool @Inject constructor(
    private val justWorkWorker: JustWorkWorker
) : LocalMcpTool {
    
    override val name: String
    override val description: String
    override val inputSchema: JsonObject
    
    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String>
    
    // Внутренние методы
    private fun buildSystemPrompt(): String
}
```

**Логика работы:**

1. **Системный промпт (ИЗ ИСХОДНОГО ЗАДАНИЯ, без изменений):**
   ```
   Ты Kotlin Senior Developer, с многолетним опытом разработки и построения больших, но понятных и расширяемых систем; фанат Clean Architecture, SOLID, Design Patterns и best coding practicles.
   Твоя задача проанализировать текст, который тебе принес пользователь. Выдать какое-то резюме: короткое описание содержимого, что хорошо, что плохо, рекомендации.
   ```

2. **Создание JustWorkConfig:**
   ```kotlin
   val content = arguments["content"]?.jsonPrimitive?.content
       ?: return Result.failure(IllegalArgumentException("content is required"))
   
   val config = JustWorkConfig(
       agentName = AGENT_NAME,
       chatId = context?.chatId ?: 0L,  // Может быть null для этого tool
       systemPrompt = buildSystemPrompt(),
       allowedTools = emptyList(),  // content_analyzer не требует tools
       defaultModel = { /* модель по умолчанию */ },
       defaultContext = { AContextDefaultFactory.createFull() }
   )
   ```

3. **Вызов JustWorkWorker:**
   ```kotlin
   justWorkWorker.doWork(
       config = config,
       userPrompt = "Проанализируй пожалуйста это:\n$content"
   )
   ```

---

### Шаг 4.4: Регистрация инструментов в LocalMcpService

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/LocalMcpService.kt`

**Описание:** Добавление новых инструментов в сервис.

**Изменения:**

```kotlin
@Singleton
internal class LocalMcpService @Inject constructor(
    private val setReminderTool: SetReminderTool,
    private val investigateGitFileTool: InvestigateGitFileTool,  // Новый
    private val getFileAnalysisTool: GetFileAnalysisTool,        // Новый
    private val analyzeCodeContentTool: AnalyzeCodeContentTool   // Новый
) {
    private val tools: List<LocalMcpTool> = listOf(
        setReminderTool,
        investigateGitFileTool,
        getFileAnalysisTool,
        analyzeCodeContentTool
    )
    
    // ... остальной код
}
```

**Важно:**
- У инструментов `@Inject constructor` — не нужно создавать `provide` методы
- Dagger автоматически создаст экземпляры

---

### Шаг 4.5: Обновление McpToolNames

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/McpConstants.kt`

**Описание:** Добавление имен новых инструментов.

**Изменения:**

```kotlin
object McpToolNames {
    // ... существующие
    
    const val INVESTIGATE_GIT_FILE = "investigate_git_file"
    const val GET_FILE_ANALYSIS = "get_file_analysis"
    const val ANALYZE_CODE_CONTENT = "analyze_code_content"
    
    val ALLOWED_TOOL_NAMES = setOf(
        // ... существующие
        INVESTIGATE_GIT_FILE,
        GET_FILE_ANALYSIS,
        ANALYZE_CODE_CONTENT
    )
}
```

---

## Резюме этапа

**Что получим:**
- ✅ InvestigateGitFileTool — главный инструмент пайплайна (использует JustWorkWorker)
- ✅ GetFileAnalysisTool — инструмент с кешированием анализа
- ✅ AnalyzeCodeContentTool — инструмент анализа контента агентом
- ✅ Регистрация инструментов в LocalMcpService (через @Inject)
- ✅ Обновленный список ALLOWED_TOOL_NAMES

**Критерии успеха:**
1. InvestigateGitFileTool создает JustWorkConfig с правильными настройками
2. JustWorkWorker использует onCreateCallback для настройки агента
3. GetFileAnalysisTool проверяет кеш перед анализом
4. AnalyzeCodeContentTool использует точный текст промпта из задания
5. Все инструменты имеют inputSchema с description
6. Инструменты зарегистрированы в LocalMcpService через @Inject

---

## Зависимости от других этапов

- ✅ Зависит от Этапа 3 (JustWorkWorker, JustWorkConfig)
- ⚠️ Зависит от Этапа 5 (McpServer get_git_file_list tool)

---

## План реализации (подробный)

1. Создать InvestigateGitFileTool.kt (inputSchema с description)
2. Создать GetFileAnalysisTool.kt (inputSchema с description)
3. Создать AnalyzeCodeContentTool.kt (inputSchema с description, точный промпт)
4. Обновить LocalMcpService.kt (@Inject конструкторы)
5. Обновить McpConstants.kt (McpToolNames)
6. Собрать проект и проверить компиляцию
7. Протестировать инструменты
