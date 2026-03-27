# RAG Pipeline — описание шагов

## Обзор

Пайплайн — это последовательность шагов (`PipelineStep`), которые принимают `PipelineContext` и возвращают обновлённый контекст. Каждый шаг измеряет своё время выполнения; все метрики собираются в `PipelineMetrics`.

```
Запрос пользователя
       │
       ▼
[1] QueryOptimizeStep   (опциональный)
       │
       ▼
[2] RetrievalStep       (всегда)
       │
       ▼
[3] ThresholdFilterStep (опциональный)
       │
       ▼
[4] RerankStep          (опциональный)
       │
       ▼
[5] TopKStep            (всегда)
       │
       ▼
[6] ContextPackingStep  (всегда)
       │
       ▼
   PackedContext → LLM
```

---

## Шаги пайплайна

### 1. QueryOptimizeStep

**Условие запуска:** `enableQueryOptimize = true` в конфиге **И** сервер запущен с `TRANSLATE_QUERIES=true`.
Если env-флаг не установлен — шаг пропускается с предупреждением, даже если параметр запроса включён.

**Что делает:** Отправляет исходный запрос в LLM (модель задаётся через `TRANSLATE_LLM_MODEL`) с системным промптом, который:
1. Делает запрос самодостаточным и конкретным
2. Переводит на английский язык
3. Убирает стоп-слова
4. Добавляет технические ключевые слова (`class`, `function`, `repository`, `use case`, и т.д.)

**Результат:** `ctx.query` обновляется оптимизированной строкой. Оригинальный запрос остаётся в `ctx.originalQuery`.

**Пример:** `"где обрабатываются сообщения чата?"` → `"chat message handling repository use case Kotlin"`

---

### 2. RetrievalStep (составной)

**Условие запуска:** всегда выполняется.

Выбор стратегии поиска задаётся параметром `retrievalStrategy`. Возвращает `retrievalTopK` чанков.

#### Стратегия TWO_STAGE (по умолчанию)

Двухэтапный поиск с приоритетом по метаданным классов.

**Этап 1 — отбор релевантных классов (Stage 1):**

Основной путь (если векторы метаданных сгенерированы):
- Векторизует запрос через embedding-модель
- Сравнивает вектор запроса с векторами метаданных каждого класса (cosine similarity)
- Добавляет keyword-буст для точных совпадений имён: `score = embeddingScore + min(kwScore * 0.2, 0.2)`
- Буст ограничен `KEYWORD_BOOST_MAX = 0.2` — семантика управляет рейтингом, keyword только усиливает
- Отсекает классы с `embeddingScore < 0.1`
- Берёт топ `COARSE_TOP_K = 5` классов

Fallback (если векторы метаданных отсутствуют — например, первый запуск):
- Keyword-поиск по полям `responsibility`, `domainTags`, `className`, `keyMethods`
- Ранжирует по доле токенов запроса, найденных в текстовых полях
- Эффективен только для английских запросов с точными именами

**Если ни один класс не прошёл** — переходит в `standardSearch`: прямой cosine similarity по всем структурным чанкам.

**Этап 2 — drill-down по чанкам (Stage 2):**

Для каждого из `COARSE_TOP_K = 5` классов:
- Фильтрует структурные чанки, принадлежащие классу (по `fileName` или `declarationName`; если ничего — по вхождению имени класса в контент)
- Считает cosine similarity каждого чанка с вектором запроса
- Добавляет `methodBoost = 0.1` для чанков, чьё `declarationName` совпадает с ключевыми методами из метаданных
- Берёт топ `DRILL_DOWN_PER_CLASS = 3` чанков из каждого класса

Итого: до `5 × 3 = 15` чанков, затем дедупликация, сортировка, обрезка до `retrievalTopK`.

#### Стратегия HYBRID

Прямой гибридный поиск без двухэтапной фильтрации:
- Векторизует запрос
- Для каждого чанка вычисляет:
  - `embeddingScore` — cosine similarity
  - `keywordScore` — TF-подобный overlap токенов запроса и контента
  - `finalScore = 0.7 * embeddingScore + 0.3 * keywordScore` (веса зафиксированы в `ScoredChunk`)
- Сортирует по `finalScore`, берёт топ `retrievalTopK`

Параметр `chunkingStrategy` (`"structural"` или `"fixed"`) определяет, из какой таблицы берутся чанки. Актуален только для `HYBRID`.

---

### 3. ThresholdFilterStep

**Условие запуска:** `threshold > 0.0`.

Убирает из результатов все чанки, у которых `score < threshold`. Логирует количество до и после фильтрации.

**Типичное значение:** `0.66` в пресетах `FILTERED`, `RERANKED_*`.

---

### 4. RerankStep (составной)

**Условие запуска:** `rerankStrategy != NONE`.

Принимает все результаты после фильтра и переупорядочивает их. Финальная обрезка до `finalTopK` выполняется в следующем шаге (`TopKStep`), а не здесь.

#### Стратегия HEURISTIC

- Токенизирует запрос и контент каждого чанка
- Считает `overlap = |queryWords ∩ chunkWords| / |queryWords|`
- Добавляет `bonus = overlap * 0.1` к текущему `score`
- Бонус ограничен `0.1` чтобы не перекрыть embedding score
- Сортирует по новому score

Быстро, без сетевых вызовов. Хорошо работает если запрос и код на одном языке.

#### Стратегия LLM

- Формирует промпт: запрос + все чанки (обрезанные до `400` символов каждый)
- Просит LLM вернуть строки в формате `"Chunk N: X.XX"` (score от 0.00 до 1.00)
- Парсит ответ через regex `Chunk (\d+): ([\d.]+)` — устойчиво к лишнему тексту
- Подставляет LLM-оценки вместо embedding-score и сортирует
- При ошибке парсинга — возвращает оригинальный порядок с логом

Медленно (один LLM-вызов на запрос), но учитывает семантическое соответствие кода запросу.

---

### 5. TopKStep

**Условие запуска:** всегда.

Обрезает список результатов до `finalTopK` (по умолчанию `5`). Простой `take(topK)`.

---

### 6. ContextPackingStep

**Условие запуска:** всегда.

Упаковывает список `SearchResult` в `PackedContext` для передачи в LLM.

**Алгоритм:**
1. Группирует чанки по классу (`fileName` без `.kt`-суффикса)
2. Сортирует группы по максимальному `score` среди чанков группы
3. Для каждой группы:
   - Дедуплицирует чанки по хешу контента
   - Сортирует чанки по `startLine` (порядок в файле)
   - Считает примерное число токенов: `длина_текста / 4`
4. Добавляет группы до достижения лимита `tokenLimit = 6000` токенов
   - Первая группа берётся всегда, даже если превышает лимит
   - Остальные группы пропускаются если `usedTokens + groupTokens > tokenLimit`

**Результат:** `PackedContext` — список `ClassGroup`, каждая с именем класса, путём к файлу, топ-скором и списком чанков.

---

## Стратегии нарезки (Chunking)

Нарезка выполняется при **индексации** (не при поиске). Результат — чанки с `strategy="structural"` или `strategy="fixed"` в БД. Выбор стратегии управляется env var `USE_AST_CHUNKING`.

```
FileScanner (kt, kts, md, txt)
        │
        ▼
LanguageAwareChunker(useAst)
        ├── .kt / .kts + useAst=false → StructuralStrategy   (regex)
        ├── .kt / .kts + useAst=true  → AstChunkingStrategy  (ktreesitter)
        ├── .md                       → MarkdownChunkingStrategy
        └── остальные                 → FixedSizeStrategy (fallback)
```

Параллельно `FixedSizeStrategy` индексируется отдельно — даёт `strategy="fixed"` индекс.

---

### `StructuralStrategy` (режим по умолчанию, `USE_AST_CHUNKING=false`)

**Подход**: regex-сплит по ключевым словам Kotlin (`fun`, `class`, `interface`, `object`, ...).

**Алгоритм**:
1. Разбить файл по `(?=\n(?:fun |class |interface |...))`.
2. Переместить KDoc-блоки и аннотации к следующему объявлению (`attachPreamblesForward`).
3. Если блок > `maxChunkSize` — sub-split через `FixedSizeStrategy`.

**Ограничения**:
- Не отслеживает глубину скобок: `fun` в лямбде, `companion object` внутри класса — триггерят лишний сплит.
- Sub-split режет строки посередине при больших блоках.
- `declarationName` может быть мусором (`decl=for`, `decl=serves`) если чанк начался не с объявления.

---

### `AstChunkingStrategy` (`USE_AST_CHUNKING=true`, файлы `.kt`/`.kts`)

**Подход**: настоящий AST через [ktreesitter](https://github.com/tree-sitter/kotlin-tree-sitter) + Kotlin grammar от [fwcd](https://github.com/fwcd/tree-sitter-kotlin). Нативная grammar-библиотека поставляется модулем `:rag-grammar`.

**Алгоритм split-then-merge**:

1. **Parse** — `Parser.parse(source)` → `Tree` → `rootNode`.
2. **Split** — рекурсивный обход AST, извлекаем "интересные" ноды:
   - `class_declaration`, `interface_declaration`
   - `object_declaration`, `companion_object`
   - `function_declaration`
   - `property_declaration`, `typealias_declaration`
3. Для каждой ноды:
   - `text` = `node.text().toString()` — точные байтовые границы, никогда не посередине строки.
   - `declarationName` = `node.childByFieldName("name")` — гарантированно имя объявления.
   - `parentScope` = имя enclosing-класса (если нода в `class_body`) — методы знают своего владельца.
4. **Merge** — жадное слияние соседних нод с одинаковым `parentScope`, пока суммарный размер ≤ `maxChunkSize`.
5. **Oversized node** — одна нода > `maxChunkSize`: sub-split через `FixedSizeStrategy` с сохранением `declarationName` + `parentScope`.

**Что решает vs StructuralStrategy**:

| Проблема | StructuralStrategy | AstChunkingStrategy |
|----------|-------------------|-------------------|
| `fun` в лямбде триггерит сплит | да | нет — AST видит глубину |
| `companion object` отрывается от класса | да | нет — `companion_object` в `class_body` |
| Sub-split посередине строки | да | нет — oversized → FixedSize с сохранением метаданных |
| `decl=for`, `decl=serves` (мусор) | часто | никогда — `node.childByFieldName("name")` |
| `parentScope` недоступен | — | `parentScope` заполняется для всех вложенных объявлений |

**Метрики (ожидаемые после переиндексации)**:

| | До (regex) | После (AST) |
|--|--|--|
| structural chunks в БД | ~1367 | ~2500+ |
| chunks начинаются с объявления | ~60% | ~100% |
| LLM reranker score=0.00 для всех | каждый второй запрос | не должно быть |

**Поле `parentScope`**:
Заполняется для методов внутри классов: `parentScope = "GraphAIAgent"` для методов внутри `GraphAIAgent`. Используется в `TwoStageSearchService.drillDown()` как третье условие при exactMatch — позволяет найти методы класса по его имени даже если `fileName` и `declarationName` не совпадают с именем класса.

---

### `MarkdownChunkingStrategy` (файлы `.md`)

**Подход**: split по заголовкам `#`, `##`, `###`.

**Алгоритм**:
1. Split по `(?=\n#{1,3} )` — каждый заголовок начинает новый чанк.
2. `declarationName` = текст заголовка без `#` и пробелов.
3. `packageName = ""`, `parentScope = null`.
4. Если нет заголовков (plain text .md) — fallback на `FixedSizeStrategy`.
5. Oversized секция — sub-split через `FixedSizeStrategy`.

**Ранее**: `.md` обрабатывался `StructuralStrategy`, которая ничего не находила → весь файл как один блок → sub-split посимвольно.

---

### `FixedSizeStrategy` (fallback и `strategy="fixed"` индекс)

**Подход**: sliding window с overlap.

- `chunkSize = 1000`, `overlap = 200` (для `strategy="fixed"` индекса)
- `chunkSize = maxChunkSize`, `overlap = maxChunkSize / 5` (как fallback внутри других стратегий)
- Используется для `.txt` и неизвестных расширений через `LanguageAwareChunker`.

---

### Переключение стратегий

```bash
# Старая стратегия (regression check)
USE_AST_CHUNKING=false FORCE_REINDEX=true CODE_PATH=... ./gradlew :rag-server:run

# Новая AST стратегия (полный прогон с метаданными)
USE_AST_CHUNKING=true FORCE_REINDEX=true EXTRACT_METADATA=true CODE_PATH=... ./gradlew :rag-server:run
```

Оба режима пишут в `strategy="structural"` — поисковый pipeline (`TwoStageSearchService`, `HybridSearchService`) не меняется.

---

## Пресеты

| Пресет              | queryOpt | strategy  | topK | threshold | rerank    | finalTopK |
|---------------------|----------|-----------|------|-----------|-----------|-----------|
| `BASELINE`          | true     | TWO_STAGE | 10   | —         | NONE      | 5         |
| `FILTERED`          | true     | TWO_STAGE | 15   | 0.66      | NONE      | 5         |
| `RERANKED_HEURISTIC`| true     | TWO_STAGE | 15   | 0.66      | HEURISTIC | 5         |
| `RERANKED_LLM`      | true     | TWO_STAGE | 15   | 0.66      | LLM       | 5         |

---

## Анализ архитектуры и рекомендации

### Сильные стороны

- **Чистый интерфейс `PipelineStep`** — добавление нового шага не требует изменения существующего кода.
- **Immutable context** — `PipelineContext` передаётся как data class, каждый шаг возвращает копию. Нет скрытых мутаций.
- **Метрики встроены в executor** — измерение времени каждого шага централизовано, без дублирования в шагах.
- **Fallback-цепочка в TwoStageSearchService** — корректная деградация от embedding → keyword → standard при отсутствии векторов.

### Проблемы расширяемости

**1. Конфигурация захардкожена в `buildPipeline` (`RagServer.kt`)**

Логика сборки пайплайна смешана с HTTP-сервером. При добавлении нового шага нужно редактировать `RagServer.kt`, а не только модуль шага.

*Рекомендация:* выделить `PipelineFactory` / `PipelineBuilder` — отдельный класс, принимающий `PipelineConfig` и возвращающий `PipelineExecutor`. Убрать `buildPipeline` из `main`.

**2. `RetrievalStep` зависит от двух конкретных сервисов**

Шаг принимает `TwoStageSearchService` и `SearchService` напрямую, а не через интерфейс. При добавлении третьей стратегии нужно менять `RetrievalStep`.

*Рекомендация:* ввести интерфейс `SearchStrategy` с методом `search(query, topK)`, вынести выбор реализации во фабрику. `RetrievalStep` зависит только от интерфейса.

**3. `RerankStrategy.NONE` порождает `error("unreachable")`**

Ветка `NONE` в `buildReranker` выбрасывает исключение — это признак того, что `NONE` является "маркером-отсутствием", а не стратегией.

*Рекомендация:* использовать `Reranker?` (nullable) вместо `RerankStrategy.NONE`, либо паттерн Null Object (`NoopReranker`), который просто возвращает исходный список.

**4. Keyword-буст и веса зафиксированы магическими константами**

`KEYWORD_BOOST_MAX = 0.2`, `methodBoost = 0.1`, веса гибридного поиска `0.7/0.3`, бонус heuristic reranker `0.1` — разбросаны по разным классам без единого места конфигурации.

*Рекомендация:* вынести в `TwoStageConfig` / `HybridSearchConfig` с разумными defaults. Это упростит эксперименты и A/B-тестирование пресетов.

**5. `ContextPacker` с фиксированным `tokenLimit`**

Лимит `6000` — константа в конструкторе без связи с конфигом пайплайна. Разные модели имеют разные контекстные окна.

*Рекомендация:* пробросить `tokenLimit` через `PipelineConfig` или `RagConfig`.

**6. Метрики неполные**

`countAfterRerank` не отражает реальное изменение — количество после реранка совпадает с количеством до (реранк только переупорядочивает). Нет метрики `countAfterTopK`.

*Рекомендация:* переименовать `countAfterRerank` → `countBeforeTopK`, добавить `queryOptimized: Boolean` и `optimizedQuery: String?` в метрики для трассировки.

### Итоговые приоритеты

| Приоритет | Что сделать |
|-----------|-------------|
| Высокий   | Выделить `PipelineFactory` из `RagServer.kt` |
| Высокий   | Интерфейс `SearchStrategy` вместо двух конкретных сервисов в `RetrievalStep` |
| Средний   | Убрать `RerankStrategy.NONE` как enum-значение, заменить `Reranker?` |
| Средний   | Вынести магические константы в конфиг |
| Низкий    | Пробросить `tokenLimit` через `PipelineConfig` |
| Низкий    | Исправить метрику `countAfterRerank` |
