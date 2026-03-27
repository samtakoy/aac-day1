# День 25. Мини-чат с RAG + памятью — Общий план

## Принятые решения (согласованы с пользователем)

| Вопрос | Решение |
|---|---|
| Short History | Вариант B: TaskState LLM также возвращает `last_response_summary` |
| LLM для TaskState | POST /task-state/update добавляется в существующий `rag-server` (отдельная папка, кандидат на переезд в AgentServer) |
| TaskState структура | Упрощённый функционально полный вариант (см. ниже) |
| RagServer расширение | Входит в этот день |
| Декомпозиция провайдеров | Новый `RagContextMemoryProvider` оборачивает `AutoRagMemoryProvider` |
| DebugInfo | Команда `debuginfo` в чате → info-сообщение с TaskState + short history |

## TaskState JSON структура

```json
{
  "current_focus": { "file": "", "class": "", "method": "" },
  "tech_stack": "Kotlin, Android, ...",
  "intent": "general|debugging|architecture|implementation",
  "context_switched": false,
  "confirmed_decisions": [],
  "open_questions": []
}
```

## Архитектурная схема

```
User → RagTalkDelegate → RagWorker
                              ↓
                    aiAgentFactory.getOrCreate(agent)
                              ↓
                    AIAgent.process(prompt)
                              ↓
                    RagContextMemoryProvider.appendUserPrompt()
                         ├── TaskStateUpdater.update(last_messages, current_state)
                         │      └── POST rag-server/task-state/update (Ollama)
                         │           returns { updated_state, last_response_summary }
                         │
                         ├── AutoRagMemoryProvider.appendUserPrompt()
                         │      └── GET rag-server/search?query=...&preset=reranked_llm
                         │           &task_state=...&history=...
                         │
                         └── result → LLM Chat (OpenRouter)
                              ↓
                    RagContextMemoryProvider.postProcess()
                         └── сохранить last_response_summary в short_history
```

## Этапы реализации

### Этап 1: Android — инфраструктура RAG-группы чатов
**Файл:** `plans/day25_stage1.md`
**Результат:** Можно создать группу чатов типа RAG_CONTEXT, открыть чат, написать сообщение (пока без RAG — просто разговор с LLM + ContextSummaryStrategy)
**Статус:** ✅ реализован

### Этап 2: AgentServer endpoint + TaskState
**Файл:** `plans/day25_stage2.md`
**Результат:** `POST rag-server/task-state/update` работает. RagWorker обновляет TaskState при каждом запросе через `RagContextMemoryProvider`.
**Статус:** ✅ реализован

### Этап 3: RagContextMemoryProvider + Short History
**Файл:** `plans/day25_stage3.md`
**Результат:** `RagContextMemoryProvider` оборачивает AutoRag, хранит short_history. `debuginfo` показывает текущий TaskState.
**Статус:** ✅ реализован

### Этап 4: RagServer /search расширение + QueryOptimizer
**Файл:** `plans/day25_stage4.md`
**Результат:** `/search` принимает `preset`, `task_state`, `history`. QueryOptimizer использует контекст задачи.
**Статус:** ✅ реализован

### Этап 5: Логирование
**Файл:** `plans/day25_stage5.md`
**Результат:** Все ключевые шаги логируются с тегом `(rag)(ktor)`.
**Статус:** ✅ реализован

## Связи между этапами

```
Этап 1 (инфра) → Этап 2 (TaskState) → Этап 3 (Short History)
                                               ↓
Этап 4 (RagServer) ←──────────────────────────┘  (нужны данные из Short History)
Этап 5 (Логи) — параллелен, но лучше в конце
```

## Ключевые файлы кодовой базы (шаблоны)

- `PlannerTalkDelegate` → шаблон для `RagTalkDelegate`
- `TaskWorker` → шаблон для `RagWorker`
- `PlannerConsoleFeatureEntryImpl` → шаблон для `RagConsoleFeatureEntryImpl`
- `AutoRagMemoryProvider` → оборачивается в `RagContextMemoryProvider`
- `AgentRulesMemoryProvider` → паттерн хранения в `AgentMemoryRepository`
- `ContextSummaryStrategy` → используется как есть в `RagWorker`
- `rag-server/indexing/OllamaLlmProvider.kt` → шаблон для TaskState endpoint
