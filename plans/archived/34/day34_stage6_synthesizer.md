# Этап 6. OrchestratorWorker — Финальный агент-синтезатор

## Общее описание

После успешного завершения пайплайна запускается финальный агент-синтезатор.
Он получает полный контекст задачи и все результаты подзадач, формирует итоговый ответ пользователю.

Синтезатор заменяет финальное `addBotMessage("✅ Ok")` — его ответ и есть итоговое BOT-сообщение.

---

## Место в общей схеме

```
OrchestratorWorker.doWork()
│
├── Phase 1: decompose() → INFO-блоки
│
├── Phase 2: pipeline loop
│       ├── executeStep() → INFO
│       ├── verifyStep()  → INFO (если isVerifyOn)
│       └── results.add(...)
│
└── Phase 3 (новый): synthesize()
        └── addBotMessage(result)   ← итоговый ответ пользователю
```

---

## Входные данные синтезатора

**System prompt** — фиксированный `SYNTHESIZE_SYSTEM_PROMPT`.

**User prompt** — строится методом `buildSynthesizerUserPrompt()`:

```
Задача:
[taskDescription если есть, иначе cleanPrompt]

Результаты подзадач:

Подзадача 1: [subtask text]
Результат: [result]

Подзадача 2: [subtask text]
Результат: [result]

...
```

---

## Новые сущности в `OrchestratorWorker`

### Константы (companion object)

```kotlin
private const val SYNTHESIZER_AGENT_NAME = "orchestrator_synthesizer"

private val SYNTHESIZE_SYSTEM_PROMPT = """
Ты финальный верификатор пайплайна.
Тебе переданы описание задачи и результаты всех подзадач.

Твоя цель — сформировать итоговый ответ пользователю:
- Если задача запрашивала данные или информацию — верни их в удобном виде согласно условиям задачи.
- Если задача предполагала выполнение действий — подтверди что все шаги выполнены и кратко резюмируй результат.
- Если какой-то шаг вернул пустой или некорректный результат — сообщи об этом явно.
""".trimIndent()
```

### Изменения в `doWork()`

Заменить:
```kotlin
chatTools.addBotMessage(chat.id, "✅ Ok")
```

На:
```kotlin
synthesize(cleanPrompt, orchestratorResults, results, chat, onEvent)
```

### Новый метод `synthesize()`

```kotlin
private suspend fun synthesize(
    cleanPrompt: String,
    orchestratorData: OrchestratorParsedResult,
    results: List<SubTaskResult>,
    chat: Chat,
    onEvent: (suspend (WorkerEvent) -> Unit)?
) {
    val config = JustWorkConfig(
        agentName = "${SYNTHESIZER_AGENT_NAME}_${chat.id}",
        chatId = chat.id,
        systemPrompt = SYNTHESIZE_SYSTEM_PROMPT,
        allowedTools = emptyList(),
        defaultModel = { chat.settings.model },
        defaultContext = { AContextDefaultFactory.createFull() },
        recreateAgent = true,
        memoryTypes = emptyList(),
    )

    justWorkWorker.doWork(config, buildSynthesizerUserPrompt(cleanPrompt, orchestratorData, results), onEvent = onEvent)
        .fold(
            onSuccess = { text -> chatTools.addBotMessage(chat.id, text) },
            onFailure = { chatTools.addBotMessage(chat.id, "✅ Ok") }  // fallback
        )
}
```

### Новый метод `buildSynthesizerUserPrompt()`

```kotlin
private fun buildSynthesizerUserPrompt(
    cleanPrompt: String,
    orchestratorData: OrchestratorParsedResult,
    results: List<SubTaskResult>,
): String = buildString {
    appendLine("Задача:")
    appendLine(orchestratorData.taskDescription ?: cleanPrompt)
    appendLine()
    appendLine("Результаты подзадач:")
    results.forEach { r ->
        appendLine()
        appendLine("Подзадача ${r.index}: ${r.subtask}")
        appendLine("Результат: ${r.result}")
    }
}
```

---

## Поведение при ошибке синтезатора

Если `JustWorkWorker` вернул ошибку → fallback: `addBotMessage("✅ Ok")`.
Синтезатор не должен блокировать успешный пайплайн.

---

## Конфигурация агента-синтезатора

| Параметр | Значение |
|---|---|
| `agentName` | `"orchestrator_synthesizer_${chat.id}"` |
| `allowedTools` | `emptyList()` — нет вызовов инструментов |
| `memoryTypes` | `emptyList()` |
| `recreateAgent` | `true` |
| `defaultModel` | `{ chat.settings.model }` |

---

## Критерии успеха

- Финальное сообщение пайплайна — ответ синтезатора (BOT), а не `"✅ Ok"`
- Синтезатор видит описание задачи + все результаты подзадач
- Если задача запрашивала данные (список файлов, содержимое и т.п.) — они присутствуют в итоговом ответе
- Ошибка синтезатора → fallback `"✅ Ok"`, пайплайн не падает
- Не затрагивает per-step верификатор и остальные воркеры
