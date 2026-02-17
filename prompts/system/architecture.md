# Architecture Overview

## Project Structure

```
app/src/main/java/com/example/day/
├── app/                    # Application level
│   ├── di/                 # AppComponent (Dagger)
│   └── MyApp.kt
├── core/                   # Core modules (shared)
│   ├── core_features/
│   │   └── chat/           # Chat core feature
│   │       ├── domain/     # Domain layer
│   │       │   ├── model/  # Chat, User, ChatMessage, etc.
│   │       │   ├── usecase/
│   │       │   └── ChatRepository.kt
│   │       └── data/       # Data layer (internal)
│   │           ├── local/  # Room database
│   │           │   ├── model/     # Entities
│   │           │   ├── dao/       # DAOs
│   │           │   ├── mapper/    # Mappers
│   │           │   └── ChatDatabase.kt
│   │           └── ChatRepositoryImpl.kt
│   └── di/                 # Core DI (NetworkModule)
└── features/               # Feature modules
    └── console/            # Console feature
        ├── api/            # Feature entry point
        └── impl/           # Feature implementation
            ├── domain/
            ├── data/
            ├── di/         # Feature DI (Dagger)
            └── ui/
```

## Key Technologies

- **Kotlin** + **Jetpack Compose**
- **Dagger/Hilt** for DI
- **Room** for local database
- **KSP** for annotation processing
- **Coroutines + Flow** for async/reactive
- **Clean Architecture** (domain/data separation)

## Core Features

### Chat Core (`com.example.day.core.core_features.chat`)
- Domain models: Chat, User, ChatMessage, ChatMessageStatus, UserType
- Use Cases: CreateChat, GetChatList, AddMessage, RemoveMessage, ChangeStatus, GetMessages, ClearChat, ClearNotViewed, DropChat
- Room: ChatDatabase with UserEntity, ChatEntity, MessageEntity
- Constants: ChatDbConst (avoid magic numbers)

### Console Feature (`com.example.day.features.console`)
- LLM chat interface
- Uses ModelRequest/ModelResponse with mappers
- Remote API via Retrofit

## Important Patterns

1. **Internal visibility** for data layer implementations
2. **Constants centralized** in *Const objects (no magic numbers)
3. **Explicit when** for enum mapping (avoid ordinal)
4. **Mutex** for thread-safe operations
5. **Flow** for reactive data streams
