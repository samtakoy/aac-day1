# Chat Core Feature Module

**Package:** `com.example.day.core.core_features.chat`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

Chat and message management system with support for chat groups, types, and settings.

## Overview

The Chat feature provides:
- Chat message storage and retrieval
- Chat group organization
- Chat types (LLM, RAG, Planner, Agents)
- Per-chat settings
- Message status tracking
- User management

## Purpose

The Chat feature is the **data layer** for conversations. It manages:
- **Chats** - Individual conversation threads
- **Messages** - User and assistant messages within chats
- **Groups** - Organization of chats (e.g., "Project X", "Personal")
- **Settings** - Per-chat model configuration

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `ConsoleFeature` | Stores messages via `ChatRepository` |
| `ChatsFeature` | Lists chats via `GetChatsByGroupUseCase` |
| `AgentCoreFeature` | Creates agent chats via `CreateChatUseCase` |
| `MemoryCoreFeature` | Links chats to LTM groups |

## Data Model

```
ChatGroup (e.g., "Project X")
  │
  ├── Chat (LLM type) - "Code review chat"
  │     ├── Message (user) - "Review my PR"
  │     └── Message (assistant) - "Here's my review..."
  │
  ├── Chat (RAG type) - "File Q&A"
  │     ├── Message (user) - "Explain main.py"
  │     └── Message (assistant) - "main.py is the entry point..."
  │
  └── Chat (Planner type) - "Build feature"
        └── Messages with task stages...
```

## Chat Types

| Type | Console Entry | Purpose |
|------|---------------|---------|
| `LLM` | `ConsoleFeatureEntry` | Direct conversation |
| `RAG` | `RagConsoleFeatureEntry` | File context Q&A |
| `PLANNER` | `PlannerConsoleFeatureEntry` | Multi-stage planning |
| `AGENTS` | `AgentsConsoleFeatureEntry` | Multi-agent work |

## Message Status

Messages track delivery state:

| Status | Meaning |
|--------|---------|
| `SENDING` | Request in progress |
| `SENT` | Delivered to UI |
| `DELIVERED` | User seen |
| `ERROR` | Failed to send |

## Database Schema

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ chat_groups  │────▶│    chats    │◀────│   messages   │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ name         │     │ group_id (FK)│     │ chat_id (FK) │
│ type_id (FK) │     │ title        │     │ role         │
│ color        │     │ created_at   │     │ content      │
└──────────────┘     │ updated_at   │     │ buttons      │
      │              └──────────────┘     │ status       │
      │                     │             │ timestamp    │
      ▼                     ▼             └──────────────┘
┌──────────────┐     ┌──────────────┐
│  chat_types  │     │ chat_settings│
├──────────────┤     ├──────────────┤
│ id (PK)      │     │ chat_id (FK) │
│ name         │     │ model        │
└──────────────┘     │ temperature  │
                     │ max_tokens   │
                     └──────────────┘
```

## Key Components

### Domain Layer

#### Models

- [`Chat.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/Chat.kt) - Chat entity
- [`ChatGroup.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatGroup.kt) - Group entity
- [`ChatMessage.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatMessage.kt) - Message entity
- [`ChatSettings.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatSettings.kt) - Per-chat settings
- [`ChatType.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatType.kt) - Chat type enum
- [`ChatMessageStatus.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatMessageStatus.kt)
- [`ChatGroupColors.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatGroupColors.kt)
- [`User.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/User.kt)
- [`UserType.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/model/UserType.kt)

#### Repository

- [`ChatRepository.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/ChatRepository.kt) - Main repository interface

#### Tools

- [`ChatTools.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/tools/ChatTools.kt) - Interface
- [`ChatToolsImpl.kt`](app/src/main/java/com/example/day/core/core_features/chat/domain/tools/ChatToolsImpl.kt) - Implementation

#### Use Cases

**Message operations:**
- `AddChatMessageUseCase`, `GetChatMessagesUseCase`, `GetChatMessagesAsFlowUseCase`
- `ChangeMessageStatusUseCase`, `ClearChatNotViewedMessageUseCase`, `RemoveChatMessageUseCase`

**Chat operations:**
- `CreateChatUseCase`, `GetOrCreateChatUseCase`, `DropChatUseCase`, `ClearChatUseCase`
- `GetChatByIdAsFlowUseCase`, `GetChatsByGroupUseCase`, `UpdateChatTitleUseCase`

**Group operations:**
- `CreateChatGroupUseCase`, `GetChatGroupsUseCase`, `UpdateChatGroupUseCase`, `DeleteChatGroupUseCase`
- `CreatePlannerGroupWithMainChatUseCase`, `CreatePlannerStageChatUseCase`

**Settings:**
- `GetChatSettingsUseCase`, `UpdateChatSettingsUseCase`, `GetChatTypesUseCase`
- `HandleMessageButtonClickUseCase`

### Data Layer

#### Database

- [`ChatDatabase.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/ChatDatabase.kt) - Room database

#### DAOs

- [`ChatDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/ChatDao.kt)
- [`ChatGroupDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/ChatGroupDao.kt)
- [`MessageDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/MessageDao.kt)
- [`ChatSettingsDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/ChatSettingsDao.kt)
- [`ChatTypeDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/ChatTypeDao.kt)
- [`UserDao.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/dao/UserDao.kt)

#### Entities

- [`ChatEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/ChatEntity.kt)
- [`ChatGroupEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/ChatGroupEntity.kt)
- [`MessageEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/MessageEntity.kt)
- [`ChatSettingsEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/ChatSettingsEntity.kt)
- [`ChatTypeEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/ChatTypeEntity.kt)
- [`UserEntity.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/UserEntity.kt)
- [`ButtonsDataModel.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/ButtonsDataModel.kt)

#### Join Relations

- [`ChatWithGroup.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/joins/ChatWithGroup.kt)
- [`ChatGroupWithType.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/joins/ChatGroupWithType.kt)
- [`ChatWithGroupAndSettings.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/model/joins/ChatWithGroupAndSettings.kt)

#### Mappers

- [`ChatMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/ChatMapper.kt)
- [`ChatGroupMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/ChatGroupMapper.kt)
- [`MessageMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/MessageMapper.kt)
- [`ChatSettingsMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/ChatSettingsMapper.kt)
- [`ChatTypeMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/ChatTypeMapper.kt)
- [`UserMapper.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/local/mapper/UserMapper.kt)

#### Repository Implementation

- [`ChatRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/chat/data/ChatRepositoryImpl.kt)

### DI

- [`ChatCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/chat/di/ChatCoreFeatureModule.kt)

## Database Schema

### Tables

| Table | Purpose |
|-------|---------|
| `chats` | Chat entities |
| `messages` | Message entities |
| `chat_groups` | Group entities |
| `chat_types` | Type entities |
| `chat_settings` | Per-chat settings |
| `users` | User entities |

## Module Structure

```
core/core_features/chat/
├── data/
│   ├── local/
│   │   ├── ChatDatabase.kt
│   │   ├── dao/
│   │   │   ├── ChatDao.kt
│   │   │   ├── ChatGroupDao.kt
│   │   │   ├── ChatSettingsDao.kt
│   │   │   ├── ChatTypeDao.kt
│   │   │   ├── MessageDao.kt
│   │   │   └── UserDao.kt
│   │   ├── mapper/
│   │   │   ├── ChatGroupMapper.kt
│   │   │   ├── ChatMapper.kt
│   │   │   ├── ChatSettingsMapper.kt
│   │   │   ├── ChatTypeMapper.kt
│   │   │   ├── MessageMapper.kt
│   │   │   └── UserMapper.kt
│   │   └── model/
│   │       ├── ButtonsDataModel.kt
│   │       ├── ChatDbConst.kt
│   │       ├── ChatEntity.kt
│   │       ├── ChatGroupEntity.kt
│   │       ├── ChatSettingsEntity.kt
│   │       ├── ChatTypeEntity.kt
│   │       ├── MessageEntity.kt
│   │       ├── UserEntity.kt
│   │       └── joins/
│   │           ├── ChatGroupWithType.kt
│   │           ├── ChatWithGroup.kt
│   │           └── ChatWithGroupAndSettings.kt
│   └── ChatRepositoryImpl.kt
├── di/
│   └── ChatCoreFeatureModule.kt
└── domain/
    ├── ChatRepository.kt
    ├── model/
    ├── tools/
    │   ├── ChatTools.kt
    │   └── ChatToolsImpl.kt
    └── usecase/
```
