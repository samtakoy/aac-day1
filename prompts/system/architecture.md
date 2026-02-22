# Architecture Overview

## Project Structure

```
app/src/main/java/com/example/day/
├── app/                    # Application level
│   ├── di/                 # AppComponent (Dagger Hilt)
│   └── MyApp.kt
├── core/                   # Core modules (shared)
│   ├── core_features/
│   │   ├── chat/           # Chat core feature
│   │   │   ├── domain/     # Domain layer
│   │   │   │   ├── model/  # Chat, User, ChatMessage, ChatMessageStatus, UserType, ChatType, ChatGroup, ChatSettings
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── AddChatMessageUseCase
│   │   │   │   │   ├── ChangeMessageStatusUseCase
│   │   │   │   │   ├── ClearChatNotViewedMessageUseCase
│   │   │   │   │   ├── ClearChatUseCase
│   │   │   │   │   ├── CreateChatGroupUseCase
│   │   │   │   │   ├── CreateChatUseCase
│   │   │   │   │   ├── DeleteChatGroupUseCase
│   │   │   │   │   ├── DropChatUseCase
│   │   │   │   │   ├── GetChatGroupsUseCase
│   │   │   │   │   ├── GetChatListAsFlowUseCase
│   │   │   │   │   ├── GetChatMessagesAsFlowUseCase
│   │   │   │   │   ├── GetChatMessagesUseCase
│   │   │   │   │   ├── GetChatsByGroupUseCase
│   │   │   │   │   ├── GetChatTypesUseCase
│   │   │   │   │   ├── RemoveChatMessageUseCase
│   │   │   │   │   └── UpdateChatGroupUseCase
│   │   │   │   └── ChatRepository.kt
│   │   │   └── data/       # Data layer (internal)
│   │   │       ├── local/  # Room database
│   │   │       │   ├── model/     # Entities (UserEntity, ChatEntity, MessageEntity, ChatTypeEntity, ChatGroupEntity)
│   │   │       │   ├── dao/       # DAOs (UserDao, ChatDao, MessageDao, ChatTypeDao, ChatGroupDao)
│   │   │       │   ├── mapper/    # Mappers (UserMapper, ChatMapper, MessageMapper, ChatTypeMapper, ChatGroupMapper)
│   │   │       │   └── ChatDatabase.kt
│   │   │       └── ChatRepositoryImpl.kt
│   │   └── llm/            # LLM integration module
│   │       ├── domain/     # Domain layer
│   │       │   ├── LlmRepository.kt
│   │       │   ├── LlmRequestUseCase.kt
│   │       │   ├── ModelConst.kt
│   │       │   └── model/   # ModelRequest, ModelResult, ModelSettings
│   │       └── data/       # Data layer
│   │           ├── remote/ # Ktor API client
│   │           │   ├── RemoteLlmApi.kt
│   │           │   ├── RemoteLlmApiImpl.kt
│   │           │   └── model/ # Request/Response DTOs
│   │           └── LlmRepositoryImpl.kt
│   ├── di/                 # Core DI (NetworkModule)
│   ├── ui/
│   │   ├── theme/          # Material 3 theme
│   │   └── uikit/          # Reusable UI components
│   │       ├── chat/       # Chat UI components
│   │       │   ├── bar/    # ChatBarView, ChatSendButton
│   │       │   └── list/   # ChatListView, ChatMessageView, AvatarView
│   │       └── dialogs/    # Dialog components
│   │           ├── confirm/ # ConfirmDialog
│   │           └── group/   # GroupEditDialog, GroupEditDialogState
│   └── feature_entries/   # Feature entry points
└── features/               # Feature modules
    ├── chats/             # Chat UI feature
    │   ├── api/           # Feature entry interface
    │   └── impl/          # Feature implementation
    │       ├── di/        # Feature DI
    │       └── ui/        # Screens and ViewModels
    ├── group_choice/      # Group selection feature
    │   ├── api/           # Feature entry interface
    │   │   └── GroupChoiceFeatureEntry.kt
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
            │       ├── ChatCommand.kt
            │       ├── AgMessageHandler.kt
            │       └── worker/ # Workers: Simple, Step, Prompt, Team, AWorker
            ├── di/        # Feature DI
            └── ui/        # ConsoleScreen, ViewModels, Delegates
                ├── components/ # ChatSettingsView, ChatSettingsUiModel
                ├── delegates/  # TalkDelegate, LlmTalkDelegate, AgentsTalkDelegate
                └── viewmodel/  # ConsoleViewModel, ConsoleViewModelImpl
```

## Key Technologies

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Dagger Hilt** for Dependency Injection
- **Room** for local database
- **KSP** for annotation processing
- **Ktor Client** for networking (not Retrofit!)
- **Kotlinx Serialization** for JSON
- **Coroutines + Flow** for async/reactive
- **Clean Architecture** (domain/data separation)

## Core Features

### Chat Core (`com.example.day.core.core_features.chat`)
- Domain models: Chat, User, ChatMessage, ChatMessageStatus, UserType, ChatType, ChatGroup, ChatSettings
- Use Cases:
  - Message: AddChatMessage, ChangeMessageStatus, ClearChatNotViewedMessage, ClearChat, DropChat, GetChatMessages, GetChatMessagesAsFlow, RemoveChatMessage
  - Chat: CreateChat, GetChatListAsFlow, GetChatsByGroup
  - Group: CreateChatGroup, DeleteChatGroup, GetChatGroups, UpdateChatGroup
  - Types: GetChatTypes
- Room: ChatDatabase with UserEntity, ChatEntity, MessageEntity, ChatTypeEntity, ChatGroupEntity
- Constants: ChatDbConst (avoid magic numbers)

### LLM Core (`com.example.day.core.core_features.llm`)
- ModelRequest/ModelResponse with mappers
- Remote API via Ktor Client (OpenRouter)
- Supports multiple models:
  - `meta-llama/llama-3.3-70b-instruct` (default)
  - `qwen/qwen3-next-80b-a3b-instruct`
  - `google/gemma-3-12b-it`
  - `deepseek/deepseek-r1-0528`
  - And many others via OpenRouter

### Console Feature (`com.example.day.features.console`)
- LLM chat interface with settings
- Uses ModelRequest/ModelResponse with mappers
- Remote API via Ktor Client (not Retrofit!)
- AI Agents system with commands:
  - `@@simple` — simple task execution
  - `@@steps` — step-by-step (final answer marked with `<FINAL>`)
  - `@@prompt` — prompt generation before execution
  - `@@team` — team-based agent collaboration
- UI Components: ChatSettingsView for model/parameter configuration
- Delegates: TalkDelegate (base), LlmTalkDelegate, AgentsTalkDelegate

### Chats Feature (`com.example.day.features.chats`)
- UI layer for displaying chats
- ViewModel: ChatsViewModel, ChatsViewModelImpl
- Uses core chat functionality

### Group Choice Feature (`com.example.day.features.group_choice`)
- UI for selecting chat groups
- ViewModel: GroupChoiceViewModel, GroupChoiceViewModelImpl
- Components: GroupCard, GroupsGrid
- Manages ChatGroup entities

## Important Patterns

1. **Internal visibility** for data layer implementations
2. **Constants centralized** in *Const objects (no magic numbers)
3. **Explicit when** for enum mapping (avoid ordinal)
4. **Mutex** for thread-safe operations
5. **Flow** for reactive data streams
6. **Feature Entry** pattern for navigation between features
