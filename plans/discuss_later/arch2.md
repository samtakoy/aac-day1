# Архитектура серверной части чат-ассистента (Kotlin Microservices)

## 1. Обзор и Цели
Система представляет собой серверную платформу для Android-приложения с чат-ассистентами. Основная цель — перенос логики работы с агентами и LLM с клиента на сервер, обеспечивая масштабируемость, безопасность и единую точку управления моделями.

**Ключевые требования:**
*   **MVP:** Работа как прокси к локальной Ollama, совместимость с протоколом OpenRouter.
*   **Production:** Микросервисная архитектура, поддержка реальном времени (SSE/WebSockets), наблюдаемость (Observability).
*   **Стек:** Kotlin, Ktor, JetBrains Koog, gRPC, SQL (SQLite/PostgreSQL).

---

## 2. Технологический Стек

| Компонент | Технология | Обоснование |
| :--- | :--- | :--- |
| **Язык** | Kotlin | Единый язык для сервера и Android-клиента (KMP). |
| **Web Framework** | Ktor Server | Легковесный, асинхронный (корутинны), нативная поддержка SSE/WS/gRPC. |
| **AI Orchestrator** | JetBrains Koog | Типобезопасная работа с промптами, агентами и цепочками вызовов. |
| **IPC (Internal)** | gRPC + Kotlin Coroutines | Высокая скорость, стриминг токенов, строгая типизация между микросервисами. |
| **Cache/State** | Redis | Хранение контекста чата, статусов (typing/thinking), активных сессий. (ВОЗМОЖНО, но не сразу) |
| **Persistence** | SQL (Exposed) | MVP: SQLite. Prod: PostgreSQL. Хранение истории, пользователей, метаданных. |
| **Inference** | Ollama | Локальный запуск моделей, OpenAI-совместимый API. |
| **Observability** | OpenTelemetry + Micrometer | Трассировка запросов, метрики производительности (токены, latency). |
| **Visualization** | Grafana + Loki + Jaeger | Единый дашборд для логов, метрик и трассировок. |

---

## 3. Топология Микросервисов

Система разделена на три автономных узла для управления нагрузкой и изоляции ответственности.

### 3.1. AI-Gateway (Транспорт и Безопасность)
*   **Роль:** «Лицо» системы для Android-клиента.
*   **Задачи:**
    *   Авторизация (JWT/Headers).
    *   Управление соединениями (HTTP, SSE, WebSocket).
    *   Проксирование запросов в формате OpenRouter (`/v1/chat/completions`).
    *   Трансляция внутренних gRPC потоков в клиентские протоколы.
*   **Зависимости:** Не знает о логике агентов или Ollama. Общается с Agent Service через gRPC.
*   **БД:** Не содержит прямой логики работы с основной БД (только кэш/статусы при необходимости).

### 3.2. Agent-Orchestrator (Бизнес-логика)
*   **Роль:** «Мозг» системы.
*   **Задачи:**
    *   Исполнение агентов JetBrains Koog.
    *   Управление контекстом (загрузка из Redis/DB).
    *   Вызов инструментов (Tools/Functions).
    *   Взаимодействие с Inference Node.
*   **Зависимости:**SQL DB, Ollama Service.
*   **Интерфейс:** gRPC Server (Streaming).

### 3.3. Inference-Node (Исполнение)
*   **Роль:** «Мускулы» (GPU/CPU).
*   **Задачи:** Запуск весов моделей, генерация токенов.
*   **Протокол:** HTTP (OpenAI-compatible API на порту 11434).
*   **Конфигурация (Ollama Env):**
    *   `OLLAMA_HOST=0.0.0.0` (доступ из сети Docker).
    *   `OLLAMA_KEEP_ALIVE=-1` (не выгружать модель).
    *   `OLLAMA_NUM_PARALLEL=4` (батчинг запросов).
    *   `OLLAMA_MAX_LOADED_MODELS=1` (фиксация модели в памяти).

---

## 4. Протоколы и Коммуникация

### 4.1. Клиент ↔ Gateway
Поддерживаются два варианта для гибкости UX и совместимости.

| Протокол | Сценарий | Описание |
| :--- | :--- | :--- |
| **SSE (Server-Sent Events)** | MVP / OpenRouter Compat | Односторонний стриминг. Идеально для совместимости с существующим клиентом OpenRouter. Формат: `data: {...}`. |
| **WebSocket** | Продвинутый чат | Двусторонняя связь. Позволяет прерывать генерацию, слать файлы, получать сложные статусы в реальном времени. |

**Единая модель событий (Shared DTO):**
Для поддержки обоих протоколов используется единая иерархия событий (Sealed Classes):
```kotlin
@Serializable
sealed class ChatEvent {
    data class Status(val state: String) : ChatEvent()       // "thinking", "typing", "error"
    data class Token(val content: String) : ChatEvent()      // Кусочек текста
    data class Metadata(val usage: TokenUsage) : ChatEvent() // Инфо о токенах
    data object Done : ChatEvent()
}
```

### 4.2. Gateway ↔ Agent Service (Internal)
*   **Протокол:** gRPC (HTTP/2).
*   **Преимущества:** Мультиплексирование соединений, бинарная сериализация (Protobuf), нативная поддержка Flow (Stream).
*   **Контракт (chat_service.proto):**
```protobuf
service AgentService {
    rpc ChatStream (StreamRequest) returns (stream StreamResponse);
}
message StreamRequest {
    string user_id = 1;
    string message = 2;
    map<string, string> metadata = 3;
}
message StreamResponse {
    oneof event {
        string token = 1;
        string status = 2;
        string error = 3;
    }
}
```
*   **Трассировка:** Trace ID передается через метаданные gRPC (`ClientInterceptor`) для сквозной наблюдаемости.

### 4.3. Agent Service ↔ Ollama
*   **Протокол:** HTTP REST.
*   **Конфигурация Koog:**
```kotlin
val ollamaConfig = OpenAIModelConfiguration(
    host = "ollama-service",
    port = 11434,
    apiKey = "ollama",
    pathPrefix = "/v1"
)
val chatModel = OpenAIChatModel(modelName = "llama3", configuration = ollamaConfig)
```

---

## 5. Управление Данными (Storage Strategy)

### 5.1. Разделение ответственности
*   **SQL DB (Source of Truth):** Долговременное хранение, транзакции, история чатов, пользователи.
*   **Redis (Operational Memory):** Высокая скорость, TTL, текущий контекст, статусы сессий.

### 5.2. Схема данных (SQL via Exposed)
**Таблицы:**
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
    val role = varchar("role", 16) // user/assistant/system
    val content = text("content")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
```
**Паттерн доступа:** Репозиторий (`ChatRepository`).
*   **MVP:** SQLite (`jdbc:sqlite:chat.db`).
*   **Prod:** PostgreSQL (замена драйвера и URL без изменения кода логики).
*   **Важно:** Сохранение только финального ответа агрегированного сообщения, не каждого токена.

### 5.3. Структура Redis (возможно а будущем)
**Ключи и Назначение:**
1.  **Контекст чата:**
    *   Key: `chat:session:{sessionId}`
    *   Value: JSON `[{"role": "user", "content": "..."}]`
    *   TTL: Зависит от политики памяти (или без TTL для активных).
2.  **Статусы (Typing/Thinking):**
    *   Key: `chat:status:{sessionId}`
    *   Value: String (e.g., "thinking")
    *   TTL: 30 сек (автоочистка если сервис упадет).
3.  **Активные сессии:**
    *   Key: `user:{userId}:sessions`

**Паттерн работы (Read-through / Write-behind):**
1.  Запрос → Проверка Redis на контекст.
2.  Если нет → Загрузка из SQL DB → Кэширование в Redis.
3.  Генерация ответа → Обновление Redis.
4.  Асинхронная запись итогов в SQL DB.

---

## 6. Observability (Наблюдаемость)

В промышленной архитектуре логи не смотрят в файлах, они агрегируются.

### 6.1. Инструментарий
1.  **Micrometer:** Сбор метрик (токены/сек, активные сессии, ошибки). Интеграция с Prometheus.
2.  **OpenTelemetry (OTel):** Распределенная трассировка. связывает запрос Android → Gateway → Agent → Ollama.
3.  **Grafana LGTM Stack:**
    *   **Grafana:** Дашборды.
    *   **Loki:** Хранилище логов (JSON формат).
    *   **Tempo/Jaeger:** Визуализация трассировок.

### 6.2. Ключевые метрики
*   `llm_tokens_per_second`: Скорость генерации (UX).
*   `agent_active_sessions`: Количество открытых WS/SSE.
*   `ollama_queue_size`: Очередь к модели.
*   `time_to_first_token (TTFT)`: Задержка до начала ответа.

### 6.3. Логирование
*   Формат: JSON (Logback + LogstashEncoder).
*   Обязательные поля: `trace_id`, `user_id`, `session_id`.
*   Запрет: Не логировать PII (личные данные) в открытом виде.

---

## 7. Структура Проекта (Gradle Multi-project)

Для обеспечения типобезопасности и переиспользования кода используется следующая структура модулей:

```text
root/
├── build.gradle.kts
├── settings.gradle.kts
├── shared-api/             <-- Kotlin Multiplatform (KMP)
│   └── src/commonMain/     <-- DTO (ChatEvent, Request), валидация
├── shared-grpc/            <-- JVM Only
│   └── src/main/proto/     <-- .proto файлы, генерация stubs
├── gateway-service/        <-- Ktor Server
│   └── deps: shared-api, shared-grpc
├── agent-service/          <-- Koog Logic
│   └── deps: shared-grpc, DB, Redis
└── android-client/         <-- Приложение
    └── deps: shared-api
```

**Преимущества:**
*   **Shared-API:** Если изменить поле JSON на сервере, Android-клиент не скомпилируется. Исключает ошибки интеграции.
*   **Shared-gRPC:** Гарантирует совместимость контрактов между Gateway и Agent.

---

## 8. Дорожная Карта (Roadmap)

### Этап 1: MVP (Proxy)
*   **Архитектура:** Монолитный Ktor сервер (Gateway + Agent логика внутри).
*   **БД:** SQLite.
*   **Redis:** Опционально (только если нужны статусы typing).
*   **Протокол:** SSE (OpenRouter совместимость).
*   **Koog:** Прямой вызов модели Ollama через OpenAI-коннектор.

### Этап 2: Микросервисы и Состояния
*   **Разделение:** Вынос Agent Logic в отдельный сервис.
*   **Коммуникация:** Внедрение gRPC между Gateway и Agent.
*   **БД:** Переход на PostgreSQL.
*   **Кэш:** Внедрение Redis для контекста и статусов. (Redis - под вопросом)
*   **Протокол:** Добавление WebSocket для полного контроля чата.

### Этап 3: Масштабирование и Observability
*   **Infra:** Вынос Ollama на отдельные GPU-ноды за Load Balancer (Nginx).
*   **Monitor:** Подключение Grafana, Loki, Jaeger.
*   **Features:** A/B тестирование агентов (запуск Agent Service v1 и v2 параллельно).

---

## 9. Важные Архитектурные Тонкости

1.  **Backpressure:** Использование `Kotlin Flow` сквозным потоком (Ollama → Koog → gRPC → Ktor → Client). Если клиент отключается, поток закрывается на всех уровнях, экономя ресурсы GPU.
2.  **Stateless Agents:** Agent Service не хранит состояние в памяти. Контекст загружается из Redis/DB на каждый запрос. Это позволяет масштабировать сервис горизонтально.
3.  **Load Balancing:**
    *   **External:** Nginx/Cloud LB перед Gateway (Sticky Sessions для WebSocket).
    *   **Internal:** gRPC Client Side Load Balancing или Service Mesh для вызова Agent Service.
    *   **Inference:** LB перед узлами Ollama (Least Connections) для распределения GPU нагрузки.
4.  **Migration:** Абстракция репозитория (`ChatRepository`) позволяет заменить SQLite на PostgreSQL изменением конфигурации JDBC, не меняя бизнес-логику.
