# Этап 4: Search + MCP Tools + main()

## Общее описание
Финальный этап. Реализация поиска по векторам, регистрация трёх MCP-тулов, сборка `main()` и добавление Dockerfile по аналогии с mcp-server. Сервер становится полностью рабочим.

## Что получим
Работающий MCP-сервер на порту 3001. Android-агент видит тулы `search_codebase`, `search_codebase_fixed`, `get_index_status`. При вызове `search_codebase` возвращает релевантные фрагменты кодовой базы.

## Критерии успеха
- Сервер запускается, логирует «RAG MCP Server started on port 3001»
- Через MCP Inspector (или curl): вызов `get_index_status` возвращает корректные счётчики чанков
- Вызов `search_codebase` с запросом «как работает MCP» возвращает релевантные фрагменты `.kt` файлов
- Агент в Android-приложении видит три тула при подключении к `http://10.0.2.2:3001`

---

## Задачи этапа

### 4.1 VectorMath ✅

Файл: `search/VectorMath.kt`

`object VectorMath`:
- `fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float`

Алгоритм:
1. Проверка: если размеры векторов не совпадают → вернуть `0f`
2. Считать скалярное произведение `dotProduct`, нормы `norm1`, `norm2`
3. Если `norm1 == 0f || norm2 == 0f` → вернуть `0f`
4. Вернуть `dotProduct / (sqrt(norm1) * sqrt(norm2))`

Примечание: для нормализованных векторов (Ollama nomic-embed-text нормализует) это просто скалярное произведение, но формулу держим полную для корректности.

---

### 4.2 SearchService ✅

Файл: `search/SearchService.kt`

`class SearchService(private val db: CodeDatabase, private val embeddingProvider: EmbeddingProvider)`

Метод: `suspend fun search(query: String, strategy: String, topK: Int): List<SearchResult>`

Алгоритм:
1. `val queryVector = embeddingProvider.embed(query)`
2. `val allVectors = db.getAllVectors(strategy)` — список `(ChunkEntity, FloatArray)`
3. Если список пустой → вернуть пустой список
4. Для каждой пары: `score = VectorMath.cosineSimilarity(queryVector, chunkVector)`
5. Отсортировать по `score` убывая
6. Взять top `topK`
7. Вернуть `List<SearchResult>`

---

### 4.3 RagToolNames ✅

Файл: `tools/RagToolNames.kt`

`object RagToolNames`:
- `const val SEARCH_CODEBASE = "search_codebase"`
- `const val SEARCH_CODEBASE_FIXED = "search_codebase_fixed"`
- `const val GET_INDEX_STATUS = "get_index_status"`

---

### 4.4 RagTools ✅

Файл: `tools/RagTools.kt`

Top-level функция: `fun registerRagTools(server: Server, searchService: SearchService, db: CodeDatabase, topK: Int)`

---

#### Tool: search_codebase (structural)

`server.addTool(name = RagToolNames.SEARCH_CODEBASE, ...)`

Описание:
> «Используй этот инструмент для поиска по внутренней кодовой базе Android-проекта. Помогает найти реализацию классов, use cases, репозиториев, DI-компонентов и архитектурных паттернов. Возвращает логически завершённые блоки кода (функции, классы).»

InputSchema: `{ "query": { "type": "string", "description": "Поисковый запрос на естественном языке или название класса/метода" } }`, required: `["query"]`

Логика handler:
1. Извлечь `query` из аргументов
2. `searchService.search(query, strategy = "structural", topK)`
3. Если список пустой → `CallToolResult(content = [TextContent("Ничего не найдено в индексе кодовой базы. Проверьте статус индекса через get_index_status.")])`
4. Иначе: форматировать результаты как `"[${i+1}/${results.size}] ${result.chunk.fileName} (${result.chunk.strategy})\nScore: ${"%.3f".format(result.score)}\n\n${result.chunk.content}"`, объединить через `"\n${"=".repeat(60)}\n"`
5. Вернуть `CallToolResult(content = [TextContent(text = formatted)])`

---

#### Tool: search_codebase_fixed (fixed-size)

`server.addTool(name = RagToolNames.SEARCH_CODEBASE_FIXED, ...)`

Описание:
> «Альтернативный поиск по кодовой базе с нарезкой фиксированного размера. Используй если search_codebase не дал нужного результата или хочешь сравнить подходы. Может возвращать фрагменты без чётких границ функций.»

InputSchema: то же — только `query`.

Логика handler: аналогична `search_codebase`, но `strategy = "fixed"`.

---

#### Tool: get_index_status

`server.addTool(name = RagToolNames.GET_INDEX_STATUS, ...)`

Описание:
> «Проверь статус индекса кодовой базы перед поиском. Возвращает количество проиндексированных чанков по каждой стратегии и дату индексации. Если is_ready = false — индекс не готов и поиск не даст результатов.»

InputSchema: `{}` (нет параметров)

Логика handler:
1. `db.getStats()` → `IndexStats`
2. Формировать текстовый ответ:
   ```
   Статус индекса:
   - Готов: ${stats.isReady}
   - Всего чанков: ${stats.totalChunks}
   - Структурная стратегия: ${stats.structuralChunks} чанков
   - Фиксированная стратегия: ${stats.fixedChunks} чанков
   - Последняя индексация: ${stats.indexedAt ?: "никогда"}
   ```
3. `CallToolResult(content = [TextContent(text = ...)])`

---

### 4.5 RagServer.kt — итоговый main() ✅

Файл: `RagServer.kt`

Последовательность `main()`:

1. `val config = RagConfig.from()`
2. `val json = Json { ignoreUnknownKeys = true; isLenient = true }`
3. `val httpClient = HttpClient(OkHttp) { ... }` — с ContentNegotiation, Logging, HttpTimeout
4. `val db = CodeDatabase(config.dbPath)` → `db.connect()`
5. `val embeddingProvider = createEmbeddingProvider(config, httpClient)`
6. `val scanner = FileScanner`
7. `val indexingService = IndexingService(db, embeddingProvider)`
8. Log «Starting indexing...»
9. `runBlocking { indexingService.indexAll(scanner, config) }` — блокируем main, пока индексация не завершится
10. Log «Indexing done. Starting MCP server on port ${config.serverPort}»
11. Создать `Server(serverInfo = Implementation(name = "codebase-rag", version = "1.0.0"), options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))))`
12. `val searchService = SearchService(db, embeddingProvider)`
13. `registerRagTools(server, searchService, db, config.searchTopK)`
14. `embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") { install(ContentNegotiation) { json(json) }; mcpStreamableHttp { server }; routing { post("/message") { call.respondRedirect("/mcp/message") } } }.start(wait = true)`

---

### 4.6 Dockerfile ✅

Файл: `rag-server/Dockerfile`

По образцу `mcp-server/Dockerfile`:
- `FROM eclipse-temurin:21-jre`
- `COPY build/libs/rag-server.jar /app/rag-server.jar`
- `EXPOSE 3001`
- `ENTRYPOINT ["java", "-jar", "/app/rag-server.jar"]`

---

### 4.7 Кросс-проверка с общим планом

| Требование общего плана | Этап 4 покрывает |
|------------------------|-----------------|
| Три MCP-тула | ✅ search_codebase, search_codebase_fixed, get_index_status |
| Cosine similarity поиск | ✅ VectorMath + SearchService |
| Агент видит тулы | ✅ registerRagTools + mcpStreamableHttp |
| Dockerfile | ✅ по образцу mcp-server |
| Порт 3001 | ✅ config.serverPort |
| Форматированный вывод для агента | ✅ с именем файла, score, содержимым |
