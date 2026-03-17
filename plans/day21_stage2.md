# Этап 2: Пайплайн индексации

## Общее описание
Реализация трёх компонентов: FileScanner (сбор файлов), ChunkingStrategy с двумя реализациями (Fixed + Structural), IndexingService (оркестратор). На этом этапе эмбеддинги заменяются заглушкой — `FloatArray(1536) { 0f }` — чтобы протестировать пайплайн без реального embedding-провайдера.

## Что получим
При запуске сервера: файлы из `CODE_PATH` разбиваются на чанки двумя стратегиями и сохраняются в SQLite. Можно убедиться через `sqlite3`: `SELECT strategy, COUNT(*) FROM code_chunks GROUP BY strategy`.

## Критерии успеха
- `code_chunks` содержит строки с `strategy='fixed'` и `strategy='structural'`
- `code_vectors` содержит соответствующие BLOB-записи (пусть нулевые на этом этапе)
- При повторном запуске без `FORCE_REINDEX=true` — индексация пропускается
- При `FORCE_REINDEX=true` — старые записи удаляются, индексация повторяется

---

## Задачи этапа

### 2.1 FileScanner ✅

Файл: `indexing/FileScanner.kt`

Объект `FileScanner` с методом:
`fun scan(rootPath: String): List<File>`

Логика:
1. `File(rootPath).walkTopDown()` — рекурсивный обход
2. Фильтр директорий через `onEnter { !EXCLUDED_DIRS.contains(it.name) }` — исключить `build`, `.git`, `.gradle`, `generated`, `.idea`
3. Фильтр файлов: расширение в `INCLUDED_EXTENSIONS` = `{"kt", "kts", "md"}`
4. Сортировка по пути для детерминированного порядка

Константы (`private`):
- `EXCLUDED_DIRS: Set<String>`
- `INCLUDED_EXTENSIONS: Set<String>`

---

### 2.2 ChunkingStrategy ✅

Файл: `indexing/ChunkingStrategy.kt`

Interface `ChunkingStrategy`:
- `val strategyName: String` — `"fixed"` или `"structural"`
- `fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>`

---

#### FixedSizeStrategy

`class FixedSizeStrategy(val chunkSize: Int = 1000, val overlap: Int = 200)`

Алгоритм метода `split()`:
1. Если `content.length <= chunkSize` — вернуть один чанк со всем содержимым
2. Иначе: скользящее окно с шагом `chunkSize - overlap`
3. Для каждого окна: префикс `"// File: $fileName\n"` + подстрока

Метаданные каждого чанка:
- `strategy = "fixed"`
- `chunkOrder` = индекс окна

Особенности:
- Граница окна не выравнивается по символам переноса строки — режем строго по позиции
- Последний чанк берёт остаток до конца файла

---

#### StructuralStrategy

`class StructuralStrategy(val maxChunkSize: Int = 2000)`

Алгоритм метода `split()`:
1. Разбить файл по regex `(?=\n(fun |class |interface |object |data class |sealed |enum |abstract |companion ))` — lookahead, чтобы разделитель оставался в начале следующего блока
2. Отфильтровать пустые блоки
3. Для каждого блока:
   - Если `block.length <= maxChunkSize` — один чанк с префиксом `"// File: $fileName\n"`
   - Если больше — разбить через `FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)` и добавить все под-чанки с тем же `chunkOrder` + суффикс (чтобы отличать)
4. `chunkOrder` = порядковый номер блока в файле

Метаданные:
- `strategy = "structural"`

---

### 2.3 IndexingService ✅

Файл: `indexing/IndexingService.kt`

`class IndexingService(private val db: CodeDatabase, private val embeddingProvider: EmbeddingProvider)`

Главный метод: `suspend fun indexAll(scanner: FileScanner, config: RagConfig)`

Для каждой стратегии (`FixedSizeStrategy`, `StructuralStrategy`) выполняет:

```
indexStrategy(strategy, files, config.forceReindex)
```

Метод `indexStrategy(strategy, files, forceReindex)`:
1. Если `db.hasIndex(strategy.strategyName) && !forceReindex` → log «Skipping ${strategy.strategyName}: index exists» → return
2. Если `forceReindex` → `db.clearIndex(strategy.strategyName)` → log «Cleared old index for ${strategy.strategyName}»
3. Log «Indexing ${files.size} files with ${strategy.strategyName} strategy»
4. Итерация по файлам:
   - `strategy.split(content, filePath, fileName)` → чанки
   - Для каждого чанка: `embeddingProvider.embed(chunk.content)` → `FloatArray`
   - `db.saveChunk(chunk, embedding)`
5. Log «Indexed X chunks for ${strategy.strategyName}»

Обработка ошибок:
- Ошибка `embed()` для конкретного чанка — логировать и продолжить (не прерывать весь пайплайн)
- Ошибка чтения файла — логировать и перейти к следующему файлу

На этапе 2 передаётся заглушка `EmbeddingProvider`:
```
val stubProvider = EmbeddingProvider { FloatArray(1536) { 0f } }
```
(SAM-совместимый функциональный интерфейс — реальные провайдеры подключаются на Этапе 3)
