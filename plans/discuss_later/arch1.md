Я структурировал твой диалог в **архитектурный документ уровня senior/system design**, без потери информации, но с нормальной иерархией, связями и акцентами.

---

# 📘 Архитектура LLM Chat Platform (Kotlin + Koog + Ollama)

## 1. Цели системы

### Основная цель

Перенос логики работы с LLM и агентами с Android-клиента на сервер с возможностью:

* работы как OpenRouter-compatible API
* поддержки чат-коммуникации (статусы, стриминг)
* масштабирования до микросервисной архитектуры

### Требования

* Совместимость с `/v1/chat/completions`
* Поддержка стриминга (SSE / WebSocket)
* Поддержка агентов (Koog)
* Работа с локальной Ollama (MVP)
* Готовность к high-load

---

## 2. High-Level архитектура

```
Android Client
      │
      ▼
AI Gateway (Ktor)
      │
      ▼
Agent Service (Koog)
      │
      ▼
Inference Node (Ollama)
```

---

## 3. Архитектурные принципы

### 3.1 Разделение ответственности

| Сервис        | Ответственность            |
| ------------- | -------------------------- |
| Gateway       | Транспорт, API, соединения |
| Agent Service | Бизнес-логика, агенты      |
| Inference     | Генерация текста           |

---

### 3.2 Реактивная модель

Вся система построена на:

```
Flow<ChatEvent>
```

Это ключевое решение, позволяющее:

* поддерживать SSE
* поддерживать WebSocket
* поддерживать gRPC streaming
* не переписывать бизнес-логику

---

## 4. Контракты и модели

### 4.1 Внутренняя модель событий

```kotlin
sealed class ChatEvent {
    data class Status(val state: String)
    data class Token(val content: String)
    data class Metadata(val usage: TokenUsage)
    data object Done
}
```

---

### 4.2 OpenAI/OpenRouter API

```kotlin
POST /v1/chat/completions
```

Поддержка:

* `stream = true` → SSE
* `stream = false` → обычный JSON

---

## 5. AI Gateway (Ktor)

### 5.1 Роль

* входная точка системы
* совместимость с OpenRouter
* управление соединениями

---

### 5.2 Технологии

* Ktor
* kotlinx.serialization
* gRPC client
* SSE / WebSocket

---

### 5.3 Эндпоинты

#### HTTP (OpenAI-compatible)

```
POST /v1/chat/completions
```

#### SSE (MVP)

```
text/event-stream
```

#### WebSocket (future)

```
/chat/ws
```

---

### 5.4 Логика работы

```
Request → Gateway
        → gRPC → Agent Service
        → Stream (Flow)
        → SSE / WS клиенту
```

---

### 5.5 Ключевая идея

Gateway НЕ знает:

* про Koog
* про Ollama

👉 только про gRPC контракт

---

## 6. Agent Service (Koog)

### 6.1 Роль

* оркестрация агентов
* работа с контекстом
* интеграция с моделями

---

### 6.2 Внутренняя структура

```
gRPC Layer
    ↓
Agent Orchestrator
    ↓
Koog Agents
    ↓
Model (Ollama)
```

---

### 6.3 Конфигурация модели

```kotlin
OpenAIChatModel(
    modelName = "llama3",
    configuration = OpenAIModelConfiguration(
        host = "ollama-service",
        port = 11434,
        pathPrefix = "/v1"
    )
)
```

---

### 6.4 Поток обработки

```
gRPC request
 → Status(thinking)
 → Koog execution
 → Token stream
 → Done
```

---

### 6.5 Memory (важно)

Agent Service должен быть **stateless**

Контекст:

* хранится в БД
* подгружается перед вызовом

---

### 6.6 Tools / Function Calling

Агент может:

* ходить в API
* работать с БД
* вызывать функции

Важно:

```
Status: "calling_tool"
```

---

## 7. Inference Node (Ollama)

### 7.1 Роль

* чистый inference
* без логики
* без состояния

---

### 7.2 API

```
POST /v1/chat/completions
```

---

### 7.3 Production настройки

```
OLLAMA_KEEP_ALIVE=-1
OLLAMA_NUM_PARALLEL=4
OLLAMA_MAX_LOADED_MODELS=1
```

---

### 7.4 Масштабирование

#### Вертикально

* больше GPU

#### Горизонтально

```
Load Balancer → Ollama nodes
```

---

## 8. Коммуникации

### 8.1 Клиент → Gateway

* HTTP
* SSE
* WebSocket

---

### 8.2 Gateway → Agent Service

* gRPC (streaming)

---

### 8.3 Agent → Ollama

* HTTP (OpenAI-compatible)

---

## 9. Поддержка SSE и WebSocket

### Принцип

```
Flow<ChatEvent> → адаптер → транспорт
```

---

### SSE

```kotlin
respondSse {
    flow.collect { send(...) }
}
```

---

### WebSocket

```kotlin
webSocket {
    flow.collect { send(...) }
}
```

---

### Вывод

👉 один core → два транспорта
👉 без дублирования логики

---

## 10. Микросервисная структура проекта

```
root
├── gateway-service
├── agent-service
├── shared-api (KMP)
├── shared-grpc
```

---

### 10.1 shared-api (KMP)

Используется:

* Android
* Gateway

Содержит:

* DTO
* ChatEvent
* API модели

---

### 10.2 shared-grpc

Используется:

* Gateway
* Agent Service

Содержит:

* proto файлы
* gRPC модели

---

## 11. Observability (минимум)

### 2 инструмента

#### 1. Micrometer

* метрики

#### 2. OpenTelemetry

* трассировка

---

### Что получаем

| Что        | Инструмент |
| ---------- | ---------- |
| latency    | OTel       |
| bottleneck | OTel       |
| нагрузка   | Micrometer |

---

## 12. Load Balancer

### Это НЕ микросервис

Это:

* Nginx / HAProxy / Cloud LB

---

### Где используется

1. Перед Gateway
2. Перед Ollama

---

### Важная фича

**Sticky sessions** для WebSocket

---

## 13. Поток запроса (end-to-end)

```
Android
 → Gateway
 → gRPC
 → Agent Service
 → Ollama
 → Tokens
 → обратно через всю цепочку
```

---

## 14. MVP → Production эволюция

### MVP

* Gateway + Agent Service
* SSE
* Ollama

---

### Stage 2

* Gateway + Agent Service
* gRPC

---

### Stage 3

* scaling Ollama
* WebSockets

---

## 15. Ключевые архитектурные решения

### 1. Flow как единый контракт

Самое важное решение во всей системе

---

### 2. Koog как единый слой LLM

Без смешивания SDK

---

### 3. Gateway без бизнес-логики

Чистый транспорт

---

### 4. Stateless Agent Service

Готовность к масштабированию

---

## 16. Главные риски

### 1. Ollama bottleneck

GPU ограничение

### 2. Контекст чата

Рост payload

### 3. WebSocket scaling

Нужен sticky LB

---

## 17. Итог

Это архитектура:

* совместима с OpenRouter
* готова к WebSocket
* масштабируется
* не требует переписывания при росте

---

Если хочешь, дальше можем:

* сделать **диаграмму (C4 / sequence)**
* расписать **конкретные Gradle модули**
* или собрать **MVP skeleton проекта** (очень практично)

