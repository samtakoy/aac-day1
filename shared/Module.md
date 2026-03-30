# Shared DTOs Module

**Package:** `com.example.day.shared.dto`  
**Module:** `:shared:simple-chat-api`  
**Type:** Shared Library (Kotlin/JVM + Android)

Common DTOs shared between all modules for OpenAI-compatible chat completions API.

## Overview

This module provides Kotlinx Serialization-compatible data classes for OpenAI Chat Completions API format. These DTOs are used across all services (Android app, AI Gateway, RAG Server) to ensure consistent API communication.

## Key Classes

### [`OpenAiDtos.kt`](shared/simple-chat-api/src/main/kotlin/com/example/day/shared/dto/OpenAiDtos.kt)

#### ChatCompletionRequest

OpenAI-compatible chat completion request.

```kotlin
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)
```

**Properties:**
| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `model` | String | Yes | Model identifier (e.g., `gpt-4o-mini`, `qwen2.5-coder-7b`) |
| `messages` | List<Message> | Yes | Conversation messages |
| `stream` | Boolean | No | Enable streaming (default: false) |
| `maxTokens` | Int? | No | Max tokens in response |
| `temperature` | Double? | No | Sampling temperature (0.0-2.0) |

#### Message

Single message in conversation.

```kotlin
@Serializable
data class Message(
    val role: String,
    val content: String
)
```

**Roles:**
- `system` - System instructions
- `user` - User message
- `assistant` - AI response

#### ChatCompletionResponse

OpenAI-compatible chat completion response.

```kotlin
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String = "",
    val choices: List<Choice>,
    val usage: Usage? = null
)
```

#### Choice

Single completion choice.

```kotlin
@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String? = null
)
```

#### Usage

Token usage statistics.

```kotlin
@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)
```

## Usage Examples

### Creating a Request

```kotlin
val request = ChatCompletionRequest(
    model = "gpt-4o-mini",
    messages = listOf(
        Message(role = "system", content = "You are a helpful assistant."),
        Message(role = "user", content = "Hello!")
    ),
    temperature = 0.7,
    maxTokens = 1000
)
```

### Serializing/Deserializing

```kotlin
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

// Serialize to JSON string
val jsonString = json.encodeToString(ChatCompletionRequest.serializer(), request)

// Deserialize from JSON string
val response = json.decodeFromString(ChatCompletionResponse.serializer(), jsonString)
```

## Dependencies

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
    }
}
```

## Module Structure

```
shared/simple-chat-api/
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── com/example/day/shared/
                └── dto/
                    └── OpenAiDtos.kt
```

## Notes

- All DTOs use `@Serializable` annotation for Kotlinx Serialization
- Field naming follows OpenAI API conventions with `snake_case`
- `SerialName` used for JSON-to-Kotlin mapping where names differ
