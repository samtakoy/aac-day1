# AI Gateway Module

**Package:** `com.example.day.aigateway`  
**Module:** `:ai-gateway`  
**Type:** Backend Service (Kotlin/JVM)

AI Gateway is a proxy server between Android application and local Ollama, providing OpenAI-compatible API.

## Overview

The AI Gateway:
- Accepts requests in OpenAI API format (`/v1/chat/completions`)
- Routes requests to local Ollama server
- Handles concurrency limiting and rate limiting
- Caches model information from Ollama

## Purpose

The AI Gateway solves the **network limitation** where Android emulators can't directly reach localhost services on the host machine. It acts as a **proxy** that:

1. **Exposes OpenAI-compatible API** - Android app uses standard OpenAI client code
2. **Forwards to Ollama** - Converts requests to Ollama format
3. **Protects Ollama** - Rate limiting and concurrency controls prevent overload
4. **Manages Context** - Caches model context limits to prevent crashes

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Android App (Day)                                               │
│   - Uses OpenAI-compatible client                               │
│   - Sends to http://10.0.2.2:8081 (emulator localhost)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  AI Gateway (:8081)                                              │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │ ChatRoutes.kt                                            │  │
│   │  - POST /v1/chat/completions                             │  │
│   │  - GET /v1/models                                       │  │
│   └─────────────────────────────────────────────────────────┘  │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │ Middleware                                               │  │
│   │  - ConcurrencyLimiter (max 4 parallel)                 │  │
│   │  - RateLimiter (60 RPM per IP)                         │  │
│   └─────────────────────────────────────────────────────────┘  │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │ LlmRouter                                                │  │
│   │  (MVP: routes all to Ollama)                            │  │
│   └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Ollama (localhost:11434)                                        │
│   - Local LLM inference                                         │
│   - Serves models like qwen2.5:7b                               │
└─────────────────────────────────────────────────────────────────┘
```

## Request Flow

### Chat Completion

```bash
# 1. Android sends OpenAI format
POST /v1/chat/completions
{
  "model": "qwen2.5:7b",
  "messages": [{"role": "user", "content": "Hello!"}]
}

# 2. AI Gateway:
#    a. Checks concurrency limit
#    b. Checks rate limit (RPM)
#    c. Converts to Ollama format
#    d. Adds num_ctx (context size)
#    e. Forwards to Ollama

# 3. Ollama responds with
{
  "model": "qwen2.5:7b", 
  "message": {"role": "assistant", "content": "Hello!"}
}

# 4. AI Gateway:
#    a. Converts response to OpenAI format
#    b. Returns to Android
```

## Concurrency Management

`ConcurrencyLimiter` prevents Ollama from being overwhelmed:

```kotlin
class ConcurrencyLimiter(
    private val maxConcurrent: Int = 4
) {
    // Uses Semaphore to limit parallel Ollama requests
    // Returns 429 if limit exceeded
}
```

## Rate Limiting

Per-IP rate limiting protects against abuse:

- **RPM**: 60 requests per minute per IP
- **Headers**: Returns `X-RateLimit-Remaining` and `Retry-After`

## Model Context Size

AI Gateway prevents Ollama crashes by limiting context:

```kotlin
// Environment: OLLAMA_MAX_CONTEXT (default: 8192)
val numCtx = min(OLLAMA_MAX_CONTEXT, modelLimitFromOllama)
```

This ensures requests don't exceed model context window.

## Deployment Options

### Docker Compose

```yaml
services:
  ai-gateway:
    build: ./ai-gateway
    ports:
      - "8081:8081"
    environment:
      - OLLAMA_URL=http://host.docker.internal:11434
      - CONCURRENCY_LIMIT=4
      - RATE_LIMIT_RPM=60
```

### Standalone JAR

```bash
java -jar ai-gateway.jar \
  --ollama-url=http://localhost:11434 \
  --port=8081 \
  --concurrency-limit=4
```

## Key Components

### Main Entry Point

- [`AiGatewayServer.kt`](src/main/kotlin/com/example/day/aigateway/AiGatewayServer.kt) - Main server entry point

### API (`api/routes/`)

- [`ChatRoutes.kt`](src/main/kotlin/com/example/day/aigateway/api/routes/ChatRoutes.kt) - HTTP route handlers

### Configuration (`config/`)

- [`AiGatewayConfig.kt`](src/main/kotlin/com/example/day/aigateway/config/AiGatewayConfig.kt) - Configuration data class

### LLM Integration (`llm/`)

- [`LlmRouter.kt`](src/main/kotlin/com/example/day/aigateway/llm/LlmRouter.kt) - Routes LLM requests (MVP: Ollama only)
- [`LlmProvider.kt`](src/main/kotlin/com/example/day/aigateway/llm/LlmProvider.kt) - LLM provider interface
- [`OllamaProvider.kt`](src/main/kotlin/com/example/day/aigateway/llm/OllamaProvider.kt) - Ollama API implementation
- [`OllamaModelInfoCache.kt`](src/main/kotlin/com/example/day/aigateway/llm/OllamaModelInfoCache.kt) - Model info cache

### Middleware

- [`ConcurrencyLimiter.kt`](src/main/kotlin/com/example/day/aigateway/middleware/ConcurrencyLimiter.kt) - Concurrency control

## HTTP API

### POST /v1/chat/completions

OpenAI-compatible chat completions endpoint.

```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:7b",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### GET /v1/models

Returns available models from Ollama.

```bash
curl http://localhost:8081/v1/models
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server URL |
| `PORT` | `8081` | Server port |
| `RATE_LIMIT_RPM` | `60` | Max requests per minute per IP |
| `CONCURRENCY_LIMIT` | `4` | Max concurrent requests to Ollama |
| `OLLAMA_MAX_CONTEXT` | `8192` | Max context tokens (admin limit) |

### Rate Limiting

Two types of limits protect the service:

1. **RPM per-IP** - Returns `429` with `X-RateLimit-Remaining` and `Retry-After` headers
2. **Concurrency** - Returns `429` immediately if limit exceeded

### Context Size Management

`num_ctx` is automatically computed:

```
num_ctx = min(OLLAMA_MAX_CONTEXT, model_limit_from_ollama)
```

This prevents Ollama from crashing when requests exceed model limits.

## Usage Example

### Starting the Server

```bash
# Build
./gradlew :ai-gateway:build

# Run
java -jar ai-gateway/build/libs/ai-gateway.jar

# Or with custom Ollama URL
OLLAMA_URL=http://192.168.1.100:11434 java -jar ai-gateway/build/libs/ai-gateway.jar
```

### Via Docker Compose

```bash
docker-compose up ai-gateway
```

### From Android Application

```kotlin
// Configure in DataStore
val settings = dataStore.data.first()
val baseUrl = settings.localServerUrl // e.g., http://10.0.2.2:8081

// Use with LLM request
val response = httpClient.post("$baseUrl/v1/chat/completions") {
    contentType(ContentType.Application.Json)
    setBody(ChatCompletionRequest(
        model = "qwen2.5:7b",
        messages = listOf(Message("user", "Hello!"))
    ))
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AiGatewayServer.kt                        │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐ │
│  │ Ktor Netty  │  │ RateLimit  │  │ ConcurrencyLimiter  │ │
│  │ :8081       │  │ RPM/IP     │  │ MAX_PARALLEL        │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬───────────┘ │
└─────────┼────────────────┼────────────────────┼─────────────┘
          │                │                    │
          ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                      ChatRoutes.kt                           │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ POST /v1/chat/completions  │  GET /v1/models        │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                       LlmRouter.kt                          │
│              (MVP: routes all to Ollama)                    │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    OllamaProvider.kt                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │ /api/chat   │  │ /api/tags   │  │ /api/show       │   │
│  │ (completion)│  │ (models)    │  │ (model info)    │   │
│  └─────────────┘  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│               OllamaModelInfoCache.kt                       │
│        (caches model context limits)                        │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
ai-gateway/
├── build.gradle.kts
├── Dockerfile
├── SETUP.md
└── src/main/kotlin/com/example/day/aigateway/
    ├── AiGatewayServer.kt          # Main entry
    ├── api/routes/
    │   └── ChatRoutes.kt          # HTTP routes
    ├── config/
    │   └── AiGatewayConfig.kt     # Configuration
    ├── llm/
    │   ├── LlmProvider.kt         # Interface
    │   ├── LlmRouter.kt           # Router (MVP)
    │   ├── OllamaProvider.kt      # Ollama impl
    │   ├── OllamaModelInfoCache.kt # Cache
    │   └── OllamaCompletionRequest.kt
    └── middleware/
        └── ConcurrencyLimiter.kt   # Concurrency control
```
