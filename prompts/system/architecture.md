# Architecture Overview

## Project Structure

```
app/src/main/java/com/example/day/
├── app/                    # Application level
│   ├── di/                 # AppComponent (Dagger)
│   │   └── AppComponent.kt # Main component implementing feature deps interfaces
│   └── MyApp.kt
├── core/                   # Core modules (shared)
│   ├── core_features/
│   │   ├── chat/           # Chat core feature
│   │   │   ├── domain/     # Domain layer
│   │   │   │   ├── model/  # Chat, User, ChatMessage, ChatMessageStatus, UserType, ChatType, ChatGroup, ChatSettings, ChatGroupColors
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── AddChatMessageUseCase
│   │   │   │   │   ├── ChangeMessageStatusUseCase
│   │   │   │   │   ├── ClearChatNotViewedMessageUseCase
│   │   │   │   │   ├── ClearChatUseCase
│   │   │   │   │   ├── CreateChatGroupUseCase
│   │   │   │   │   ├── CreateChatUseCase
│   │   │   │   │   ├── DeleteChatGroupUseCase
│   │   │   │   │   ├── DropChatUseCase
│   │   │   │   │   ├── GetChatByIdAsFlowUseCase    # NEW: Flow-based chat subscription
│   │   │   │   │   ├── GetChatGroupsUseCase
│   │   │   │   │   ├── GetChatMessagesAsFlowUseCase
│   │   │   │   │   ├── GetChatMessagesUseCase
│   │   │   │   │   ├── GetChatMessagesWithStatusUseCase
│   │   │   │   │   ├── GetChatSettingsUseCase      # NEW: Get settings for chat
│   │   │   │   │   ├── GetChatsByGroupUseCase
│   │   │   │   │   ├── GetChatTypesUseCase
│   │   │   │   │   ├── RemoveChatMessageUseCase
│   │   │   │   │   ├── UpdateChatGroupUseCase
│   │   │   │   │   └── UpdateChatSettingsUseCase   # NEW: Update chat settings
│   │   │   │   └── ChatRepository.kt
│   │   │   └── data/       # Data layer (internal)
│   │   │       ├── local/  # Room database
│   │   │       │   ├── model/     # Entities: UserEntity, ChatEntity, MessageEntity, ChatTypeEntity, ChatGroupEntity
│   │   │       │   │   │          # NEW: ChatSettingsEntity, ModelSettingsEntity (JSON-serialized)
│   │   │       │   │   └── joins/ # ChatWithGroupAndSettings (Room relation)
│   │   │       │   ├── dao/       # DAOs: UserDao, ChatDao, MessageDao, ChatTypeDao, ChatGroupDao
│   │   │       │   │   │          # NEW: ChatSettingsDao
│   │   │       │   ├── mapper/    # Mappers: UserMapper, ChatMapper, MessageMapper, ChatTypeMapper, ChatGroupMapper
│   │   │       │   │   │          # NEW: ChatSettingsMapper, ModelSettingsMapper (JSON serialization)
│   │   │       │   └── ChatDatabase.kt  # Version 3, includes ChatSettingsEntity
│   │   │       └── ChatRepositoryImpl.kt
│   │   └── llm/            # LLM integration module
│   │       ├── domain/     # Domain layer
│   │       │   ├── LlmRepository.kt
│   │       │   ├── LlmRequestUseCase.kt
│   │       │   ├── LlmRequestUseCaseImpl.kt
│   │       │   ├── ModelConst.kt
│   │       │   └── model/   # ModelRequest, ModelResult, ModelSettings
│   │       └── data/       # Data layer
│   │           ├── remote/ # Ktor API client
│   │           │   ├── RemoteLlmApi.kt
│   │           │   ├── RemoteLlmApiImpl.kt
│   │           │   ├── mappers/ # ModelRequestMapperImpl, ModelResponseMapperImpl
│   │           │   └── model/ # Request/Response DTOs (ChatRequestDto, MessageDto, ChatResponseDto)
│   │           └── LlmRepositoryImpl.kt
│   ├── di/                 # Core DI (NetworkModule)
│   ├── ui/
│   │   ├── theme/          # Material 3 theme (Color, Theme, Type)
│   │   └── uikit/          # Reusable UI components
│   │       ├── chat/       # Chat UI components
│   │       │   ├── bar/    # ChatBarView, ChatSendButton, ChatBarUiModel, ChatBarUiEvent
│   │       │   └── list/   # ChatListView, ChatMessageView, AvatarView, ChatListUiModel
│   │       └── dialogs/    # Dialog components
│   │           ├── confirm/ # ConfirmDialog
│   │           └── group/   # GroupEditDialog, GroupEditDialogState
│   └── feature_entries/   # FeatureEntryProvider
└── features/               # Feature modules
    ├── chats/             # Chat list UI feature
    │   ├── api/           # ChatsFeatureEntry
    │   └── impl/          # Feature implementation
    │       ├── di/        # ChatsFeatureComponent, ChatsFeatureDeps, ChatsFeatureModule, ChatsFeatureScope
    │       └── ui/        # ChatsScreen, ChatsViewModel, ChatsViewModelImpl
    ├── group_choice/      # Group selection feature
    │   ├── api/           # GroupChoiceFeatureEntry
    │   └── impl/          # Feature implementation
    │       ├── di/        # Feature DI
    │       └── ui/        # Screens, ViewModels, Components
    │           ├── components/ # GroupCard, GroupsGrid
    │           └── viewmodel/   # GroupChoiceViewModel, GroupChoiceViewModelImpl
    └── console/           # AI Console feature (main)
        ├── api/           # Feature entry points
        │   ├── ConsoleFeatureEntry.kt
        │   └── AgentsConsoleFeatureEntry.kt
        └── impl/          # Feature implementation
            ├── domain/
            │   └── agents/ # AI agents system
            │       ├── ChatCommand.kt      # Command parsing (@@simple, @@steps, etc.)
            │       ├── AgMessageHandler.kt # Message handling
            │       ├── model/              # TeamAgentConfig
            │       └── worker/             # Workers: SimpleWorker, StepWorker, PromptWorker, TeamWorker, AWorker
            ├── di/        # Feature DI
            │   ├── ConsoleFeatureComponent.kt  # Dagger component
            │   ├── ConsoleFeatureDeps.kt       # Dependencies interface (implemented by AppComponent)
            │   ├── ConsoleFeatureModule.kt
            │   ├── ConsoleFeatureScope.kt
            │   └── ConsoleFeatureApiModule.kt
            └── ui/        # ConsoleScreen, ViewModels, Delegates
                ├── components/ # ChatSettingsView, ChatSettingsUiModel
                ├── delegates/  # TalkDelegate, LlmTalkDelegate, AgentsTalkDelegate
                └── viewmodel/  # ConsoleViewModel, ConsoleViewModelImpl
```

## Key Technologies

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Dagger** (not Hilt!) for Dependency Injection with feature-scoped components
- **Room** for local database with relations and joins
- **KSP** for annotation processing
- **Ktor Client** for networking (not Retrofit!)
- **Kotlinx Serialization** for JSON (ModelSettings stored as JSON in DB)
- **Coroutines + Flow** for async/reactive
- **Clean Architecture** (domain/data/ui separation)
- **kotlinx.collections.immutable** for immutable collections

## Core Features

### Chat Core (`com.example.day.core.core_features.chat`)

#### Domain Models:
- `Chat` - includes `settings: ChatSettings` field (loaded via Room relation)
- `User` with `UserType` (User/Agent)
- `ChatMessage` with `ChatMessageStatus` (Sending/Delivered/Viewed)
- `ChatType` - chat type enumeration
- `ChatGroup` with `ChatGroupColors`
- `ChatSettings` - per-chat settings (systemPrompt, model settings)
- `ModelSettings` - LLM model configuration (name, maxTokens, stopSequence, jsonFormat, reasoningEffort)

#### Use Cases:
- **Message**: AddChatMessage, ChangeMessageStatus, ClearChatNotViewedMessage, ClearChat, DropChat, GetChatMessages, GetChatMessagesAsFlow, GetChatMessagesWithStatus, RemoveChatMessage
- **Chat**: CreateChat, GetChatByIdAsFlow, GetChatsByGroup
- **Settings**: GetChatSettings, UpdateChatSettings (NEW)
- **Group**: CreateChatGroup, DeleteChatGroup, GetChatGroups, UpdateChatGroup
- **Types**: GetChatTypes

#### Data Layer:
- **Room Database**: ChatDatabase (version 3)
- **Entities**: UserEntity, ChatEntity, MessageEntity, ChatTypeEntity, ChatGroupEntity, ChatSettingsEntity, ModelSettingsEntity
- **Relations**: ChatWithGroupAndSettings (loads Chat with Group and Settings in single query)
- **DAOs**: UserDao, ChatDao, MessageDao, ChatTypeDao, ChatGroupDao, ChatSettingsDao
- **Mappers**: Entity ↔ Domain mapping with JSON serialization for ModelSettings

### LLM Core (`com.example.day.core.core_features.llm`)

- `ModelRequest`/`ModelResult` with mappers
- `ModelSettings` - model configuration (shared with ChatSettings)
- Remote API via Ktor Client (OpenRouter)
- Default model: `meta-llama/llama-3.3-70b-instruct`
- Supports: qwen, gemma, deepseek, and others via OpenRouter

### Console Feature (`com.example.day.features.console`)

#### AI Agents System:
Commands for different execution modes:
- `@@simple` — simple task execution
- `@@steps` — step-by-step (final answer marked with `<FINAL>`)
- `@@prompt` — prompt generation before execution
- `@@team` — team-based agent collaboration
- `@@compare` — compare multiple approaches

#### Workers:
- `AWorker` - abstract base
- `SimpleWorker` - direct execution
- `StepWorker` - step-by-step reasoning
- `PromptWorker` - generates prompt first
- `TeamWorker` - multi-agent collaboration

#### UI Components:
- `ChatSettingsView` - model/parameter configuration (loaded from DB, saved to DB)
- `ChatBarView` - message input with send button
- `ChatListView` - message display

#### Delegates:
- `TalkDelegate` - base interface
- `LlmTalkDelegate` - direct LLM communication
- `AgentsTalkDelegate` - AI agents system

### Chats Feature (`com.example.day.features.chats`)
- UI layer for displaying chats list
- Uses core chat functionality

### Group Choice Feature (`com.example.day.features.group_choice`)
- UI for selecting chat groups
- Components: GroupCard, GroupsGrid
- Manages ChatGroup entities

## Dependency Injection Architecture

```
AppComponent (Singleton)
├── implements: ConsoleFeatureDeps, ChatsFeatureDeps, GroupChoiceFeatureDeps
├── includes: NetworkModule, ChatCoreFeatureModule, LlmCoreFeatureModule
└── provides: ChatRepository, LlmRepository, all UseCases

ConsoleFeatureComponent (ConsoleFeatureScope)
├── dependencies: ConsoleFeatureDeps
├── modules: ConsoleFeatureModule
└── provides: ConsoleViewModel.Factory, TalkDelegates
```

### Feature Dependencies Pattern:
Each feature defines a `FeatureDeps` interface that `AppComponent` implements. This allows feature components to access singleton dependencies without direct coupling.

## Important Patterns

1. **Internal visibility** for data layer implementations
2. **Constants centralized** in `*Const` objects (no magic numbers)
3. **Explicit when** for enum mapping (avoid ordinal)
4. **Mutex** for thread-safe operations (used in ChatRepositoryImpl)
5. **Flow** for reactive data streams
6. **Feature Entry** pattern for navigation between features
7. **Room Relations** for loading related entities in single query
8. **JSON serialization** for complex objects in database (ModelSettings)
9. **kotlinx.collections.immutable** for immutable collections in models

## Database Schema

### Tables:
- `users` - User entities
- `chats` - Chat entities (FK: chatGroupId, chatTypeId)
- `messages` - Message entities (FK: chatId, userId)
- `chat_groups` - Chat group entities
- `chat_types` - Chat type entities
- `chat_settings` - Per-chat settings (FK: chatId, contains modelSettings as JSON)

### Relations:
- `ChatWithGroupAndSettings` - loads Chat with ChatGroup and ChatSettings

## Navigation Flow

1. `GroupChoiceFeature` → select `ChatGroup`
2. `ChatsFeature` → display chats in group, select or create chat
3. `ConsoleFeature` → chat with LLM/agents
