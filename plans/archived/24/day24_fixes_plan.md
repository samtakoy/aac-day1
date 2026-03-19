# День 25 — Метаданные классов в контексте + сигнатуры методов: План Реализации

## Продуктовая задача

Улучшить качество RAG-ответов за счёт трёх изменений:

1. **Починить `ClassGroup.responsibility`** — поле есть, `ContextFormatter` его показывает, но `ContextPacker` никогда не заполняет. Метаданные извлекаются LLM при индексации, но в ответ не попадают.
2. **Добавить `keyMethods` в контекст** — LLM видит список ключевых методов класса даже если чанк конкретного метода не попал в топ.
3. **Добавить `params` в `MethodInfo`** — LLM и Stage 1 keyword-search видят точную сигнатуру метода, а не только имя.

---

## Анализ текущего состояния

### Broken pipe: metadata есть, но не отображается

```
IndexingService.extractMetadataForAll()
  → LLM → ClassMetadata { responsibility, keyMethods, dependencies, domainTags }
  → db.saveClassMetadata()

TwoStageSearchService.findRelevantClasses()
  → db.getAllClassMetadata()           ← метаданные используются для Stage 1 поиска
  → (Stage 1) embedding similarity по responsibility
  → (Stage 2) drill-down в чанки

ContextPacker.pack()
  → группирует SearchResult по className
  → ClassGroup(className, filePath, chunks, topScore, responsibility = null)   ← ВСЕГДА NULL
  → PackedContext

ContextFormatter.format()
  → group.responsibility?.let { appendLine("Responsibility: $it") }  ← никогда не выполняется
```

`ClassGroup.responsibility` определён в [ContextPacker.kt:57](../../../rag-server/src/main/kotlin/com/example/day/ragserver/search/context/ContextPacker.kt#L57) но `pack()` не делает DB-запрос.

### Текущий вывод ContextFormatter (без метаданных):
```
[CLASS] RagCommandHandler
File: .../RagCommandHandler.kt
Score: 0.821

--- handleRuntest [ИСТОЧНИК 1] (line 138) ---
<code>
```

### Желаемый вывод (с метаданными):
```
[CLASS] RagCommandHandler
File: .../RagCommandHandler.kt
Score: 0.821
Responsibility: Обрабатывает @@talk(rag ...) команды для RAG-пайплайна
Key methods: handleOn(), handleOff(), handleRuntest(agentId, presetArg, chat), handleRunparamtest(agentId, paramsArg, chat)

--- handleRuntest [ИСТОЧНИК 1] (line 138) ---
<code>
```

---

## Что НЕ меняем

- `dependencies` — не добавляем в контекст (граф зависимостей — перегруз для типичных вопросов, LLM видит их в коде)
- `TwoStageSearchService` — логика поиска не меняется
- `PipelineConfig` и пресеты — никаких новых параметров
- `formatFlat()` — не трогаем
- Логика переиндексации — `FORCE_REINDEX=true` уже существует, пользователь запустит вручную для получения params

---

## Файлы, которые изменяем

| Файл | Тип изменения |
|------|---------------|
| `db/ClassMetadata.kt` | Добавить `params: String?` в `MethodInfo` |
| `indexing/MetadataValidator.kt` | Валидация нового поля `params` |
| `indexing/MetadataExtractor.kt` | Обновить схему в промпте LLM |
| `search/context/ContextPacker.kt` | Добавить `keyMethods` в `ClassGroup`; заполнять из DB |
| `pipeline/steps/ContextPackingStep.kt` | Передать `CodeDatabase` в `ContextPacker` |
| `search/context/ContextFormatter.kt` | Показывать `keyMethods` в заголовке класса |
| `RagServer.kt` | Передать `db` в `ContextPacker` при создании |

---

## Детали реализации

### 1. `ClassMetadata.kt` — добавить `params` в `MethodInfo`

```kotlin
@Serializable
data class MethodInfo(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("params") val params: String? = null,  // "agentId: Long, presetArg: String?, chat: Chat"
)
```

`params: String? = null` — nullable с дефолтом, чтобы не сломать существующие записи в БД (JSON `ignoreUnknownKeys = true` уже включён).

### 2. `MetadataValidator.kt` — добавить валидацию `params`

В `keyMethods.map { it.copy(...) }`:
```kotlin
.map { it.copy(
    name = it.name.trim(),
    description = it.description.trim().take(150),
    params = it.params?.trim()?.take(200),  // ограничиваем длину
) }
```

### 3. `MetadataExtractor.kt` — обновить схему в промпте

Изменить схему `key_methods`:
```
"key_methods": [{"name": string, "description": string, "params": string|null}],
```

Добавить правило:
```
- key_methods[].params: parameter list as "name: Type, name: Type" or null if no params
```

Пример в промпте помогает LLM не галлюцинировать формат:
```
"key_methods": [
  {"name": "handleRuntest", "description": "runs test questions through RAG pipeline", "params": "agentId: Long, presetArg: String?, chat: Chat"},
  {"name": "currentUrl", "description": "returns configured server URL", "params": "agentId: Long"}
]
```

### 4. `ContextPacker.kt` — заполнять `ClassGroup` из DB

**Изменение `ClassGroup`** — добавить поле keyMethods:
```kotlin
data class ClassGroup(
    val className: String,
    val filePath: String,
    val chunks: List<ChunkEntity>,
    val topScore: Float,
    val responsibility: String? = null,
    val keyMethods: List<MethodInfo> = emptyList(),  // новое поле
)
```

**Изменение `ContextPacker`** — принимает `CodeDatabase`, заполняет поля:
```kotlin
class ContextPacker(
    private val db: CodeDatabase? = null,    // nullable для обратной совместимости с тестами
    private val tokenLimit: Int = 6000,
) {
    fun pack(results: List<SearchResult>): PackedContext {
        val byClass = results.groupBy { resolveClassName(it.chunk) }
        val sortedGroups = byClass.entries
            .sortedByDescending { (_, chunks) -> chunks.maxOf { it.score } }

        val groups = mutableListOf<ClassGroup>()
        var usedTokens = 0

        for ((className, chunks) in sortedGroups) {
            if (usedTokens >= tokenLimit) break

            val uniqueChunks = chunks
                .map { it.chunk }
                .distinctBy { it.content.trim().hashCode() }
                .sortedBy { it.startLine }

            val groupTokens = uniqueChunks.sumOf { estimateTokens(it.content) }
            if (usedTokens + groupTokens > tokenLimit && groups.isNotEmpty()) continue

            val metadata = db?.getClassMetadata(className)   // DB-запрос по имени класса

            groups.add(
                ClassGroup(
                    className = className,
                    filePath = uniqueChunks.first().filePath,
                    chunks = uniqueChunks,
                    topScore = chunks.maxOf { it.score },
                    responsibility = metadata?.responsibility?.takeIf { it.isNotBlank() },
                    keyMethods = metadata?.keyMethods ?: emptyList(),
                )
            )
            usedTokens += groupTokens
        }

        return PackedContext(groups = groups, totalTokens = usedTokens)
    }
    // ... остальное без изменений
}
```

Импорт `MethodInfo` в ContextPacker нужен для типа поля. Добавить:
```kotlin
import com.example.day.ragserver.db.ClassMetadata  // уже есть в TwoStageSearchService
import com.example.day.ragserver.db.MethodInfo
```

### 5. `ContextPackingStep.kt` — передать `db`

```kotlin
class ContextPackingStep(private val packer: ContextPacker) : PipelineStep {
    override val name = "pack"
    override suspend fun process(ctx: PipelineContext): PipelineContext =
        ctx.copy(packed = packer.pack(ctx.results))
}
```

Шаг не меняется — изменение инкапсулировано в `ContextPacker`.

### 6. `RagServer.kt` — передать `db` в `ContextPacker`

В функции `buildPipeline`:
```kotlin
// БЫЛО:
add(ContextPackingStep(ContextPacker()))

// СТАНЕТ:
add(ContextPackingStep(ContextPacker(db = db)))
```

### 7. `ContextFormatter.kt` — показывать `keyMethods`

После строки `group.responsibility?.let { appendLine("Responsibility: $it") }` добавить:

```kotlin
if (group.keyMethods.isNotEmpty()) {
    val methodsLine = group.keyMethods.joinToString(", ") { m ->
        if (m.params != null) "${m.name}(${m.params})" else "${m.name}()"
    }
    appendLine("Key methods: $methodsLine")
}
```

Импортировать `MethodInfo`:
```kotlin
import com.example.day.ragserver.db.MethodInfo
```

---

## Ожидаемый формат вывода

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] RagCommandHandler
File: .../RagCommandHandler.kt
Score: 0.821
Responsibility: Обрабатывает @@talk(rag ...) команды для настройки RAG-пайплайна
Key methods: handleOn(agentId: Long), handleOff(agentId: Long), handleRuntest(agentId: Long, presetArg: String?, chat: Chat), handleRunparamtest(agentId: Long, paramsArg: String?, chat: Chat)

--- handleRuntest [ИСТОЧНИК 1] (line 138) ---
<code>
```

---

## Порядок реализации

1. `ClassMetadata.kt` — добавить `params: String?` в `MethodInfo`
2. `MetadataValidator.kt` — добавить валидацию `params`
3. `MetadataExtractor.kt` — обновить схему и правила в промпте
4. `ContextPacker.kt` — добавить `keyMethods` в `ClassGroup`; принять `db`; заполнять metadata
5. `RagServer.kt` — передать `db` в `ContextPacker(db = db)`
6. `ContextFormatter.kt` — отобразить `keyMethods` в заголовке класса

---

## Важные детали

### Обратная совместимость БД
- Существующие записи в `class_metadata` не имеют `params` — но `ignoreUnknownKeys = true` + `params: String? = null` обрабатывают это автоматически
- Старые записи будут показывать `handleRuntest()` без параметров — это нормально
- После `FORCE_REINDEX=true` все записи получат `params`

### Токены
- Каждая строка `Key methods:` добавляет ~50-150 токенов на класс
- При лимите 6000 и обычных 2-3 классах в контексте — некритично
- `ContextPacker` уже считает только токены чанков, не заголовков — это ok

### Транзакции DB
- `db.getClassMetadata(className)` — уже реализован в `CodeDatabase.kt:155`
- Вызывается внутри `transaction {}` в impl — потокобезопасен

---

## Верификация (ручная)

Запустить `@@talk(rag --runtest baseline)`:

| Критерий | Ожидание |
|----------|----------|
| В заголовке класса есть `Responsibility:` | ✅ для каждого класса с метаданными |
| В заголовке класса есть `Key methods:` | ✅ для каждого класса с keyMethods |
| Методы с params показывают сигнатуры | ✅ после FORCE_REINDEX (опционально) |
| Ответы LLM ссылаются на конкретные методы из Key methods | ✅ более точные ссылки |
| Без FORCE_REINDEX сервер не падает | ✅ `params = null` → `handleRuntest()` без сигнатуры |
