Отлично — это как раз следующий “порог зрелости” системы:
👉 **память (state)** + **tool calling (реальные агенты)**

Сделаем это так, чтобы не пришлось переписывать архитектуру позже.

---

# 🧠 1. Память (DB + session) — правильно с самого начала

## 🎯 Цель

Не просто “сохранять чат”, а:

* давать LLM **контекст**
* контролировать **размер prompt**
* подготовиться к **масштабированию**

---

## 🧩 Архитектура памяти

```text id="3j7t1g"
AgentService
   ├── ChatMemory (interface)
   │     ├── DbMemory
   │     └── (потом RedisMemory)
   └── ChatRepository (persistent storage)
```

---

## 🔑 Интерфейс (самое важное)

```kotlin
interface ChatMemory {

    suspend fun loadContext(sessionId: String): List<Message>

    suspend fun appendMessage(sessionId: String, message: Message)
}
```

---

## 🗄 Реализация через БД

```kotlin
class DbChatMemory(
    private val repo: ChatRepository
) : ChatMemory {

    override suspend fun loadContext(sessionId: String): List<Message> {
        return repo.getLastMessages(sessionId, limit = 20)
    }

    override suspend fun appendMessage(sessionId: String, message: Message) {
        repo.save(message.toEntity(sessionId))
    }
}
```

---

## ⚠️ Критически важно

👉 НЕ загружай всю историю

```kotlin
limit = 20
```

---

## 🔄 Встраивание в AgentService

```kotlin
class AgentService(
    private val llm: LlmProvider,
    private val memory: ChatMemory
) {

    fun process(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

        val sessionId = request.metadata?.get("session_id")
            ?: error("session_id required")

        val history = memory.loadContext(sessionId)

        val fullContext = history + request.messages

        val responseBuffer = StringBuilder()

        llm.chat(request.copy(messages = fullContext))
            .collect { chunk ->

                chunk.choices[0].delta.content?.let {
                    responseBuffer.append(it)
                }

                emit(chunk)
            }

        // сохраняем после завершения
        memory.appendMessage(sessionId, request.messages.last())
        memory.appendMessage(sessionId, Message("assistant", responseBuffer.toString()))
    }
}
```

---

## 🧠 Почему это правильно

✔ память отделена
✔ легко добавить Redis
✔ легко менять стратегию (summary, embeddings)

---

# 🔧 2. Tool Calling (реальные агенты)

Теперь самое интересное 🔥

---

## 🎯 Цель

Позволить модели:

```text
"вызвать функцию"
```

например:

* получить погоду
* сходить в API
* выполнить действие

---

# 🧩 Архитектура

```text id="b5ck38"
AgentService
   ├── ToolRegistry
   ├── ToolExecutor
   └── LlmProvider
```

---

## 🔑 Интерфейс Tool

```kotlin
interface Tool {

    val name: String

    suspend fun execute(args: JsonObject): String
}
```

---

## 🧰 Пример tool

```kotlin
class WeatherTool : Tool {

    override val name = "get_weather"

    override suspend fun execute(args: JsonObject): String {
        val city = args["city"]?.jsonPrimitive?.content
        return "Weather in $city is sunny"
    }
}
```

---

## 📦 Registry

```kotlin
class ToolRegistry(
    tools: List<Tool>
) {
    private val map = tools.associateBy { it.name }

    fun get(name: String) = map[name]
}
```

---

# 🧠 Как LLM вызывает tool

Модель возвращает:

```json
{
  "tool_calls": [
    {
      "name": "get_weather",
      "arguments": {"city": "Berlin"}
    }
  ]
}
```

---

# 🔄 Обработка в AgentService

```kotlin
class AgentService(
    private val llm: LlmProvider,
    private val tools: ToolRegistry
) {

    fun process(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

        val firstResponse = llm.chat(request).toList()

        val toolCall = extractToolCall(firstResponse)

        if (toolCall != null) {

            val tool = tools.get(toolCall.name)
                ?: error("Tool not found")

            val result = tool.execute(toolCall.arguments)

            val secondRequest = request.copy(
                messages = request.messages + Message(
                    role = "tool",
                    content = result
                )
            )

            llm.chat(secondRequest).collect {
                emit(it)
            }

        } else {
            firstResponse.forEach { emit(it) }
        }
    }
}
```

---

# ⚠️ Важный момент

👉 Tool calling = **двухшаговый процесс**

```text id="osbyqj"
1. LLM решает вызвать tool
2. ты исполняешь tool
3. LLM формирует финальный ответ
```

---

# 🔥 Улучшение: стриминг + tools

Правильный вариант:

* сначала detect tool call (без стрима)
* потом стрим финального ответа

---

# 🧠 Связка Memory + Tools

```text id="55g2o0"
Memory → даёт контекст
Tools → дают возможности
LLM → принимает решения
```

---

# ⚡ Итоговая архитектура

```text id="4m1iyh"
Client
   ↓
API (OpenAI)
   ↓
AgentService
   ├── Memory (DB)
   ├── ToolRegistry
   └── LlmProvider
```

---

# 🚀 Что ты получил

## ✔ Настоящий AI backend

* память (stateful)
* инструменты (actions)
* стриминг
* расширяемость

---

# 💣 Частые ошибки

## ❌ Сохранять каждый токен

→ убивает БД

---

## ❌ Давать LLM весь чат

→ дорого + медленно

---

## ❌ Выполнять tools “на лету” в стриме

→ ломает поток

---

# 🧭 Следующий логичный шаг

Если идти дальше по зрелой архитектуре:

### 👉 1. Function calling (OpenAI spec полностью)

### 👉 2. Summarization memory (чтобы не рос контекст)

### 👉 3. Redis как hot memory

---

Если хочешь — дальше можем:

* разобрать **как сделать tool calling строго по OpenAI spec**
* или сделать **умную память (summary + embeddings)**


