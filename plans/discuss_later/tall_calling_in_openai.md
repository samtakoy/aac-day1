Отлично — это уже уровень “настоящей платформы”, а не просто чат-сервера.

Разберём **tool calling строго по OpenAI spec**, без упрощений и так, чтобы оно реально работало с разными моделями.

---

# 🧠 0. Что значит “по OpenAI spec”

Современный OpenAI API использует:

```json
"tools": [...]
```

и ответ модели:

```json
"tool_calls": [...]
```

👉 это НЕ старый `function_call`, а новый механизм

---

# 🧩 1. Контракт (Request)

## 📥 ChatCompletionRequest (расширенный)

```kotlin
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val tools: List<ToolDefinition>? = null,
    val tool_choice: JsonElement? = null,
    val stream: Boolean = false
)
```

---

## 🔧 ToolDefinition (строго по spec)

```kotlin
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDef
)

@Serializable
data class FunctionDef(
    val name: String,
    val description: String? = null,
    val parameters: JsonObject // JSON Schema
)
```

---

## 🧰 Пример tool (как видит модель)

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "Get weather by city",
    "parameters": {
      "type": "object",
      "properties": {
        "city": { "type": "string" }
      },
      "required": ["city"]
    }
  }
}
```

---

# 📤 2. Ответ модели (tool_calls)

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "tool_calls": [
          {
            "id": "call_123",
            "type": "function",
            "function": {
              "name": "get_weather",
              "arguments": "{\"city\":\"Berlin\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ]
}
```

---

## DTO

```kotlin
@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: ToolFunctionCall
)

@Serializable
data class ToolFunctionCall(
    val name: String,
    val arguments: String // JSON STRING (важно!)
)
```

---

# ⚠️ КРИТИЧЕСКИЙ момент

```kotlin
arguments: String
```

👉 это НЕ JsonObject
👉 это строка JSON

---

# 🧠 3. Сообщение tool (обратный ответ)

После выполнения tool ты обязан отправить:

```json
{
  "role": "tool",
  "tool_call_id": "call_123",
  "content": "Weather is sunny"
}
```

---

## DTO

```kotlin
@Serializable
data class Message(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null
)
```

---

# 🔄 4. Полный цикл (правильный)

```text
1. user → request (с tools)
2. LLM → tool_calls
3. backend → выполняет tool
4. backend → добавляет role=tool
5. LLM → финальный ответ
```

---

# 🏗 5. Реализация AgentService (правильная)

```kotlin
fun process(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

    // 1. первый вызов (без стрима!)
    val first = llm.chat(request.copy(stream = false)).toList()

    val toolCalls = extractToolCalls(first)

    if (toolCalls.isEmpty()) {
        first.forEach { emit(it) }
        return@flow
    }

    // 2. выполняем tools
    val toolMessages = toolCalls.map { call ->

        val tool = registry.get(call.function.name)
            ?: error("Tool not found")

        val argsJson = Json.parseToJsonElement(call.function.arguments).jsonObject

        val result = tool.execute(argsJson)

        Message(
            role = "tool",
            tool_call_id = call.id,
            content = result
        )
    }

    // 3. второй запрос (уже стрим!)
    val secondRequest = request.copy(
        messages = request.messages +
                   Message(role = "assistant", tool_calls = toolCalls) +
                   toolMessages,
        stream = true
    )

    llm.chat(secondRequest).collect {
        emit(it)
    }
}
```

---

# ⚠️ Почему первый вызов НЕ стрим

Потому что:

👉 тебе нужно получить **целый JSON с tool_calls**

Стрим ломает парсинг.

---

# 🔍 6. extractToolCalls

```kotlin
fun extractToolCalls(chunks: List<ChatCompletionChunk>): List<ToolCall> {

    val message = chunks.lastOrNull()?.choices?.firstOrNull()?.delta

    return message?.tool_calls ?: emptyList()
}
```

(зависит от твоей реализации, но идея такая)

---

# 🧰 7. ToolRegistry (финальный вид)

```kotlin
class ToolRegistry(
    private val tools: Map<String, Tool>
) {

    fun get(name: String): Tool? = tools[name]

    fun definitions(): List<ToolDefinition> =
        tools.values.map { it.definition }
}
```

---

# 🧠 8. Tool interface (production-ready)

```kotlin
interface Tool {

    val definition: ToolDefinition

    suspend fun execute(args: JsonObject): String
}
```

---

# 🔥 9. Интеграция в request

```kotlin
val request = ChatCompletionRequest(
    model = "local/llama3",
    messages = messages,
    tools = toolRegistry.definitions()
)
```

---

# 💣 Частые ошибки (очень важно)

## ❌ 1. Парсить arguments как JSON напрямую

```kotlin
call.function.arguments // это STRING
```

✔ нужно:

```kotlin
Json.parseToJsonElement(...)
```

---

## ❌ 2. Не передавать tool_call_id

→ модель не понимает, какой tool ответ

---

## ❌ 3. Не добавлять assistant с tool_calls

```text
messages += assistant(tool_calls)
```

👉 это ОБЯЗАТЕЛЬНО

---

## ❌ 4. Стримить первый ответ

→ невозможно корректно собрать tool_calls

---

# 🧭 Итог

## Ты реализуешь полный OpenAI flow:

```text
tools → tool_calls → tool → final answer
```

---

## Архитектурно это даёт:

✔ совместимость с OpenAI SDK
✔ совместимость с Koog
✔ расширяемость (любые tools)
✔ поддержку reasoning моделей

---

# 🚀 Если хочешь следующий уровень

Можем дальше сделать:

### 1. 🔥 streaming tool calls (очень сложно, но топ)

### 2. 🧠 auto-retry + tool loops

### 3. 🧩 typed tools (Kotlin → JSON Schema автогенерация)

Это уже уровень production AI платформы.

