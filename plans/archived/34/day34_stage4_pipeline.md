# Этап 4. OrchestratorWorker — Пайплайн выполнения подзадач

## Общее описание

Расширить `OrchestratorWorker` второй фазой: после декомпозиции запускать последовательный цикл worker-агентов по подзадачам. Каждый воркер получает контекст всей задачи в системном промпте и накопленные результаты предыдущих шагов в user-промпте. Результат каждого шага выводится в чат как INFO-сообщение. Итог работы оркестратора — `"✅ Ok"` или сообщение об ошибке.

---

## Схема взаимодействия

```
OrchestratorWorker.doWork()
│
├── Phase 1 (существующий): JustWorkWorker → OrchestratorResultParser → INFO-блоки
│
└── Phase 2 (новый): если subtasks не пусты
        │
        ├── i=0: systemPrompt=buildWorkerSystemPrompt(parsed)
        │         userPrompt=buildWorkerUserPrompt(subtasks, [], 0)
        │         JustWorkWorker.doWork() → result
        │         addInfoMessage("✅ Подзадача 1:\n[result]")
        │
        ├── i=1: userPrompt=buildWorkerUserPrompt(subtasks, [result0], 1)
        │         JustWorkWorker.doWork() → result
        │         addInfoMessage("✅ Подзадача 2:\n[result]")
        │
        ├── ошибка на любом шаге
        │         addInfoMessage("❌ Ошибка подзадачи N: [message]") → return
        │
        └── все ок → addInfoMessage("✅ Ok")
```

---

## Новые сущности

### `SubTaskResult` (приватный data class в `OrchestratorWorker`)

Используется только внутри воркера для накопления результатов цикла.

Поля:
- `index: Int` — номер подзадачи (1-based)
- `subtask: String` — текст подзадачи
- `result: String` — ответ воркера

---

## Изменения в `OrchestratorWorker`

**Файл:** `core/core_features/agent/domain/workers/concrete/OrchestratorWorker.kt`

### Константы (companion object)

Добавить:
- `WORKER_AGENT_NAME = "orchestrator_worker"` — имя агента для воркеров подзадач

### Phase 2 в `doWork()`

После существующего вывода INFO-блоков (taskDescription + subtasks), добавить:

```
if (parsed.subtasks.isNotEmpty()) {
    val results = mutableListOf<SubTaskResult>()
    
    for ((index, subtask) in parsed.subtasks.withIndex()) {
        addInfoMessage("⚙️ Подзадача ${index+1}/${total}...")
        
        val config = JustWorkConfig(
            agentName = WORKER_AGENT_NAME + "_${chat.id}",
            chatId = chat.id,
            systemPrompt = buildWorkerSystemPrompt(parsed),
            allowedTools = emptyList(),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true,
            memoryTypes = emptyList(),
        )
        
        justWorkWorker.doWork(config, buildWorkerUserPrompt(parsed.subtasks, results, index), onEvent = onEvent)
            .onSuccess { text ->
                results.add(SubTaskResult(index + 1, subtask, text))
                addInfoMessage("✅ Подзадача ${index+1}:\n$text")
            }
            .onFailure { error ->
                addInfoMessage("❌ Ошибка подзадачи ${index+1}: ${error.message}")
                return  // стоп пайплайна
            }
    }
    
    addInfoMessage("✅ Ok")
}
```

### `buildWorkerSystemPrompt(parsed: OrchestratorParsedResult): String`

Приватный метод. Формирует системный промпт воркера.

Структура результата:
```
[taskDescription, если не null]

Ты работаешь как часть пайплайна из N шагов:
1. [subtask1]
2. [subtask2]
...

Выполняй только свою подзадачу. Результаты предыдущих шагов переданы в запросе.
```

Параметры:
- `parsed: OrchestratorParsedResult`

### `buildWorkerUserPrompt(subtasks, previousResults, currentIndex): String`

Приватный метод. Формирует user-промпт воркера с нарастающим контекстом.

Параметры:
- `subtasks: List<String>`
- `previousResults: List<SubTaskResult>`
- `currentIndex: Int`

Структура результата:

Если `previousResults` не пусты:
```
Результаты выполненных подзадач:

Подзадача 1: [subtask1 text]
Результат: [result1]

Подзадача 2: [subtask2 text]
Результат: [result2]

---
```

Затем всегда:
```
Твоя текущая задача (подзадача N):
[subtasks[currentIndex]]
```

---

## Конфигурация воркера подзадачи

| Параметр | Значение | Обоснование |
|---|---|---|
| `agentName` | `"orchestrator_worker_${chat.id}"` | Одно имя, `recreateAgent=true` гарантирует чистую историю |
| `allowedTools` | `emptyList()` | Нет фильтра — все инструменты доступны |
| `memoryTypes` | `emptyList()` | Без ToolCallHelper — LLM использует инструменты сама |
| `recreateAgent` | `true` | Каждый воркер стартует с чистой историей |
| `defaultModel` | `{ chat.settings.model }` | Та же модель что у оркестратора |

---

## Поведение при ошибке

- Ошибка на шаге N → `addInfoMessage("❌ Ошибка подзадачи N: [message]")` → выход из цикла
- Следующие подзадачи не выполняются
- Оркестратор не выводит `"✅ Ok"`

---

## Результат и критерии успеха

**Что получим:** рабочий пайплайн последовательного выполнения подзадач с накопленным контекстом.

**Критерии успеха:**
- `@@task` с многошаговой задачей → оркестратор декомпозирует → последовательно запускает воркеры
- Каждый воркер видит результаты всех предыдущих шагов в user-промпте
- Каждый шаг отображается в чате отдельным INFO: прогресс + результат
- Ошибка на шаге N → пайплайн останавливается, следующие шаги не запускаются
- Успешное завершение → последнее сообщение `"✅ Ok"`
- Компилируется без ошибок, не затрагивает AssistantWorker и другие воркеры
