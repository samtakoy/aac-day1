Окей, тогда давай сразу **как в прод-архитектуре**, без упрощений под один сервис и без обсуждения load balancer. Ниже — как встроить БД (Exposed + SQLite сейчас) в твою уже спроектированную систему с Gateway + Agent Service.

---

# 📦 Роль БД в архитектуре

В твоей схеме:

```text
Gateway → Agent Service → Ollama
```

👉 База данных живёт **НЕ в Gateway**, а в:

```text
Agent Service (primary)
+ частично Gateway (read/cache при необходимости)
```

---

# 🧠 Что вообще хранить?

Минимально необходимый набор:

### 1. Чаты

```kotlin
ChatSession
- id
- userId
- createdAt
- updatedAt
```

---

### 2. Сообщения

```kotlin
ChatMessage
- id
- sessionId
- role (user/assistant/system)
- content
- createdAt
```

---

### 3. (опционально, но важно)

```kotlin
MessageMetadata
- tokensUsed
- latency
- model
```

---

# 🏗 Где используется БД

## Agent Service (главное место)

👉 здесь происходит:

* загрузка контекста
* сохранение сообщений
* работа с памятью агента

---

## Gateway (минимально)

👉 только если нужно:

* быстрые статусы
* lightweight кэш

**Но лучше не тащить туда БД на старте**

---

# ⚠️ SQLite в твоей архитектуре

Ты выбрал:

```kotlin
Exposed + SQLite
```

Это нормально для старта, но:

### ❗ Ограничения SQLite

* нет нормальной конкурентной записи
* плохо масштабируется
* один writer

👉 В микросервисах это узкое место

---

## ✔ Рекомендация

### Сейчас (можно оставить)

* SQLite + Exposed

### Но сразу закладывай интерфейс:

```kotlin
interface ChatRepository {
    suspend fun getMessages(sessionId: String): List<ChatMessage>
    suspend fun saveMessage(message: ChatMessage)
}
```

👉 чтобы потом заменить на PostgreSQL без боли

---

# 🧩 Интеграция с Agent Service

## Поток обработки с БД

```text
1. Получили gRPC request
2. Загрузили историю из БД
3. Передали в Koog
4. Стримим ответ
5. Сохраняем ответ в БД
```

---

## Пример (упрощённо)

```kotlin
class AgentOrchestrator(
    private val repo: ChatRepository,
    private val agent: Agent
) {

    fun process(userId: String, sessionId: String, message: String): Flow<ChatEvent> = flow {

        emit(ChatEvent.Status("loading_history"))

        val history = repo.getMessages(sessionId)

        emit(ChatEvent.Status("thinking"))

        agent.executeStreaming(
            history + ChatMessage.user(message)
        ).collect { chunk ->
            emit(ChatEvent.Token(chunk.content))
        }

        // сохраняем
        repo.saveMessage(ChatMessage.user(message))
        repo.saveMessage(ChatMessage.assistant("...assembled response..."))

        emit(ChatEvent.Done)
    }
}
```

---

# 🗄 Реализация через Exposed

## Таблицы

```kotlin
object ChatSessions : Table() {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 64)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ChatMessages : Table() {
    val id = varchar("id", 64)
    val sessionId = varchar("session_id", 64)
    val role = varchar("role", 16)
    val content = text("content")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

---

## Инициализация

```kotlin
Database.connect(
    url = "jdbc:sqlite:chat.db",
    driver = "org.sqlite.JDBC"
)

transaction {
    SchemaUtils.create(ChatSessions, ChatMessages)
}
```

---

## Репозиторий

```kotlin
class ExposedChatRepository : ChatRepository {

    override suspend fun getMessages(sessionId: String): List<ChatMessage> =
        newSuspendedTransaction {
            ChatMessages
                .select { ChatMessages.sessionId eq sessionId }
                .map {
                    ChatMessage(
                        role = it[ChatMessages.role],
                        content = it[ChatMessages.content]
                    )
                }
        }

    override suspend fun saveMessage(message: ChatMessage) {
        newSuspendedTransaction {
            ChatMessages.insert {
                it[id] = message.id
                it[sessionId] = message.sessionId
                it[role] = message.role
                it[content] = message.content
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }
}
```

---

# ⚡ Важная архитектурная тонкость

## ❌ Ошибка

Сохранять токены по мере стриминга

## ✅ Правильно

* стримить → агрегировать → сохранить финальный ответ

---

# 🧠 Контекст (Memory Strategy)

Ты НЕ должен грузить всю историю:

## Правильный подход

```kotlin
last N messages (например 10–20)
```

или:

```kotlin
summary + последние сообщения
```

---

# 🚀 Как эволюционирует БД

## Сейчас

* SQLite
* Exposed

---

## Потом (без изменения архитектуры)

Просто меняешь:

```kotlin
jdbc:postgresql://...
```

и добавляешь:

```kotlin
implementation("org.postgresql:postgresql")
```

👉 весь код остаётся

---

# 🔥 Ключевые выводы

### 1. БД живёт в Agent Service

не в Gateway

---

### 2. SQLite — ок, но временно

закладывай abstraction

---

### 3. Репозиторий обязателен

иначе не мигрируешь

---

### 4. Не сохраняй стрим

только финальный ответ

---

### 5. Контекст ограничивай

иначе LLM станет дорогим и медленным

---

Если хочешь, дальше можем:

* спроектировать **схему под PostgreSQL (с индексами и scaling)**
* или сделать **memory layer (Redis + DB hybrid)** ← это следующий уровень “взрослой” архитектуры

