# Этап 1: Scaffold + БД

## Общее описание
Создание нового Gradle-модуля `rag-server/` и его базовой инфраструктуры: конфигурация из ENV, схема SQLite с двумя таблицами, минимальная точка входа.

## Что получим
Модуль успешно компилируется. При запуске читает ENV-переменные и инициализирует (создаёт, если нет) SQLite базу данных с нужной схемой. `main()` запускается без ошибок.

## Критерии успеха
- `./gradlew :rag-server:build` проходит без ошибок
- Запуск `RagServer.kt` с минимальными ENV создаёт файл `rag_index.db` с таблицами `code_chunks` и `code_vectors`
- Таблицы можно проверить через `sqlite3 rag_index.db .tables`

---

## Задачи этапа

### 1.1 Создание модуля ✅

**settings.gradle.kts** — добавить `include(":rag-server")`

**rag-server/build.gradle.kts** — новый файл, аналог mcp-server/build.gradle.kts.

Зависимости:
- `libs.mcp.kotlin.sdk.server` — MCP SDK (тот же алиас)
- `io.ktor:ktor-server-core`, `ktor-server-netty`, `ktor-server-content-negotiation`, `ktor-server-sse` — те же версии через libs.versions.toml
- `io.ktor:ktor-client-core`, `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-client-logging` — для HTTP к Ollama/OpenRouter
- `io.ktor:ktor-serialization-kotlinx-json`
- `libs.kotlinx.serialization.json`
- `libs.kotlinx.coroutines.core`
- `org.jetbrains.exposed:exposed-core`, `exposed-jdbc` — версия 0.61.0 (актуальная на 2026)
- `org.xerial:sqlite-jdbc` — версия 3.49.1.0 (актуальная на 2026)
- `org.slf4j:slf4j-simple` — версия 2.0.17 (для логов Exposed)

mainClass: `com.example.day.ragserver.RagServerKt`
archiveBaseName: `rag-server`

---

### 1.2 RagConfig ✅

Файл: `config/RagConfig.kt`

Data class `RagConfig` с полями (все String или примитивы):
- `codePath: String` — путь к директории с исходниками
- `dbPath: String` — путь к SQLite файлу
- `embeddingProvider: String` — `"ollama"` или `"openrouter"`
- `ollamaBaseUrl: String`
- `embeddingModel: String`
- `openRouterApiKey: String` — пустая строка если не задана
- `serverPort: Int`
- `forceReindex: Boolean`
- `searchTopK: Int`

Companion object `from()` — читает переменные окружения через `System.getenv()` со значениями по умолчанию:

| ENV | Default |
|-----|---------|
| `CODE_PATH` | обязательный, error если не задан |
| `DB_PATH` | `./rag_index.db` |
| `EMBEDDING_PROVIDER` | `ollama` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `EMBEDDING_MODEL` | `nomic-embed-text` |
| `OPENROUTER_API_KEY` | `""` |
| `RAG_SERVER_PORT` | `3001` |
| `FORCE_REINDEX` | `false` |
| `SEARCH_TOP_K` | `5` |

---

### 1.3 ChunkEntity ✅

Файл: `db/ChunkEntity.kt`

Data class `ChunkEntity`:
- `id: Long` — `-1L` если ещё не сохранён
- `content: String` — текст чанка
- `filePath: String` — абсолютный путь к файлу
- `fileName: String` — только имя файла (для отображения)
- `strategy: String` — `"fixed"` или `"structural"`
- `chunkOrder: Int` — порядковый номер чанка в файле
- `indexedAt: String` — ISO timestamp

Data class `SearchResult`:
- `chunk: ChunkEntity`
- `score: Float`

---

### 1.4 CodeDatabase ✅

Файл: `db/CodeDatabase.kt`

Использует Exposed с прямым JDBC (без DAO слоя — не нужен).

Два Exposed Table object:
- `CodeChunksTable` с колонками: `id` (autoIncrement), `content`, `filePath`, `fileName`, `strategy`, `chunkOrder`, `indexedAt`
- `CodeVectorsTable` с колонками: `chunkId` (reference на CodeChunksTable.id, onDelete CASCADE), `embedding` (blob)

Класс `CodeDatabase(dbPath: String)`:

Методы:
- `connect()` — подключение через `Database.connect("jdbc:sqlite:$dbPath", ...)`, затем `SchemaUtils.createMissingTablesAndColumns()`
- `hasIndex(strategy: String): Boolean` — `CodeChunksTable.selectAll().where { strategy eq ... }.count() > 0`
- `clearIndex(strategy: String)` — удаление всех чанков по стратегии (cascade удалит векторы)
- `saveChunk(entity: ChunkEntity, embedding: FloatArray): Long` — insert в обе таблицы в одной транзакции, возвращает id
- `getAllVectors(strategy: String): List<Pair<ChunkEntity, FloatArray>>` — join двух таблиц, возвращает все чанки с векторами для данной стратегии
- `getStats(): IndexStats` — count по стратегиям, максимальная дата индексации

Data class `IndexStats`:
- `totalChunks: Int`
- `structuralChunks: Int`
- `fixedChunks: Int`
- `indexedAt: String?` — null если индекса нет
- `isReady: Boolean` — true если оба индекса существуют

Вспомогательная функция: преобразование `FloatArray ↔ ByteArray` (через `ByteBuffer`).

---

### 1.5 Минимальный RagServer.kt (заглушка) ✅

Файл: `RagServer.kt`

Функция `main()`:
1. `RagConfig.from()` — читаем конфиг
2. `CodeDatabase(config.dbPath).connect()` — инициализируем БД
3. Вывод в лог: «RAG Server initialized. DB: ${config.dbPath}. Code path: ${config.codePath}»
4. Заглушка — `TODO("MCP server will be added in Stage 4")`

На этом этапе `main()` не запускает HTTP-сервер — это добавляется на Этапе 4.
