Отлично — давай сделаем это **сразу правильно и “на вырост”**:

1. точный **OpenAI-compatible контракт (Ktor + DTO + streaming)**
2. **интеграция с Ollama без костылей**, чтобы можно было переключать провайдеры

---

# 🧱 1. OpenAI-compatible API (что именно реализовать)

Минимально необходимое:

```http
POST /v1/chat/completions
GET  /v1/models
```

---

## 📥 Request (Chat Completions)

```kotlin
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,

    // расширяемое поле (ВАЖНО)
    val metadata: Map<String, String>? = null
)

@Serializable
data class Message(
    val role: String, // "user", "assistant", "system"
    val content: String
)
```

---

## 📤 Response (non-stream)

```kotlin
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String?
)
```

---

## 🌊 Streaming (SSE как у OpenAI)

Формат:

```text
data: {json}
data: {json}
data: [DONE]
```

---

## DTO для стрима

```kotlin
@Serializable
data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>
)

@Serializable
data class ChunkChoice(
    val delta: Delta,
    val index: Int,
    val finish_reason: String? = null
)

@Serializable
data class Delta(
    val content: String? = null
)
```

---

# ⚙️ Ktor endpoint (SSE)

```kotlin
post("/v1/chat/completions") {
    val request = call.receive<ChatCompletionRequest>()

    if (request.stream) {
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {

            llm.chat(request).collect { chunk ->
                val json = json.encodeToString(chunk)
                write("data: $json\n\n")
                flush()
            }

            write("data: [DONE]\n\n")
        }

    } else {
        val full = llm.chat(request).toList()
        val text = full.joinToString("") { it.choices[0].delta.content.orEmpty() }

        call.respond(
            ChatCompletionResponse(
                id = "chatcmpl-123",
                choices = listOf(
                    Choice(0, Message("assistant", text), "stop")
                )
            )
        )
    }
}
```

---

# 🧠 2. LlmProvider (ядро гибкости)

```kotlin
interface LlmProvider {
    fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk>
}
```

---

# 🔌 3. Ollama Provider (без костылей)

Ollama уже почти OpenAI, но:

* другой endpoint (`/api/chat`)
* другой streaming формат

👉 делаем адаптер

---

## 🔄 Маппинг request

```kotlin
private fun ChatCompletionRequest.toOllama(): OllamaRequest {
    return OllamaRequest(
        model = this.model,
        messages = this.messages.map {
            OllamaMessage(it.role, it.content)
        },
        stream = true
    )
}
```

---

## 📥 Ollama response (stream)

Ollama стримит примерно так:

```json
{
  "message": {
    "role": "assistant",
    "content": "Hello"
  },
  "done": false
}
```

---

## 🔄 Маппинг в OpenAI chunk

```kotlin
private fun OllamaChunk.toOpenAI(): ChatCompletionChunk {
    return ChatCompletionChunk(
        id = "chatcmpl-local",
        choices = listOf(
            ChunkChoice(
                index = 0,
                delta = Delta(content = this.message?.content),
                finish_reason = if (done) "stop" else null
            )
        )
    )
}
```

---

## 🚀 Реализация

```kotlin
class OllamaProvider(
    private val client: HttpClient
) : LlmProvider {

    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

        val response: HttpResponse = client.post("http://localhost:11434/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request.toOllama())
        }

        response.bodyAsChannel().toFlow().collect { rawChunk ->

            val ollamaChunk = parse<OllamaChunk>(rawChunk)

            emit(ollamaChunk.toOpenAI())
        }
    }
}
```

---

# 🌐 4. OpenRouter Provider (для миграции)

```kotlin
class OpenRouterProvider(
    private val client: HttpClient
) : LlmProvider {

    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

        val response = client.post("https://openrouter.ai/api/v1/chat/completions") {
            headers {
                append("Authorization", "Bearer $apiKey")
            }
            setBody(request)
        }

        response.bodyAsChannel().toFlow().collect { chunk ->
            emit(parse(chunk))
        }
    }
}
```

---

# 🔀 5. Runtime переключение (очень важно)

```kotlin
class LlmRouter(
    private val openRouter: OpenRouterProvider,
    private val ollama: OllamaProvider
) : LlmProvider {

    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> {
        return when {
            request.model.startsWith("local/") -> ollama.chat(request)
            else -> openRouter.chat(request)
        }
    }
}
```

---

# 🧠 6. Модель naming strategy

```text
local/llama3
openai/gpt-4
openrouter/mistral
```

👉 ты контролируешь routing

---

# 📦 7. /v1/models endpoint

```kotlin
get("/v1/models") {
    call.respond(
        mapOf(
            "data" to listOf(
                mapOf("id" to "local/llama3"),
                mapOf("id" to "openrouter/mistral")
            )
        )
    )
}
```

---

# ⚡ 8. Важные детали, которые обычно ломают систему

## ❗ 1. Не стримь raw Ollama

ВСЕГДА нормализуй в OpenAI формат

---

## ❗ 2. Не смешивай API и провайдер

```text
Controller ≠ LLM client
```

---

## ❗ 3. Делай metadata passthrough

```kotlin
metadata: Map<String, String>
```

👉 туда потом пойдут:

* user_id
* session_id
* agent config

---

## ❗ 4. Всегда используй Flow

Это даёт тебе:

* backpressure
* отмену
* streaming

---

# 🧭 Итоговая архитектура

```text
Android
   ↓
OpenAI-compatible API (Ktor)
   ↓
Agent Layer
   ↓
LlmProvider (Router)
   ├── Ollama
   └── OpenRouter
```

---

# 🔥 Что ты получаешь

✔ миграция OpenRouter → Ollama без боли
✔ один API для клиента
✔ возможность добавить любые модели
✔ готовность к микросервисам

---

