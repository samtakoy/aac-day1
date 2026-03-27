# Этап 5. Server: ComparisonService + ReportParser + ReferenceAnswerParser

## Описание
Серверная часть: три новых компонента + асинхронный запуск сравнения после сохранения каждого отчёта.
Зависит от Этапа 3 (файлы LOCAL/CLOUD уже создаются).

## Задачи

### 5.1 Переместить answers_v2.md
- `plans/answers_v2.md` → `rag-server/data/answers_v2.md`
- Env var `REFERENCE_ANSWERS_PATH` (default: `./data/answers_v2.md`) — добавить в `RagConfig`

### 5.2 ReferenceAnswerParser
Файл: `rag-server/src/.../comparison/ReferenceAnswerParser.kt`

```
object ReferenceAnswerParser {
    fun parse(filePath: String): Map<Int, String>
}
```

Логика:
- Читать файл построчно
- Распознать заголовки `## Q{N}:` (regex `## Q(\d+):`)
- Собрать текст до следующего `## Q` или `---` как тело ответа (без блоков кода — или с ними, решить)
- Вернуть `Map<Int, String>` (1-based)

### 5.3 ReportParser
Файл: `rag-server/src/.../comparison/ReportParser.kt`

```
object ReportParser {
    fun parse(filePath: String): List<Pair<String, String>>   // question → llmAnswer
}
```

Логика: парсит markdown-файл отчёта в формате `buildRuntestReport`:
- Заголовок вопроса: `### Вопрос N` или `**Вопрос:**`
- Текст ответа: секция `**Ответ:**`
- Изучить фактический формат `buildRuntestReport` перед реализацией

### 5.4 ComparisonService
Файл: `rag-server/src/.../comparison/ComparisonService.kt`

```
class ComparisonService(
    private val ollamaProvider: OllamaLlmProvider,
    private val reportsDir: String,
    private val answersPath: String,
)

suspend fun tryRunComparison(): Unit
```

Логика `tryRunComparison`:
1. Найти все файлы `$reportsDir/runtest_RAG_WORKER_LOCAL_*.md` → взять последний по имени (timestamp в имени → лексикографический sort)
2. Найти все файлы `$reportsDir/runtest_RAG_WORKER_CLOUD_*.md` → взять последний
3. Если одного нет → `println("[ComparisonService] Нет пары LOCAL/CLOUD для сравнения")` → выйти
4. `val localItems = ReportParser.parse(localFile)`
5. `val cloudItems = ReportParser.parse(cloudFile)`
6. `val refs = ReferenceAnswerParser.parse(answersPath)` — если пустой → лог + выйти
7. `val count = minOf(localItems.size, cloudItems.size, refs.size)`
8. Цикл `i in 0 until count`:
   - `val question = localItems[i].first`
   - `val answerLocal = localItems[i].second`
   - `val answerCloud = cloudItems[i].second`
   - `val refAnswer = refs[i + 1] ?: continue`
   - `val prompt = buildComparisonPrompt(question, answerLocal, answerCloud, refAnswer)`
   - `val evaluation = ollamaProvider.generate(prompt)`
   - Распарсить оценки из `evaluation` (или сохранить raw)
   - Добавить в `results`
9. Сохранить `comparison_{timestamp}.md` с секциями по вопросам + средние баллы в конце

### 5.5 Промпт сравнения

```
COMPARISON_PROMPT_TEMPLATE:

Ты — независимый эксперт, оценивающий качество ответов на технические вопросы.

**Вопрос:** {question}

**Ответ 1:** {answerLocal}

**Ответ 2:** {answerCloud}

**Эталонный ответ:** {refAnswer}

Оцени каждый ответ по шкале от 0 до 10, сравнивая с эталонным.
Критерии: точность, полнота, конкретность, наличие примеров.

Ответь строго в формате:
Оценка 1: X/10
Оценка 2: X/10
Комментарий: ...
```

### 5.6 Формат итогового отчёта comparison_{timestamp}.md

```markdown
# Отчёт сравнения: LOCAL vs CLOUD
**Дата:** {timestamp}
**LOCAL-отчёт:** {localFile}
**CLOUD-отчёт:** {cloudFile}

---

## Вопрос 1: {question}
**Оценка LOCAL:** X/10
**Оценка CLOUD:** X/10
**Комментарий:** ...

...

---

## Итого
| | LOCAL | CLOUD |
|--|--|--|
| Средний балл | X.X | X.X |
| Медиана | X.X | X.X |
```

### 5.7 Подключить ComparisonService в RagServer.kt

В `main()`:
- Создать `comparisonService = ComparisonService(rerankerLlmProvider, reportsDir="./reports", answersPath=config.referenceAnswersPath)`
- В обработчике `POST /runtest/save`, после `call.respond(...)`:
  ```kotlin
  coroutineScope.launch { comparisonService.tryRunComparison() }
  ```

### 5.8 Добавить referenceAnswersPath в RagConfig

Файл: `rag-server/src/.../config/RagConfig.kt`

Поле: `referenceAnswersPath: String = System.getenv("REFERENCE_ANSWERS_PATH") ?: "./data/answers_v2.md"`

---

## Резюме

**Что получим:** после запуска двух тестов (LOCAL + CLOUD) сервер автоматически генерирует сравнительный отчёт через Ollama, сохраняет его в `./reports/comparison_{timestamp}.md`. Отчёт содержит оценки обоих моделей по каждому вопросу и средние баллы.

**Критерии успеха:**
- После двух тестов (LOCAL + CLOUD) файл `comparison_*.md` появляется в `./reports/`
- Файл содержит оценки по всем вопросам и итоговую таблицу
- Если только один тип отчёта (два LOCAL или два CLOUD) — сравнение не запускается, в логах сообщение
- Если `answers_v2.md` не найден — сравнение не запускается, в логах ошибка

---

## Подробный план реализации

### Шаг 1. Переместить answers_v2.md и добавить env var

- Скопировать `plans/answers_v2.md` → `rag-server/data/answers_v2.md`
- В `RagConfig.from()` добавить парсинг `REFERENCE_ANSWERS_PATH`

### Шаг 2. ReferenceAnswerParser

Создать `rag-server/src/.../comparison/ReferenceAnswerParser.kt`.
Тест вручную: распарсить файл, убедиться что Q1-Q29 присутствуют с непустым текстом.

### Шаг 3. Изучить формат buildRuntestReport

Прочитать функцию `buildRuntestReport` в `RagServer.kt`. Реализовать `ReportParser.parse()` на основе реального формата.

### Шаг 4. ReportParser

Создать `rag-server/src/.../comparison/ReportParser.kt`.
Протестировать на уже существующем отчёте из `./reports/` (если есть).

### Шаг 5. ComparisonService

Создать `rag-server/src/.../comparison/ComparisonService.kt`.
Логика поиска файлов: `File(reportsDir).listFiles()?.filter { it.name.startsWith("runtest_RAG_WORKER_LOCAL_") }?.sortedBy { it.name }?.lastOrNull()`

### Шаг 6. Подключить в RagServer.kt

- Добавить coroutineScope (уже есть через `runBlocking`, или использовать `GlobalScope.launch`)
- Инстанцировать `ComparisonService`
- Подключить async trigger

### Шаг 7. Проверка

1. Убедиться что `./reports/` содержит хотя бы один LOCAL и один CLOUD отчёт (из Этапа 4)
2. Запустить новый тест (любой тип) → наблюдать логи сервера
3. Найти `comparison_*.md` в `./reports/`
4. Проверить качество оценок (читаемость, наличие баллов)
