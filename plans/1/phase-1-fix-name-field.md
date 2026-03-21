# Phase 1: Исправить Missing `name` Field

> **Статус**: TODO  
> **Приоритет**: P0 (Critical)  
> **Оценка сложности**: Low (1-2 дня)

---

## Мотивация

OpenRouter API требует, чтобы tool response message содержала поле `name`:

```json
{
  "role": "tool",
  "tool_call_id": "call_abc123",
  "name": "get_weather",           // ← КРИТИЧНО
  "content": "{\"temperature\":...}"
}
```

**Текущее состояние**: В коде есть `// TODO нет имени функции` (строка 148), что означает поле `name` не заполняется. Это нарушение спецификации OpenRouter и может приводить к неопределённому поведению.

---

## Цели

1. ✅ Добавить поле `name` в `ModelRequest.Message`
2. ✅ Заполнять `name` при создании tool response message
3. ✅ Проверить, что `LlmRequestUseCaseImpl` правильно передаёт `name` в запрос

---

## Пошаговый план

### Шаг 1: Добавить поле `name` в `ModelRequest.Message`

**Файл**: `app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelRequest.kt`

```kotlin
data class Message(
    val role: Role,
    val content: String,
    val thinking: String? = null,
    val cachePrompt: Boolean? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null  // ← ДОБАВИТЬ
)
```

### Шаг 2: Заполнять `name` в ToolCallOrchestratorImpl

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt`

Найти код создания tool message (около строки 143-150):

```kotlin
toolMessages.add(
    ModelRequest.Message(
        role = ModelRequest.Role.Tool,
        content = content,
        toolCallId = call.id,
        name = call.function.name  // ← ДОБАВИТЬ
    )
)
```

**Удалить TODO комментарий** (строка 148):
```kotlin
// TODO нет имени функции  ← УДАЛИТЬ
```

### Шаг 3: Проверить LlmRequestUseCaseImpl

**Файл**: `app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCaseImpl.kt`

Проверить, что при создании `ModelRequest` поле `name` не теряется. Обычно используется для role=Tool:

```kotlin
// В методе buildList:
if (prompt?.content?.isNullOrBlank() == false) {
    add(prompt.reqUserMessage())  // name не нужен для user message
}
```

---

## Критерии завершения

1. ✅ `ModelRequest.Message` содержит поле `name: String?`
2. ✅ Tool response message содержит `name = call.function.name`
3. ✅ TODO комментарий удалён
4. ✅ Проект компилируется
5. ✅ Unit тесты (если есть) проходят

---

## Файлы для изменения

| Файл | Изменение |
|------|-----------|
| `ModelRequest.kt` | Добавить `name` field |
| `ToolCallOrchestratorImpl.kt` | Заполнять `name` при создании tool message |
| `LlmRequestUseCaseImpl.kt` | Проверить корректность передачи |

---

## Риски

| Риск | Вероятность | Mitigation |
|------|------------|------------|
| Поле `name` уже где-то используется с другим значением | Low | Поиск по проекту перед изменением |
|breaking existing tests | Low | Запустить тесты после изменения |
