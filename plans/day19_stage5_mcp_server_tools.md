# Этап 5: McpServer расширения

## Общее описание

Добавление новых MCP-инструментов в McpServer для работы с GitHub:
1. get_git_file_list — получение списка всех файлов репозитория
2. reset_git_file_list_cache — сброс кеша списка файлов

**Цель этапа:** Реализовать серверные инструменты для доступа к файлам GitHub.

---

## Задачи этапа

### Шаг 5.1: GitHubApiClient расширение

**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/github/GitHubApiClient.kt`

**Описание:** Добавление метода для получения списка всех файлов репозитория.

**Новые методы:**
```kotlin
class GitHubApiClient {
    // ... существующие методы
    
    /**
     * Получает список всех файлов в репозитории.
     * Использует GitHub Tree API для рекурсивного получения структуры.
     * 
     * @return Список полных путей к файлам (/path/to/file.ext)
     */
    suspend fun listAllFiles(): Result<List<String>>
    
    /**
     * Скачивает содержимое файла по полному пути.
     * 
     * @param filePath Полный путь к файлу в репозитории
     * @return Содержимое файла как String
     */
    suspend fun getFileContent(filePath: String): Result<String>
}
```

**Логика работы `listAllFiles()`:**

1. **Использование GitHub Tree API:**
   ```kotlin
   suspend fun listAllFiles(): Result<List<String>> {
       val owner = defaultOwner ?: return Result.failure(Error("GITHUB_OWNER not set"))
       val repo = defaultRepo ?: return Result.failure(Error("GITHUB_REPO not set"))
       
       // Получаем текущую ветку (по умолчанию main/master)
       val branch = getDefaultBranch(owner, repo)
       
       // Запрашиваем дерево файлов рекурсивно
       val treeUrl = "/repos/$owner/$repo/git/trees/$branch?recursive=1"
       val response = client.get(treeUrl)
       
       // Парсим ответ
       val tree = response.body<JsonObject>()["tree"]?.jsonArray
           ?: return Result.failure(Error("Invalid response"))
       
       // Фильтруем только файлы (не директории)
       val files = tree.mapNotNull { element ->
           val obj = element.jsonObject
           val type = obj["type"]?.jsonPrimitive?.content
           val path = obj["path"]?.jsonPrimitive?.content
           if (type == "blob" && path != null) "/$path" else null
       }
       
       return Result.success(files)
   }
   ```

**Логика работы `getFileContent()`:**

```kotlin
suspend fun getFileContent(filePath: String): Result<String> {
    val owner = defaultOwner ?: return Result.failure(Error("GITHUB_OWNER not set"))
    val repo = defaultRepo ?: return Result.failure(Error("GITHUB_REPO not set"))
    
    // Убираем ведущий слэш если есть
    val cleanPath = filePath.trimStart('/')
    
    // Запрашиваем содержимое файла
    val url = "/repos/$owner/$repo/contents/$cleanPath"
    val response = client.get(url)
    
    // Парсим ответ (GitHub возвращает base64 закодированное содержимое)
    val jsonObject = response.body<JsonObject>()
    val contentBase64 = jsonObject["content"]?.jsonPrimitive?.content
        ?: return Result.failure(Error("No content in response"))
    
    // Декодируем из base64
    val decodedContent = String(Base64.getDecoder().decode(contentBase64))
    
    return Result.success(decodedContent)
}
```

---

### Шаг 5.2: get_git_file_list tool

**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/GitHubTools.kt`

**Описание:** MCP-инструмент для получения списка файлов GitHub.

**Регистрация:**
```kotlin
fun registerGetGitFileListTool(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = "get_git_file_list",
        description = "Получает список всех полных имен файлов из git репозитория. " +
            "Возвращает массив путей к файлам в формате /path/to/file.ext",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // Нет параметров, owner и repo берутся из env
            },
            required = emptyList()
        )
    ) { request ->
        // Вызываем API
        val result = api.listAllFiles()
        
        result.fold(
            onSuccess = { fileList ->
                // Формируем ответ
                val responseJson = buildJsonObject {
                    put("status", "ok")
                    put("content", JsonArray(fileList.map { JsonPrimitive(it) }))
                }
                CallToolResult(
                    content = listOf(TextContent(text = responseJson.toString()))
                )
            },
            onFailure = { error ->
                val responseJson = buildJsonObject {
                    put("status", "error")
                    put("error", error.message ?: "Unknown error")
                }
                CallToolResult(
                    content = listOf(TextContent(text = responseJson.toString())),
                    isError = true
                )
            }
        )
    }
}
```

---

### Шаг 5.3: reset_git_file_list_cache tool

**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/GitHubTools.kt`

**Описание:** MCP-инструмент для сброса кеша списка файлов.

**Регистрация:**
```kotlin
fun registerResetGitFileListCacheTool(server: Server, cacheRepository: GitFileCacheRepository) {
    server.addTool(
        name = "reset_git_file_list_cache",
        description = "Сбрасывает кеш файлов git. Используйте для принудительного обновления списка файлов.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // Нет параметров
            },
            required = emptyList()
        )
    ) { request ->
        // Сбрасываем кеш
        cacheRepository.clearCache()
        
        val responseJson = buildJsonObject {
            put("status", "ok")
            put("content", JsonArray(emptyList()))
        }
        CallToolResult(
            content = listOf(TextContent(text = responseJson.toString()))
        )
    }
}
```

---

### Шаг 5.4: Кеширование на стороне приложения

**Описание:** Кеширование списка файлов реализовано на стороне приложения (app module) через GitFileCacheRepository.

**Схема работы:**

```
McpServer (get_git_file_list tool)
       │
       │ (возвращает список файлов)
       ▼
LocalMcpService / GetGitFileListUseCase
       │
       │ (сохраняет в кеш)
       ▼
GitFileCacheRepository → GitFileCacheEntity (Room БД)
```

**Преимущества:**
- Кеширование в той же БД что и остальные данные приложения
- Не требует дополнительных HTTP endpoints в McpServer
- Единый подход к кешированию для всех инструментов

---

### Шаг 5.5: Регистрация инструментов в McpServer

**Расположение:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/McpServer.kt`

**Описание:** Добавление новых инструментов в сервер.

**Изменения:**

```kotlin
fun main() {
    // ... существующий код
    
    registerMcpTools(server, githubClient)
    
    // Новые инструменты
    registerGetGitFileListTool(server, githubClient)
    registerResetGitFileListCacheTool(server, gitFileListCache)
}
```

---

### Шаг 5.6: Обновление McpConstants

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/domain/McpConstants.kt`

**Описание:** Добавление имен новых инструментов.

**Изменения:**

```kotlin
object McpToolNames {
    // ... существующие
    
    const val GET_GIT_FILE_LIST = "get_git_file_list"
    const val RESET_GIT_FILE_LIST_CACHE = "reset_git_file_list_cache"
    
    val ALLOWED_TOOL_NAMES = setOf(
        // ... существующие
        GET_GIT_FILE_LIST,
        RESET_GIT_FILE_LIST_CACHE
    )
}
```

---

## Резюме этапа

**Что получим:**
- ✅ GitHubApiClient.listAllFiles() — получение списка файлов
- ✅ GitHubApiClient.getFileContent() — скачивание содержимого файла
- ✅ get_git_file_list MCP tool — инструмент получения списка
- ✅ reset_git_file_list_cache MCP tool — инструмент сброса кеша
- ✅ Кеширование на стороне приложения (GitFileCacheRepository)

**Критерии успеха:**
1. get_git_file_list возвращает корректный список путей
2. reset_git_file_list_cache очищает кеш
3. GitHub API вызовы корректно обрабатывают ошибки
4. Инструменты зарегистрированы в McpServer
5. Формат ответа соответствует спецификации

---

## Зависимости от других этапов

- ✅ Зависит от Этапа 1 (GitFileCacheRepository для кеширования на стороне приложения)
- ⚠️ Этап 4 использует get_git_file_list tool

---

## План реализации (подробный)

1. Обновить GitHubApiClient.kt (добавить listAllFiles и getFileContent)
2. Создать registerGetGitFileListTool в GitHubTools.kt
3. Создать registerResetGitFileListCacheTool в GitHubTools.kt
4. Обновить McpServer.kt (регистрация инструментов)
5. Обновить McpConstants.kt (McpToolNames)
6. Собрать mcp-server и проверить компиляцию
7. Запустить сервер и протестировать инструменты
