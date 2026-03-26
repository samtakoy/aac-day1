Отлично, давай сразу собрать **цельный фундамент**, на который ты реально сможешь опереться дальше:

---

# 🧱 1. Готовый Ktor starter (минимальный, но правильный)

## 📦 Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## 🗂 Структура

```text
src/
├── api/
│   └── ChatRoutes.kt
├── llm/
│   ├── LlmProvider.kt
│   ├── OllamaProvider.kt
│   ├── OpenRouterProvider.kt
│   └── LlmRouter.kt
├── agent/
│   └── AgentService.kt
└── main.kt
```

---

## 🚀 main.kt

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    val httpClient = HttpClient(CIO)

    val ollama = OllamaProvider(httpClient)
    val openRouter = OpenRouterProvider(httpClient, apiKey = "XXX")

    val llm = LlmRouter(openRouter, ollama)
    val agent = AgentService(llm)

    routing {
        chatRoutes(agent)
    }
}
```

---

# 🌊 2. Реальный streaming (без “фейков”)

## 🔧 Extension: ByteReadChannel → Flow

```kotlin
fun ByteReadChannel.toFlow(): Flow<String> = flow {
    while (!isClosedForRead) {
        val line = readUTF8Line() ?: break
        emit(line)
    }
}
```

---

## 📡 SSE endpoint (настоящий стрим)

```kotlin
fun Route.chatRoutes(agent: AgentService) {

    post("/v1/chat/completions") {

        val request = call.receive<ChatCompletionRequest>()

        if (request.stream) {

            call.respondTextWriter(ContentType.Text.EventStream) {

                agent.process(request).collect { chunk ->

                    val json = Json.encodeToString(chunk)

                    write("data: $json\n\n")
                    flush()
                }

                write("data: [DONE]\n\n")
            }

        } else {
            val result = agent.process(request).toList()

            val text = result.joinToString("") {
                it.choices[0].delta.content.orEmpty()
            }

            call.respond(
                ChatCompletionResponse(
                    id = "chatcmpl-1",
                    choices = listOf(
                        Choice(0, Message("assistant", text), "stop")
                    )
                )
            )
        }
    }
}
```

---

## ⚠️ Важно

✔ `flush()` после каждого chunk
✔ `\n\n` обязательно
✔ `[DONE]` в конце

---

# 🧠 3. Подключение Koog (поверх твоей архитектуры)

👉 ключ: Koog НЕ заменяет LlmProvider
👉 он живёт **над ним**

---

## 📦 AgentService с Koog

```kotlin
class AgentService(
    private val llm: LlmProvider
) {

    fun process(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {

        val context = request.messages

        // здесь можно подключить Koog
        llm.chat(request.copy(messages = context))
            .collect { chunk ->
                emit(chunk)
            }
    }
}
```

---

## 🧠 Где реально появляется Koog

Когда добавляешь:

* tools
* reasoning
* multi-step

---

## 🔧 Пример с Koog (упрощённо)

```kotlin
class KoogAgent(
    private val llm: LlmProvider
) {

    fun run(messages: List<Message>): Flow<ChatCompletionChunk> = flow {

        // шаг 1: анализ
        val enriched = messages + Message(
            "system",
            "Ты умный ассистент"
        )

        // шаг 2: вызов модели
        llm.chat(
            ChatCompletionRequest(
                model = "local/llama3",
                messages = enriched,
                stream = true
            )
        ).collect {
            emit(it)
        }
    }
}
```

---

# 🔗 Как всё соединяется

```text
Client (Android)
   ↓
Ktor API (/v1/chat/completions)
   ↓
AgentService (Koog logic)
   ↓
LlmRouter
   ├── OllamaProvider
   └── OpenRouterProvider
```

---

# ⚡ Что у тебя теперь есть

## ✔ Production-ready foundation

* OpenAI-compatible API
* настоящий streaming
* переключаемые LLM
* готовность к Koog

---

## ✔ Без будущего рефакторинга

Ты уже можешь:

* вынести AgentService → в отдельный сервис
* заменить Ollama → GPU cluster
* добавить Redis → без переписывания

---

# 🚀 Следующие логичные шаги

Если идти дальше по “правильному пути”, то:

### 1️⃣ Добавить память (DB + session)

→ чтобы агент не был stateless

### 2️⃣ Добавить tool calling

→ Koog начинает реально работать

### 3️⃣ Добавить cancellation (очень важно)

→ прерывание генерации

---

Если хочешь — дальше могу:

* собрать **полностью рабочий репозиторий (как blueprint)**
* или показать **как реализовать tool calling + function execution** (это следующий уровень)

