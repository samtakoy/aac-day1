# Task Context: ToolCallOrchestrator UITL Implementation

> Created: 2026-03-21
> Status: In Progress (paused for future session)

---

## Original Task

Проанализировать проект, проанализировать `ToolCallOrchestratorImpl.kt` - его логику работы с openrouter ai, зависимости по цепочке. Предложить архитектурные улучшения, предложить как добавить User in The Loop.

---

## Что было сделано

### Анализ текущей архитектуры

Изучены следующие файлы:
- `ToolCallOrchestratorImpl.kt` - основной orchestration
- `ToolCallOrchestrator.kt` - интерфейс
- `ToolProvider.kt`, `McpToolProvider.kt` - tool execution
- `LlmRequestUseCase.kt`, `LlmRequestUseCaseImpl.kt` - LLM calls
- `ModelRequest.kt`, `ModelResult.kt` - модели
- `WorkerEvent.kt` - события от workers
- `AIAgent.kt`, `AIAgentFactory.kt` - agent creation
- `TaskWorker.kt` - task execution
- `ConsoleScreen.kt`, `ConsoleFeatureEntry.kt` - UI
- `PlannerTalkDelegate.kt` - delegation pattern

### Выявленные проблемы

| Priority | Issue | Description |
|----------|-------|-------------|
| P0 | Missing `name` field | Tool response не содержит `name` - нарушение OpenRouter spec |
| P1 | No User in The Loop | Все tool calls выполняются автоматически |
| P2 | Magic constant `MAX_TOOL_LOOPS` | Hardcoded = 3 |
| P3 | No idempotency | Duplicate tool calls могут выполниться дважды |

### Созданные файлы плана

1. `plans/toolcall-orchestrator-analysis.md` - общий обзор
2. `plans/phase-1-fix-name-field.md` - исправление name field
3. `plans/phase-2-uitl-core.md` - полная реализация UITL
4. `plans/phase-3-config-idempotency.md` - конфигурация + idempotency
5. `plans/phase-4-future-enhancements.md` - будущие улучшения

---

## Ключевые архитектурные решения (принятые)

### 1. AgentProtocol абстракция

UITL реализован через `AgentProtocol` интерфейс, позволяющий:
- **LocalAgentProtocol** - локальное выполнение
- **RemoteAgentProtocol** (future) - удалённое выполнение

### 2. SessionApprovalCache

`SessionApprovalCache` хранит решения пользователя "approve all for session" на уровне client-side.

### 3. BottomSheet вместо Dialog

Material 3 `ModalBottomSheet` для approval - less intrusive.

---

## Открытые вопросы и решения

### Вопрос 1: AgentProtocol - premature abstraction?

**Было**: AgentProtocol для support remote agent
**Решение**: Сохранить - это позволяет scale к remote agent в будущем без изменения UITL/UI слоёв

### Вопрос 2: SemanticToolCallDeduplicator

**Было**: Предложен semantic deduplication
**Решение**: Удалён - OpenRouter возвращает структурированный JSON, достаточно simple exact-match cache

### Вопрос 3: Parallel tool execution

**Было**: Предложена parallel execution
**Решение**: Отложено - sequential sufficient для большинства случаев

---

## НОВЫЕ Требования (из последней дискуссии)

### Critical: Request-Response Mode (НЕ callbacks/channels)

**Требование**: 
- AWorker НЕ должен быть жёстко связан с UI
- Режим работы: request-response-request-response...
- Никаких прямых callbacks и channels
- Должно работать без изменений при выносе AIAgent на сервер

**Текущий подход (callback-based)** - НЕПРИЕМЛЕМ:
```kotlin
// НЕПРАВИЛЬНО - жёсткая связь
approvalCallback.requestApproval(info)  // suspend до решения пользователя
```

**Правильный подход** - Request-Response:
```kotlin
// ПРАВИЛЬНО - Worker эмитит события, UI решает когда показывать
fun doWork(): Flow<WorkerEvent> = flow {
    emit(WorkerEvent.ToolApprovalRequired(...))
    emit(WorkerEvent.ToolApprovalRequired(...))  // может быть много
    // Здесь нужен способ "приостановиться" и ждать решения
}
```

### Nested Tool Calls

**Требование**: tool_A может вызывать tool_B

Это влияет на:
- Approval flow - нужен стек approvals
- Состояние worker - checkpoint должен включать call stack

---

## Proposed Solution: Shared State + Flow

### Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  UITLState (Shared State)                                         │
│                                                                    │
│  pendingApprovals: StateFlow<Map<String, ToolCallInfo>>          │
│  decisions: StateFlow<Map<String, ApprovalDecision>>              │
└──────────────────────────────────────────────────────────────────┘
                    ▲                    │
                    │                    ▼
┌──────────────────────────────────────────────────────────────────┐
│  TaskWorker (AWorker)                                            │
│                                                                    │
│  fun doWork(): Flow<WorkerEvent> {                               │
│      emit(WorkerEvent.ToolApprovalRequired(call))                  │
│      waitForDecision(call.id)  // suspend until in decisions      │
│  }                                                               │
└──────────────────────────────────────────────────────────────────┘
                    ▲
                    │ WorkerEvent
                    ▼
┌──────────────────────────────────────────────────────────────────┐
│  ViewModel                                                        │
│                                                                    │
│  Collect events → показывать UI                                   │
│  submitDecision(decision) → кладём в decisions StateFlow          │
└──────────────────────────────────────────────────────────────────┘
```

### Преимущества

1. **No direct coupling** - Worker и ViewModel общаются через Shared State
2. **Remote-ready** - UITLState можно синхронизировать с backend
3. **Request-Response** - Worker эмитит события, UI управляет когда показывать
4. **Nested support** - pendingApprovals может быть Map<String, ToolCallInfo> со stack-подобной семантикой

### Недостатки

1. **Polling** - текущий подход использует StateFlow polling
2. **State management** - нужно carefully manage state lifecycle

---

## Что нужно сделать

1. **Упростить Phase 2** - убрать AgentProtocol, использовать Shared State подход
2. **Спроектировать waitForDecision** - proper suspend вместо polling
3. **Обработать nested tool calls** - поддержка tool_A → tool_B

---

## Следующие шаги

1. Обсудить polling vs proper suspend/resume
2. Обсудить nested tool calls impact
3. Переписать Phase 2 с новым подходом
4. Реализовать упрощённый Iteration 1

---

## Команды для продолжения

```bash
# Список созданных файлов
ls plans/

# Читать текущий план
cat plans/toolcall-orchestrator-analysis.md

# Читать context
cat plans/task-context.md
```

---

## Критичные файлы для изучения при продолжении

```
app/src/main/java/com/example/day/core/core_features/agent/domain/
├── tools/impl/ToolCallOrchestratorImpl.kt
├── workers/concrete/TaskWorker.kt
└── domain/AIAgent.kt

app/src/main/java/com/example/day/features/console/
├── impl/ui/ConsoleScreen.kt
├── impl/ui/viewmodel/ConsoleViewModel.kt
└── impl/ui/delegates/PlannerTalkDelegate.kt
```
