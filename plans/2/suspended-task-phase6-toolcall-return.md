# Phase 6 - User in Loop: ToolCall Return Problem

## Status: SUSPENDED (awaiting user decision)

## Background - Зачем это все делалось

**Проект Day** - Android AI chat приложение с multi-agent архитектурой. Основано на идеях JetBrains Koog framework (смотри `jetbrains-koog-agents.md`).

**Проблема которую решали:**
Когда AI агент выполняет dangerous tool (удаление файлов, очистка данных и т.д.), пользователь должен подтвердить действие. Нужна UI прослойка которая:
1. Показывает диалог подтверждения пользователю
2. Ждет решения пользователя
3. После подтверждения выполняет tool и возвращает результат в LLM loop

**Предыдущие достижения (Phases 1-5):**
- ToolRegistry/McpToolRegistry переименованы ✅
- LlmExecutionRequest создан для унификации ✅
- WorkerEvent.UserConfirmation для UI событий ✅
- AIAgent.process() использует LlmExecutionRequest ✅

## Problem Statement

После выполнения ToolCallManager результат выполнения нужно передать Orchestrator/LLM для продолжения диалога.

**Контекст архитектуры:**
- AIAgent.process() stateless - каждый вызов начинает свежий цикл
- ToolCallManager управляет очередью подтверждений (FIFO)
- Orchestrator отдает tool calls в ToolCallManager
- Когда dangerous tool обнаружен: добавляем в очередь, показываем UI, ждем подтверждения
- Когда confirmed: выполняем из очереди в порядке FIFO

**Неразрешенный вопрос:**
Как передать результат выполнения обратно в LLM loop, учитывая stateless природу AIAgent.process()?

## Proposed Options (все признаны плохими пользователем):

1. **A - AIAgent stateful** - хранить state между вызовами
2. **B - Orchestrator stateful** - хранить state  
3. **C - ToolCallManager делает LLM запрос** - нарушает Separation of Concerns
4. **D - Callback/Continuation** - усложняет API

## User's Response

> "все варианты плохие и портят архитектуру"

Задача приостановлена до принятия нового решения от пользователя.

## Current Implementation State

### Уже реализовано (Phases 1-5):

**ToolRegistry.kt** - интерфейс для выполнения tool calls
```kotlin
interface ToolRegistry {
    suspend fun executeToolCall(request: LlmExecutionRequest): LlmExecutionResult
}
```

**McpToolRegistry.kt** - реализация через MCP

**LlmExecutionRequest.kt** - унифицированный request
```kotlin
data class LlmExecutionRequest(
    val messages: List<AContextMessage>,
    val config: LlmExecutionConfig,
    val tools: List<ToolDefinition>
)
```

**WorkerEvent.kt** - UserConfirmation события для UI
```kotlin
sealed interface UserConfirmation {
    data class ToolConfirmationRequested(
        val toolCall: ToolCallContext,
        val confirmationId: String
    ) : UserConfirmation
    
    data class ToolResults(
        val results: List<ToolExecutionResult>,
        val confirmationId: String
    ) : UserConfirmation
}
```

**UI файлы:** PlannerUiEvent.kt, PlannerTalkDelegate.kt, ConsoleViewModel.kt, ConsoleScreen.kt - базовый scaffolding

### Нужно реализовать:

1. **ToolCallManager** - управляет очередью подтверждений, выполняет через ToolRegistry
2. **AgentState** - персистентное состояние для каждого agentId (очередь, подтверждения, сообщения)
3. **Интеграция** - Orchestrator → ToolCallManager → ToolRegistry → результат → LLM loop

## Ключевые файлы затронутые реализацией:

- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallManager.kt` (new - needs implementation)
- `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/AgentState.kt` (new - needs design)
- UI файлы для confirmation dialog (new)

## References

- `jetbrains-koog-agents.md` - оригинальная документация по Koog
- `plans/toolcall-orchestrator-analysis.md` - анализ текущего ToolCallOrchestrator
- `plans/user-confirmation-architecture.md` - предыдущие обсуждения архитектуры

---

**Created:** 2026-03-21
**Reason:** All proposed options for returning tool execution results to LLM loop were rejected as architecture-damaging. Need new approach from user.
**Project:** Day Android AI Chat Application
**Framework inspiration:** JetBrains Koog
