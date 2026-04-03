# Этап 1. OrchestratorResultParser

## Общее описание

Создать чистый парсер ответа LLM без каких-либо зависимостей (не использует DI, не обращается к репозиториям, только строковые операции).

Агент-декомпозитор возвращает текст в формате с тегами:
```
[TASK_START]
Общее описание задачи
[TASK_END]
[SUBTASKS_START]
Подзадача 1: ...
[SUBTASKS_END]
[SUBTASKS_START]
Подзадача 2: ...
[SUBTASKS_END]
```

Парсер должен уметь:
- Извлекать содержимое `[TASK_START]...[TASK_END]` (может отсутствовать)
- Извлекать список содержимых каждого `[SUBTASKS_START]...[SUBTASKS_END]`
- Возвращать `null` если никаких тегов нет — значит ответ не в ожидаемом формате

---

## Задача этапа

### Новый файл

**Путь:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/OrchestratorResultParser.kt`

**Пакет:** `com.example.day.core.core_features.agent.domain.workers.concrete`

### Структура данных `OrchestratorParsedResult`

Data class (можно в том же файле):

```
data class OrchestratorParsedResult(
    val taskDescription: String?,   // содержимое [TASK_START]...[TASK_END], обрезанное trim(); null если тег отсутствует
    val subtasks: List<String>      // список содержимого каждого [SUBTASKS_START]...[SUBTASKS_END], trim() каждого элемента
)
```

### Класс `OrchestratorResultParser`

Оформить как `object` (нет состояния, нет зависимостей).

**Метод:**
```
fun parse(rawText: String): OrchestratorParsedResult?
```

**Алгоритм:**

1. Проверить наличие тега `[TASK_START]` ИЛИ `[SUBTASKS_START]` в тексте.
   Если ни одного — вернуть `null`.

2. Извлечь `taskDescription`:
   - Найти индекс `[TASK_START]` и `[TASK_END]` в тексте
   - Если оба найдены → взять подстроку между ними, применить `trim()`
   - Если не найдены → `null`

3. Извлечь `subtasks`:
   - Найти все вхождения пар `[SUBTASKS_START]...[SUBTASKS_END]`
   - Для каждой пары — взять содержимое между тегами, применить `trim()`
   - Итог — `List<String>` (пустой список если ни одной пары нет)

4. Вернуть `OrchestratorParsedResult(taskDescription, subtasks)`

**Подход к поиску пар `[SUBTASKS_START]...[SUBTASKS_END]`:**

Итеративно обходить текст: найти `[SUBTASKS_START]`, затем от этой позиции найти `[SUBTASKS_END]`, вырезать содержимое, продолжить поиск с позиции после `[SUBTASKS_END]`.

### Константы тегов

Вынести в companion object (или в верхний уровень файла) чтобы не дублировать строки:
- `TAG_TASK_START = "[TASK_START]"`
- `TAG_TASK_END = "[TASK_END]"`
- `TAG_SUBTASKS_START = "[SUBTASKS_START]"`
- `TAG_SUBTASKS_END = "[SUBTASKS_END]"`

---

## Результат и критерии успеха

**Что получим:** изолированный парсер без зависимостей, пригодный для unit-тестирования.

**Критерии успеха:**
- Полный ответ с `[TASK_START]` и несколькими `[SUBTASKS_START]` → парсится корректно
- Ответ только с `[SUBTASKS_START]` (без `[TASK_START]`) → `taskDescription = null`, subtasks заполнены
- Ответ без тегов → возвращает `null`
- Теги присутствуют, но содержимое пустое → возвращает результат с пустыми строками/пустым списком (не `null`)
- Компилируется без ошибок, нет внешних импортов кроме stdlib
