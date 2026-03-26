# День 28. Локальная LLM + RAG — Уточнённый план реализации

## Цель
Провести сравнительный тест RAG-ответов от локальной LLM (Ollama) и облачной модели в чате типа `RAG_CONTEXT`.
Автоматизировать сбор ответов, отправку отчёта на сервер и запуск качественного сравнения двух отчётов через Ollama.

---

## Схема взаимодействия

```
User → "@@testqueries" в RAG_CONTEXT чате
  → RagTalkDelegate.tryAddUserMessage
      → loop: tryAddUserMessage(chat, question, {})   // рекурсия 1 уровень
          → ragWorker.doWork() + WorkerEvent.RequestSuccess
          → возвращает String? (ответ модели)
      → стоп при null
      → RagWorker.saveTestResults(items, executionTimeMs, chat)
          → ragSearchRepository.saveRuntestResults(preset, items, executionTimeMs, isLocalLlm, url)
          → POST /runtest/save

RagServer.POST /runtest/save:
  → сохранить runtest_RAG_WORKER_{LOCAL|CLOUD}_{timestamp}.md
  → ответить { savedReport }
  → launch { comparisonService.tryRunComparison() }   // async

ComparisonService.tryRunComparison():
  → найти последний LOCAL-отчёт
  → найти последний CLOUD-отчёт
  → если оба найдены:
      → ReportParser.parse(localFile) → List<question, answerLocal>
      → ReportParser.parse(cloudFile) → List<question, answerCloud>
      → ReferenceAnswerParser.parse(answersPath) → Map<Int, String>
      → loop по вопросам:
          → OllamaLlmProvider.generate(тройка: вопрос/ответ1/ответ2/эталон)
          → собрать оценки 0-10 + комментарий
      → сохранить comparison_{timestamp}.md (баллы + средние)
  → иначе: залогировать, пропустить
```

---

## Новые / изменяемые сущности

### Android

#### TalkDelegate (интерфейс)
Метод `tryAddUserMessage(chat, inputText, onSuccess): String?`
- Было: `Unit`
- Стало: `String?` — текст ответа модели (при успехе), `null` (при ошибке или неприменимо)

#### RagTalkDelegate
- `@@debuginfo` — переименование из `debuginfo`
- `@@testqueries` — новая команда
- Захват ответа: `ragWorker.doWork(onEvent)` → `WorkerEvent.RequestSuccess.result`

#### RagWorker
Новый метод `saveTestResults(items: List<Pair<String, String>>, executionTimeMs: Long, chat: Chat): Result<String>`
- Читает server URL из agent memory
- Извлекает `isLocalLlm` из `chat.settings`
- Вызывает `ragSearchRepository.saveRuntestResults(...)`

#### RagSearchRepository (domain + impl)
Метод `saveRuntestResults` расширяется двумя параметрами:
- `executionTimeMs: Long`
- `isLocalLlm: Boolean`

#### RuntestSavePayload (network DTO — Android)
Поля: `preset, items, executionTimeMs, isLocalLlm`

---

### Server (rag-server)

#### RuntestSaveRequest (DTO)
Добавить поля: `isLocalLlm: Boolean`, `executionTimeMs: Long`

#### Именование отчётов
Формат: `runtest_RAG_WORKER_{LOCAL|CLOUD}_{yyyy-MM-dd_HH-mm}.md`

#### ComparisonService
```
ComparisonService(
  ollamaProvider: OllamaLlmProvider,
  reportsDir: String,
  answersPath: String,
)

fun tryRunComparison(): Unit
  - найти последний LOCAL и последний CLOUD отчёт
  - если оба есть → сравнить → сохранить comparison_{timestamp}.md
  - иначе → залогировать и выйти
```

#### ReportParser
```
object ReportParser {
  fun parse(filePath: String): List<Pair<String, String>>   // question → answer
}
```

#### ReferenceAnswerParser
```
object ReferenceAnswerParser {
  fun parse(filePath: String): Map<Int, String>   // 1-based index → answer text
}
```

---

## Перемещение файлов
- `plans/answers_v2.md` → `rag-server/data/answers_v2.md`
- Путь по умолчанию в сервере: `./data/answers_v2.md`
- Переопределяется через env var `REFERENCE_ANSWERS_PATH`

---

## Этапы реализации

| # | Название | Зависимости |
|---|----------|-------------|
| 1 | Фиксы и переименование | — |
| 2 | TalkDelegate: интерфейс + захват ответа | — |
| 3 | Расширение сетевого протокола (Android + Server DTO) | — |
| 4 | Команда @@testqueries + RagWorker.saveTestResults | 2, 3 |
| 5 | Server: ComparisonService + ReportParser + ReferenceAnswerParser | 3 |

Этапы 1, 2, 3 независимы и могут реализовываться в любом порядке.
