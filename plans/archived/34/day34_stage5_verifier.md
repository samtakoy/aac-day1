# Этап 5. OrchestratorWorker — Верификатор шагов пайплайна

## Общее описание

После каждого исполнителя запускать верификатор (отдельный LLM-агент), который оценивает результат шага. Верификатор может:
- пропустить результат (`[OK]`)
- прервать пайплайн с причиной (`[FAIL]`)
- переформулировать задание и запустить исполнителя повторно — ровно один раз (`[RETRY]`)

Верификатор управляется константой `isVerifyOn` в companion object. При `false` — пайплайн работает как раньше, верификатор не вызывается.

---

## Схема работы одного шага пайплайна

```
executeStep(step) → StepOutcome.Success(result)
                         │
                    isVerifyOn?
                    ├── false → results.add(result) → next step
                    └── true  → verifyStep(step, result)
                                    │
                              VerifyOutcome.Ok
                                    → results.add(result) → next step
                              VerifyOutcome.Fail(reason)
                                    → addInfoMessage("❌ Верификация подзадачи N: reason") → return
                              VerifyOutcome.Retry(revisedPrompt)
                                    → addInfoMessage("🔁 Повтор подзадачи N...")
                                    → executeStep(step.copy(userPrompt = revisedPrompt))
                                          ├── Success(result2) → results.add(result2) → next step
                                          └── Failure(msg)     → addInfoMessage("❌ ...") → return
```

Retry — строго один раз. Результат повтора не верифицируется повторно.

---

## Новые сущности

### `VerifyOutcome` (приватный sealed interface в `OrchestratorWorker`)

```kotlin
private sealed interface VerifyOutcome {
    data object Ok : VerifyOutcome
    data class Fail(val reason: String) : VerifyOutcome
    data class Retry(val revisedPrompt: String) : VerifyOutcome
}
```

---

## Изменения в `OrchestratorWorker`

**Файл:** `core/core_features/agent/domain/workers/concrete/OrchestratorWorker.kt`

### companion object — новые константы

```kotlin
const val isVerifyOn = true

private const val VERIFIER_AGENT_NAME = "orchestrator_verifier"

private val VERIFY_SYSTEM_PROMPT = """
Ты верификатор результата подзадачи в пайплайне.

Тебе передаётся описание подзадачи (включая ожидаемый результат) и фактический результат исполнителя.
Твоя задача — оценить, выполнена ли подзадача корректно.

Отвечай ТОЛЬКО одной из трёх форм — никакого другого текста:

[OK]
— результат соответствует ожиданиям подзадачи

[FAIL] <причина>
— результат некорректен и исправить его невозможно (объективный факт: ошибка инструмента, данных нет и т.п.)

[RETRY] <исправленная инструкция для исполнителя>
— результат некорректен, но задачу можно переформулировать. Напиши конкретную исправленную инструкцию.
""".trimIndent()
```

### Изменения в `doWork()` — цикл пайплайна

Текущий код:
```kotlin
is StepOutcome.Success -> {
    results.add(outcome.result)
}
```

Новый код:
```kotlin
is StepOutcome.Success -> {
    val finalResult = if (isVerifyOn) {
        when (val verify = verifyStep(step, outcome.result, chat, onEvent)) {
            is VerifyOutcome.Ok -> outcome.result
            is VerifyOutcome.Fail -> {
                chatTools.addInfoMessage(chat.id, "❌ Верификация подзадачи ${step.index + 1}: ${verify.reason}")
                return
            }
            is VerifyOutcome.Retry -> {
                chatTools.addInfoMessage(chat.id, "🔁 Повтор подзадачи ${step.index + 1}...")
                val retryStep = step.copy(userPrompt = verify.revisedPrompt)
                when (val retryOutcome = executeStep(retryStep, chat, onEvent)) {
                    is StepOutcome.Success -> retryOutcome.result
                    is StepOutcome.Failure -> {
                        chatTools.addInfoMessage(chat.id, "❌ Повтор подзадачи ${step.index + 1}: ${retryOutcome.message}")
                        return
                    }
                }
            }
        }
    } else {
        outcome.result
    }
    results.add(finalResult)
}
```

### Новый метод `verifyStep()`

Верификатор получает **тот же контекст что и исполнитель** — `step.systemPrompt` и `step.userPrompt` уже содержат общее описание задачи, список всех шагов и результаты предыдущих шагов. К ним дописываются инструкции верификации и фактический результат.

```kotlin
private suspend fun verifyStep(
    step: PipelineStep,
    result: SubTaskResult,
    chat: Chat,
    onEvent: (suspend (WorkerEvent) -> Unit)?
): VerifyOutcome {
    val config = JustWorkConfig(
        agentName = "${VERIFIER_AGENT_NAME}_${chat.id}",
        chatId = chat.id,
        systemPrompt = buildVerifierSystemPrompt(step),
        allowedTools = emptyList(),
        defaultModel = { chat.settings.model },
        defaultContext = { AContextDefaultFactory.createFull() },
        recreateAgent = true,
        memoryTypes = emptyList(),
    )

    val text = justWorkWorker.doWork(config, buildVerifierUserPrompt(step, result), onEvent = onEvent)
        .getOrElse { return VerifyOutcome.Ok }  // при ошибке — не блокируем пайплайн

    return parseVerifyOutcome(text)
}
```

### Новые методы построения промптов верификатора

`buildVerifierSystemPrompt` берёт системный промпт исполнителя (общая задача + список шагов) и дописывает инструкции верификации:

```kotlin
private fun buildVerifierSystemPrompt(step: PipelineStep): String = buildString {
    appendLine(step.systemPrompt)
    appendLine()
    appendLine("---")
    appendLine()
    append(VERIFY_SYSTEM_PROMPT)
}
```

`buildVerifierUserPrompt` берёт user-промпт исполнителя (результаты предыдущих шагов + текущая подзадача) и дописывает фактический результат:

```kotlin
private fun buildVerifierUserPrompt(step: PipelineStep, result: SubTaskResult): String = buildString {
    appendLine(step.userPrompt)
    appendLine()
    appendLine("---")
    appendLine()
    appendLine("Фактический результат исполнителя:")
    append(result.result)
}
```

Таким образом верификатор видит ровно то что видел исполнитель — плюс его ответ — и оценивает только текущий шаг.
```

### Новый метод `parseVerifyOutcome()`

```kotlin
private fun parseVerifyOutcome(text: String): VerifyOutcome {
    val trimmed = text.trim()
    return when {
        trimmed.startsWith("[OK]")    -> VerifyOutcome.Ok
        trimmed.startsWith("[FAIL]")  -> VerifyOutcome.Fail(trimmed.removePrefix("[FAIL]").trim())
        trimmed.startsWith("[RETRY]") -> VerifyOutcome.Retry(trimmed.removePrefix("[RETRY]").trim())
        else -> VerifyOutcome.Ok  // fallback: не блокировать пайплайн при нераспознанном ответе
    }
}
```

---

## Поведение при ошибках верификатора

| Ситуация | Поведение |
|---|---|
| Верификатор вернул нераспознанный формат | `VerifyOutcome.Ok` — пайплайн продолжается |
| Верификатор завершился с ошибкой (JustWorkWorker.Failure) | `VerifyOutcome.Ok` — пайплайн продолжается |
| `[FAIL]` | Пайплайн останавливается, выводится причина |
| `[RETRY]`, повтор успешен | Результат повтора добавляется в `results`, пайплайн продолжается |
| `[RETRY]`, повтор провалился | Пайплайн останавливается с сообщением об ошибке повтора |

Принцип: **верификатор не должен ломать пайплайн при своих собственных сбоях** — только при явном `[FAIL]`.

---

## Конфигурация агента-верификатора

| Параметр | Значение |
|---|---|
| `agentName` | `"orchestrator_verifier_${chat.id}"` |
| `allowedTools` | `emptyList()` — верификатор не вызывает инструменты |
| `memoryTypes` | `emptyList()` |
| `recreateAgent` | `true` — каждый вызов с чистой историей |
| `defaultModel` | `{ chat.settings.model }` |

---

## Критерии успеха

- `isVerifyOn = false` → поведение идентично текущему, верификатор не вызывается
- `isVerifyOn = true`, результат корректен → `[OK]`, пайплайн продолжается без изменений
- `isVerifyOn = true`, результат некорректен и исправим → `[RETRY]`, исполнитель перезапускается один раз
- `isVerifyOn = true`, результат некорректен и неисправим → `[FAIL]`, пайплайн останавливается
- Ошибка верификатора не останавливает пайплайн
- Компилируется без ошибок, не затрагивает другие воркеры
