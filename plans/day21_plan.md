# День 21. RAG-сервер для поиска по кодовой базе

## Постановка задачи

Реализовать пайплайн индексации документов (кодовая база Kotlin + Markdown) и предоставить доступ к нему через новый MCP-сервер. Агент в Android-приложении, получив вопрос пользователя о внутреннем устройстве проекта, вызывает тул MCP-сервера, получает релевантные фрагменты кода и отвечает на основе реального контекста.

Требования задания:
- Минимум 20–30 страниц текста для индексации (кодовая база проекта `.kt` + `.md`)
- Два алгоритма разбиения на чанки (chunking) с сравнением
- Метаданные к каждому чанку: source, file_path, strategy, chunk_order
- Хранилище: SQLite (персистентный индекс)
- Эмбеддинги: Ollama (локально) или OpenRouter (облако), переключаемо через ENV

---

## Общая архитектура

Новый Gradle-модуль `rag-server/` — самостоятельный Kotlin JVM сервер, независимый от Android-приложения. Реализован по образцу существующего `mcp-server/`.

### Стек
- Kotlin JVM, Coroutines
- MCP Kotlin SDK (io.modelcontextprotocol:kotlin-sdk-server:0.8.4) — тот же что в mcp-server
- Ktor (server: Netty, client: OkHttp) — те же версии что в mcp-server
- Exposed + SQLite JDBC — для персистентного хранения чанков и эмбеддингов
- kotlinx.serialization.json — для метаданных и HTTP-тел
- Без LangChain4j, без внешних DI-фреймворков

### Схема взаимодействия
```
Android App (TalkWorker / AIAgent)
    │
    │  MCP JSON-RPC (HTTP POST /mcp/message)
    ▼
RAG MCP Server (порт 3001)
    ├── search_codebase           → SearchService → SQLite (vectors)
    ├── search_codebase_fixed     → SearchService → SQLite (vectors)
    └── get_index_status          → CodeDatabase → chunk counts

CodeDatabase (SQLite: rag_index.db)
    ├── code_chunks               — текст + метаданные
    └── code_vectors              — FloatArray как BLOB

EmbeddingProvider
    ├── OllamaEmbeddingProvider   → POST http://localhost:11434/api/embeddings
    └── OpenRouterEmbeddingProvider → POST https://openrouter.ai/api/v1/embeddings
```

### Структура модуля
```
rag-server/
├── build.gradle.kts
├── SETUP.md                               — инструкция по запуску
└── src/main/kotlin/com/example/day/ragserver/
    ├── RagServer.kt                       — main(), точка входа
    ├── config/
    │   └── RagConfig.kt                  — чтение ENV, data class конфигурации
    ├── db/
    │   ├── ChunkEntity.kt                — доменная модель чанка
    │   └── CodeDatabase.kt               — инициализация SQLite, CRUD
    ├── indexing/
    │   ├── ChunkingStrategy.kt           — interface + две реализации
    │   ├── FileScanner.kt                — обход директории CODE_PATH
    │   └── IndexingService.kt            — оркестратор пайплайна индексации
    ├── embedding/
    │   ├── EmbeddingProvider.kt          — interface
    │   ├── OllamaEmbeddingProvider.kt
    │   └── OpenRouterEmbeddingProvider.kt
    ├── search/
    │   ├── VectorMath.kt                 — cosineSimilarity()
    │   └── SearchService.kt              — поиск по векторам
    └── tools/
        ├── RagToolNames.kt               — константы имён тулов
        └── RagTools.kt                   — регистрация тулов в Server
```

---

## Ключевые сущности

### RagConfig
Читает переменные окружения и предоставляет единую точку конфигурации.

Параметры:
- `CODE_PATH` — путь к директории с исходниками (обязательный)
- `DB_PATH` — путь к SQLite файлу (default: `./rag_index.db`)
- `EMBEDDING_PROVIDER` — `ollama` или `openrouter` (default: `ollama`)
- `OLLAMA_BASE_URL` — (default: `http://localhost:11434`)
- `EMBEDDING_MODEL` — имя модели (default: `nomic-embed-text`)
- `OPENROUTER_API_KEY` — ключ для OpenRouter (обязателен если provider=openrouter)
- `RAG_SERVER_PORT` — порт сервера (default: `3001`)
- `FORCE_REINDEX` — `true` для принудительной переиндексации (default: `false`)
- `SEARCH_TOP_K` — сколько результатов возвращать (default: `5`)

### ChunkEntity
Доменная модель чанка с полями:
- id, content, filePath, fileName, strategy (`fixed` / `structural`), chunkOrder, indexedAt
- embedding: FloatArray (не хранится в entity, только в БД)

### CodeDatabase
Управляет двумя таблицами SQLite:
- `code_chunks(id, content, file_path, file_name, strategy, chunk_order, indexed_at)`
- `code_vectors(chunk_id FK, embedding BLOB)`

Методы:
- `initialize()` — создание таблиц если не существуют
- `hasIndex(strategy)` — есть ли уже чанки для данной стратегии
- `clearIndex(strategy)` — очистка чанков по стратегии
- `saveChunk(entity, embedding)` — сохранение чанка + вектора
- `getAllVectors(strategy)` — возвращает список (ChunkEntity, FloatArray) для поиска
- `getStats()` — статистика: total, по стратегиям, дата последней индексации

### ChunkingStrategy (interface)
Метод: `split(content: String, filePath: String): List<ChunkEntity>`

Реализации:
- `FixedSizeStrategy(chunkSize: Int = 1000, overlap: Int = 200)` — скользящее окно по символам. Создаёт чанки строго фиксированного размера с перекрытием. Контекст обогащается заголовком `// File: X.kt` в начале каждого чанка.
- `StructuralStrategy(maxChunkSize: Int = 2000)` — разбивает по ключевым словам Kotlin (`fun `, `class `, `interface `, `object `, `data class `). Каждый чанк — логически завершённый блок. Если блок превышает maxChunkSize — дробится по FixedSize. Контекст обогащается заголовком `// File: X.kt`.

### FileScanner
Обходит `CODE_PATH` рекурсивно, возвращает список файлов.

Фильтры:
- Включать: `.kt`, `.kts`, `.md`
- Исключать: директории `build/`, `.git/`, `generated/`

Метод: `scan(rootPath: String): List<File>`

### IndexingService
Оркестратор пайплайна. Для каждой стратегии:
1. Проверяет `hasIndex(strategy)` — если есть и `FORCE_REINDEX=false`, пропускает
2. `FileScanner.scan()` — список файлов
3. Для каждого файла: `strategy.split()` → список ChunkEntity
4. Для каждого чанка: `EmbeddingProvider.embed(content)` → FloatArray
5. `CodeDatabase.saveChunk(entity, embedding)`

Метод: `suspend fun indexAll()` — индексирует обеими стратегиями

### EmbeddingProvider (interface)
Метод: `suspend fun embed(text: String): FloatArray`

Реализации через Ktor Client (POST с телом `{"model": "...", "prompt": "..."}`):
- `OllamaEmbeddingProvider` — POST `{OLLAMA_BASE_URL}/api/embeddings`
- `OpenRouterEmbeddingProvider` — POST `https://openrouter.ai/api/v1/embeddings` с Authorization header

### VectorMath
Утилитарный объект. Метод:
- `cosineSimilarity(v1: FloatArray, v2: FloatArray): Float`

### SearchService
Загружает все векторы из БД, считает cosine similarity с вектором запроса, возвращает top-K.

Метод: `suspend fun search(query: String, strategy: String, topK: Int): List<SearchResult>`

`SearchResult`: content, filePath, fileName, strategy, chunkOrder, score: Float

### RagTools (MCP Tools)

**`search_codebase`** (structural strategy)
- Описание для агента: «Используй для поиска по внутренней кодовой базе проекта: архитектура, реализация классов, use cases, DI. Возвращает логически завершённые блоки кода (функции, классы).»
- Параметры: `query: String`
- Логика: embed(query) → SearchService.search(strategy=structural, topK) → форматированный текст

**`search_codebase_fixed`** (fixed-size strategy)
- Описание для агента: «Альтернативный поиск по кодовой базе фиксированными чанками. Используй если search_codebase не дал результата или нужен другой угол обзора.»
- Параметры: `query: String`
- Логика: embed(query) → SearchService.search(strategy=fixed, topK) → форматированный текст

**`get_index_status`**
- Описание для агента: «Проверь статус индекса перед поиском по кодовой базе. Возвращает количество проиндексированных чанков и дату индексации.»
- Параметры: нет
- Логика: CodeDatabase.getStats() → JSON с полями total_chunks, structural_chunks, fixed_chunks, indexed_at, is_ready

---

## Этапы реализации

### Этап 1: Scaffold + БД
Создание модуля, конфигурации, схемы БД. По завершении: модуль компилируется, БД инициализируется.

### Этап 2: Индексация
FileScanner, ChunkingStrategy (Fixed + Structural), IndexingService. По завершении: файлы из CODE_PATH разбиваются на чанки двумя стратегиями (без эмбеддингов — заглушка).

### Этап 3: Embedding Providers
OllamaEmbeddingProvider, OpenRouterEmbeddingProvider, выбор через конфиг. По завершении: реальные эмбеддинги сохраняются в SQLite.

### Этап 4: Search + MCP Tools + main()
VectorMath, SearchService, регистрация тулов, точка входа. По завершении: сервер запускается, тулы доступны агенту.

---

## Сравнение стратегий (результат задания)
- Оба набора чанков хранятся в одной БД с полем `strategy`
- `search_codebase` (structural) vs `search_codebase_fixed` — агент может вызвать оба и сравнить
- Структурная стратегия ожидаемо даёт более качественный контекст: целые функции/классы с понятными границами
- Фиксированная стратегия может разрывать логические блоки, но полезна как контрольная точка
