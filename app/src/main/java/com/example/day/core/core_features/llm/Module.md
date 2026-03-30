# LLM Core Feature Module

**Package:** `com.example.day.core.core_features.llm`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

LLM (Large Language Model) integration for making AI chat requests.

## Overview

The LLM feature provides:
- LLM API communication (local Ollama, remote OpenAI-compatible)
- Model settings management
- Request/response mapping
- Token usage tracking

## Purpose

The LLM feature is the **HTTP client** that sends chat requests to LLM providers. It abstracts over different LLM backends (Ollama, OpenAI-compatible) and provides:
- Request formatting
- Response parsing
- Token usage tracking
- Model configuration

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `AgentCoreFeature` | Sends prompts via `LlmRequestUseCase` |
| `ConsoleFeature` | Direct LLM requests via `LlmTalkDelegate` |
| `MemoryCoreFeature` | Avatar generation via `GenerateProfileAvatarUseCase` |

## Request/Response Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ AgentCoreFeature / ConsoleFeature                               │
│  └── LlmRequestUseCase(request: ModelRequest)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ LlmRepositoryImpl                                                │
│  ├── LocalLlmApi (Ollama)  │  RemoteLlmApi (OpenAI-compatible) │
│  └── Routes based on model name                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ HTTP Client (Ktor with OkHttp engine)                           │
│  └── POST /api/chat (Ollama) or /v1/chat/completions (OpenAI)  │
└─────────────────────────────────────────────────────────────────┘
```

## Model Settings

Per-chat model configuration stored in `ModelSettings`:

| Setting | Description | Default |
|---------|-------------|---------|
| `model` | Model identifier | `openai/gpt-4o-mini` |
| `temperature` | Creativity (0-2) | `0.7` |
| `maxTokens` | Max response tokens | `4096` |
| `numCtx` | Context window size | `4096` |

## LLM Providers

### Local Ollama

Direct connection to local Ollama server:
- Endpoint: `http://localhost:11434`
- API: `/api/chat`
- No auth required

### Remote OpenAI-Compatible

Any OpenAI-compatible API:
- Endpoint: Configured URL
- API: `/v1/chat/completions`
- Optional Bearer token

## Token Tracking

`ModelConsumption` utility calculates token usage:

```kotlin
// Estimates tokens based on character count
fun estimateTokens(text: String): Int

// Calculates total consumption
data class ModelConsumption(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
```

## Key Components

### Domain Layer

#### Models

- [`ModelRequest.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelRequest.kt) - LLM request model
- [`ModelResult.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelResult.kt) - LLM response model
- [`ModelSettings.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt) - Per-chat model settings

#### Repository

- [`LlmRepository.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRepository.kt) - Interface

#### Use Cases

- [`LlmRequestUseCase.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCase.kt) - Interface
- [`LlmRequestUseCaseImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCaseImpl.kt) - Implementation

#### Utilities

- [`ModelConsumption.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/utils/ModelConsumption.kt) - Token calculation
- [`ModelReportBuilder.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/utils/ModelReportBuilder.kt) - Usage reports
- [`ModelConst.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/ModelConst.kt) - Constants

### Data Layer

#### Remote APIs

- [`LocalLlmApi.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApi.kt) - Ollama interface
- [`LocalLlmApiImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApiImpl.kt)
- [`RemoteLlmApi.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/RemoteLlmApi.kt) - OpenAI-compatible interface
- [`RemoteLlmApiImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/RemoteLlmApiImpl.kt)

#### Repository Implementation

- [`LlmRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/LlmRepositoryImpl.kt)

#### Mappers

- [`ModelRequestMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/ModelRequestMapperImpl.kt)
- [`ModelResponseMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/ModelResponseMapperImpl.kt)
- [`OpenAiModelRequestMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt)
- [`OpenAiModelResponseMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt)

#### DTOs

- [`ChatRequestDto.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/model/request/ChatRequestDto.kt)
- [`MessageDto.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/model/request/MessageDto.kt)
- [`ChatResponseDto.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/model/response/ChatResponseDto.kt)

#### Local

- [`ModelSettingsMapper.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt)
- [`ModelSettingsEntity.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt)

### DI

- [`LlmCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/llm/di/LlmCoreFeatureModule.kt)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ModelRequest │  │LlmRepository│  │ LlmRequestUseCase   │ │
│  │ModelResult  │  │             │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ LocalLlmApi │  │RemoteLlmApi │  │   LlmRepository    │ │
│  │  (Ollama)  │  │  (OpenAI)  │  │     Impl           │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
core/core_features/llm/
├── data/
│   ├── local/
│   │   ├── mapper/
│   │   │   └── ModelSettingsMapper.kt
│   │   └── model/
│   │       └── ModelSettingsEntity.kt
│   ├── remote/
│   │   ├── LocalLlmApi.kt
│   │   ├── LocalLlmApiImpl.kt
│   │   ├── RemoteLlmApi.kt
│   │   ├── RemoteLlmApiImpl.kt
│   │   ├── mappers/
│   │   │   ├── ModelRequestMapperImpl.kt
│   │   │   ├── ModelResponseMapperImpl.kt
│   │   │   ├── OpenAiModelRequestMapperImpl.kt
│   │   │   └── OpenAiModelResponseMapperImpl.kt
│   │   └── model/
│   │       ├── request/
│   │       │   ├── ChatRequestDto.kt
│   │       │   └── MessageDto.kt
│   │       └── response/
│   │           └── ChatResponseDto.kt
│   └── LlmRepositoryImpl.kt
├── di/
│   └── LlmCoreFeatureModule.kt
└── domain/
    ├── LlmRepository.kt
    ├── LlmRequestUseCase.kt
    ├── LlmRequestUseCaseImpl.kt
    ├── ModelConst.kt
    ├── model/
    │   ├── ModelRequest.kt
    │   ├── ModelResult.kt
    │   ├── ModelResultExt.kt
    │   └── ModelSettings.kt
    └── utils/
        ├── ModelConsumption.kt
        └── ModelReportBuilder.kt
```
