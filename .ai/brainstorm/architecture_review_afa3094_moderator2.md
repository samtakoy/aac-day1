# Multi-Agent Brainstorm: Architecture Review — Commit afa3094

## Задача
Провести анализ commit afa3094, сфокусировавшись на:
1. Двух массивах сообщений в AIAgent.kt (`llmMessages`, `newMessages`)
2. Хранении сообщений в HitlSession.kt
3. Предложить создание wrapper-класса для управления списками без добавления новых свойств в AIAgent

---

## 🤖 AI Systems Architect (включаясь)

Проанализируй системный дизайн AIAgent: как организован tool-calling loop, как управляется память (memory messages vs conversation messages), и как работает HITL session flow. 

**Фокус:** Есть ли системная проблема с двумя списками сообщений? Предложи стратегию решения.

**Анализ:**

Смотрю на AIAgent.kt (строки 46-76):

```kotlin
private suspend fun buildLlmContext(prompt: AContextMessage): LlmContext {
    val memoryMessages = memoryProvider.getMemoryContext()
    val promptMessages = memoryProvider.appendUserPrompt(prompt)
    val snapshot = strategy.process(config, contextRepository)

    val llmMessages = buildList {
        addAll(memoryMessages.map { it.toModelRequestMessage() })
        addAll(snapshot.messages.toModelRequestMessages())
        addAll(promptMessages.context.filter { it.content.isNotBlank() }.map { it.toModelRequestMessage() })
        if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
    }.toMutableList()

    val newMessages = buildList {
        addAll(snapshot.messages.toModelRequestMessages())
        if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
    }.toMutableList()

    return LlmContext(llmMessages, newMessages, snapshot)
}
```

**Системная проблема:** Оба списка (`llmMessages` и `newMessages`) модифицируются параллельно в `runToolLoop()`:
- Строки 99, 111: `newMessages.add()` и `llmMessages.add()` вызываются вместе
- Это создаёт риск рассинхронизации если разработчик забудет добавить в один из списков

**HITL Flow (resumeWithDecisions, строки 140-173):**
```kotlin
val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)
llmMessages.addAll(session.loopMessages)
val newMessages = session.loopMessages.toMutableList()  // ⚠️ БАГ!
```

**Проблема:** `newMessages` строится только из `session.loopMessages`, теряя базовый контекст `snapshot.messages`.

**Рекомендация:** 
1. Обернуть оба списка в класс-обёртку
2. Добавить методы для синхронного добавления
3. Использовать `copy()` для HITL resume

---

## 🏗 Senior Architect (включаясь)

Оцени архитектурную чистоту текущего решения с двумя списками. Предложи 2-3 варианта рефакторинга.

**Анализ:**

Текущий `LlmContext` — это data class с открытыми mutable list:
```kotlin
private data class LlmContext(
    val llmMessages: MutableList<ModelRequest.Message>,
    val newMessages: MutableList<ModelRequest.Message>,
    val snapshot: ContextStrategyResult
)
```

**Нарушения:**
- **Encapsulation:** MutableList экспонируется наружу
- **Single Responsibility:** AIAgent должен управлять бизнес-логикой, а не синхронизацией списков

**Варианты рефакторинга:**

| Вариант | Описание | Преимущества | Недостатки |
|---------|----------|--------------|------------|
| **A: MessageContext** | Wrapper-класс с методами `addToBoth()`, `addToLlmOnly()` | Капсуляция, тестируемость | Дополнительный класс |
| **B: Single List + Flags** | Один список с `MessageHolder(msg, shouldPersist)` | Простота | Runtime overhead, флаги в рантайме |
| **C: Three Lists** | `memoryMessages`, `snapshotMessages`, `newMessages` | Явность | Больше复杂性 |

**Рекомендация:** Вариант A — MessageContext

---

## 💻 Kotlin Senior Developer (включаясь)

Ты — Senior Kotlin Developer. Пользователь хочет, чтобы AIAgent НЕ получал новых свойств для хранения списков. 

Предложи конкретную реализацию MessageContext, которая:
1. Приватно хранит оба списка
2. Предоставляет методы для управления списками
3. Может быть использован для HITL resume через `copy()`

**Реализация:**

```kotlin
/**
 * Encapsulates message management for LLM context and persistence.
 * AIAgent does NOT hold message lists as properties.
 */
class MessageContext private constructor(
    private val llmMessages: MutableList<ModelRequest.Message>,
    private val persistMessages: MutableList<ModelRequest.Message>,
    val snapshot: ContextStrategyResult
) {
    // Add to LLM only (e.g., memory messages - not re-persisted)
    fun addToLlmOnly(message: ModelRequest.Message) {
        llmMessages.add(message)
    }
    
    // Add to both contexts (most common case - assistant responses)
    fun addToBoth(message: ModelRequest.Message) {
        llmMessages.add(message)
        persistMessages.add(message)
    }
    
    // Add multiple messages to LLM only
    fun addAllToLlmOnly(messages: List<ModelRequest.Message>) {
        llmMessages.addAll(messages)
    }
    
    // Add multiple messages to both
    fun addAllToBoth(messages: List<ModelRequest.Message>) {
        llmMessages.addAll(messages)
        persistMessages.addAll(messages)
    }
    
    // For LLM calls
    fun getLlmMessages(): List<ModelRequest.Message> = llmMessages.toList()
    
    // For persistence (strategy.afterResponse)
    fun getPersistMessages(): List<ModelRequest.Message> = persistMessages.toList()
    
    // Create mutable copy for modification (used in HITL resume)
    fun copy(): MessageContext = MessageContext(
        llmMessages.toMutableList(),
        persistMessages.toMutableList(),
        snapshot
    )
    
    companion object {
        fun create(
            memoryMessages: List<AContextMessage>,
            snapshotMessages: List<AContextMessage>,
            contextMessages: List<AContextMessage>,
            promptMessage: AContextMessage?,
            snapshot: ContextStrategyResult
        ): MessageContext {
            val llmMsgs = mutableListOf<ModelRequest.Message>()
            val persistMsgs = mutableListOf<ModelRequest.Message>()
            
            // Memory → LLM only (already persisted)
            llmMsgs.addAll(memoryMessages.map { it.toModelRequestMessage() })
            
            // Snapshot + Context → both
            val snapshotConverted = snapshotMessages.map { it.toModelRequestMessage() }
            llmMsgs.addAll(snapshotConverted)
            persistMsgs.addAll(snapshotConverted)
            
            val contextConverted = contextMessages.map { it.toModelRequestMessage() }
            llmMsgs.addAll(contextConverted)
            persistMsgs.addAll(contextConverted)
            
            // Prompt → both
            if (promptMessage != null && promptMessage.content.isNotBlank()) {
                val promptConverted = promptMessage.toModelRequestMessage()
                llmMsgs.add(promptConverted)
                persistMsgs.add(promptConverted)
            }
            
            return MessageContext(llmMsgs, persistMsgs, snapshot)
        }
    }
}
```

**Как используется в AIAgent:**

```kotlin
suspend fun process(prompt: AContextMessage, onEvent: ...): Result<ProcessResult> {
    val messageContext = MessageContext.create(
        memoryMessages = memoryProvider.getMemoryContext(),
        snapshotMessages = strategy.process(config, contextRepository).messages,
        contextMessages = memoryProvider.appendUserPrompt(prompt).context,
        promptMessage = prompt,
        snapshot = strategy.process(config, contextRepository)
    )
    return runToolLoop(runId, messageContext, onEvent)
}

internal suspend fun runToolLoop(
    runId: String,
    messageContext: MessageContext,
    onEvent: ...
): Result<ProcessResult> {
    while (loopCount < MAX_TOOL_LOOPS) {
        val request = OrchestratorRequest(
            messages = messageContext.getLlmMessages(),
            // ...
        )
        // ...
        when (result) {
            is OrchestratorResult.Completed -> {
                messageContext.addToBoth(result.assistantMessage)  // Синхронно!
            }
        }
    }
}
```

---

## 📝 Промежуточный итог

### Принятые решения:
1. ✅ Создать MessageContext wrapper-класс
2. ✅ AIAgent НЕ получает новых свойств для списков
3. ✅ MessageContext.kotllin приватизирует списки
4. ✅ Методы `addToBoth()` и `addToLlmOnly()` обеспечивают синхронность

### Открытые вопросы:
1. ❓ Как быть с `resumeWithDecisions()` — использовать `copy()`?
2. ❓ Удалять ли `prompt` из HitlSession?

---

## 🔍 Quality Reviewer (включаясь)

Проведи финальную проверку. Оцени:
1. Не является ли MessageContext overengineering?
2. Соблюдён ли SOLID?
3. Есть ли High Severity issues?

**Анализ:**

**Veto Criteria:**
| Критерий | Статус |
|----------|--------|
| >3 уровня абстракции | ✅ 2 уровня (AIAgent → MessageContext) |
| AI/LLM не обоснован | ✅ Обоснован (tool orchestration) |
| Нет стратегии тестирования | ✅ Тесты есть |
| Циклические зависимости | ✅ Нет |
| Overengineering | ✅ НЕ обнаружен — класс фокусированный (SRP) |

**Issues Found:**

| Severity | Issue | File | Line |
|----------|-------|------|------|
| **HIGH** | Bug: `newMessages` incorrectly derived in resume | AIAgent.kt | 151 |
| Low | Redundant `prompt` field | HitlSession.kt | 14 |

**Рекомендация:** 
1. MessageContext APPROVED
2. Bug fix REQUIRED
3. Cleanup optional

---

## ⚔️ Дебаты: Senior Architect отвечает Quality Reviewer

### Senior Architect:
> "Мой MessageContext — это НЕ overengineering. Это инкапсуляция. Кто угодно может случайно сделать `llmMessages.add()` без `newMessages.add()` и получить рассинхронизацию. MessageContext делает это невозможным."

### Quality Reviewer:
> "Согласен. Класс фокусированный, одно职责. Использование `copy()` для HITL resume — элегантное решение."

---

## 📝 Промежуточный итог #2

### Consensus:
1. ✅ MessageContext APPROVED — NOT overengineering
2. ✅ Bug fix в resumeWithDecisions() — HIGH priority
3. ✅ HitlSession.prompt — redundant, can remove

### Actions:
1. Fix bug in resumeWithDecisions()
2. Create MessageContext.kt
3. Refactor AIAgent to use MessageContext
4. Remove redundant prompt from HitlSession

---

## ✅ Итоговое решение

### Что решено:

| Решение | Детали |
|---------|--------|
| **MessageContext** | Wrapper-класс, приватные списки, методы addToBoth/addToLlmOnly |
| **Bug fix** | resumeWithDecisions() line 151 — newMessages теряет базовый контекст |
| **Cleanup** | HitlSession.prompt — redundant |

### Приоритеты:

| Priority | Действие | Файл |
|----------|---------|------|
| **P0** | Исправить `newMessages` derivation | AIAgent.kt:151 |
| **P1** | Создать MessageContext | MessageContext.kt (NEW) |
| **P1** | Рефакторить AIAgent на MessageContext | AIAgent.kt |
| **P2** | Удалить prompt из HitlSession | HitlSession.kt |

### Конкретные изменения:

**1. AIAgent.kt:151 — MUST FIX:**
```kotlin
// БЫЛО (баг):
val newMessages = session.loopMessages.toMutableList()

// СТАЛО (исправлено):
val newMessages = buildList {
    addAll(snapshot.messages)
    addAll(session.loopMessages)
}.toMutableList()
```

**2. NEW: MessageContext.kt:**
```kotlin
class MessageContext private constructor(
    private val llmMessages: MutableList<ModelRequest.Message>,
    private val persistMessages: MutableList<ModelRequest.Message>,
    val snapshot: ContextStrategyResult
) {
    fun addToBoth(message: ModelRequest.Message) { ... }
    fun addToLlmOnly(message: ModelRequest.Message) { ... }
    fun getLlmMessages(): List<ModelRequest.Message> { ... }
    fun getPersistMessages(): List<ModelRequest.Message> { ... }
    fun copy(): MessageContext { ... }
    companion object { fun create(...): MessageContext { ... } }
}
```

**3. HitlSession.kt:**
```kotlin
// УДАЛИТЬ:
val prompt: AContextMessage,  // redundant - first msg in loopMessages
```

---

**Все согласны? Если да — финальный отчёт готов.**