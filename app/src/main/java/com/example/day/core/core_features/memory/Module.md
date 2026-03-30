# Memory Core Feature Module

**Package:** `com.example.day.core.core_features.memory`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

Long-term memory and RAG (Retrieval Augmented Generation) system for AI agents.

## Overview

The Memory feature provides:
- Long-term memory storage with facts
- User profile management
- LTM group organization
- RAG context management
- Short history for conversations
- Task state tracking

## Purpose

The Memory feature provides **context management** for AI agents. It supplies relevant information to agents through the `MemoryProvider` interface, enabling:

1. **Long-Term Memory (LTM)** - Persistent facts about the user or project that persist across conversations
2. **User Profiles** - User attributes that personalize AI responses
3. **RAG Context** - Project files and code injected into the prompt for context-aware answers
4. **Short History** - Recent conversation messages for continuity
5. **Task State** - Planning state for multi-stage task execution

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `AgentCoreFeature` | Reads memory via `MemoryProvider` implementations |
| `ConsoleFeature` | Uses `RagContextMemoryProvider` for RAG chats |
| `UserSettingsFeature` | Manages profiles via `UserProfileRepository` |
| `PlannerTalkDelegate` | Uses `TaskStateRepository` for planning |

## MemoryProvider System

`MemoryProvider` is the core interface that supplies memory content to agents:

```kotlin
interface MemoryProvider {
    fun getMemoryContent(agentContext: AContext): List<AContextMessage>
}
```

### Provider Implementations

| Provider | Content Source | Used By |
|----------|----------------|---------|
| `UserProfileMemoryProvider` | User facts from `UserProfileRepository` | All agents |
| `AgentRulesMemoryProvider` | Static agent rules/instructions | All agents |
| `AgentSystemPromptMemoryProvider` | System prompts | All agents |
| `AgentToolsMemoryProvider` | Available tools description | All agents |
| `RagContextMemoryProvider` | File contents from `RagSearchRepository` | RAG chats |
| `AutoRagMemoryProvider` | Auto-selected relevant files | RAG chats |
| `TaskStateMemoryProvider` | Current planning state | Planner mode |
| `CompositeMemoryProvider` | Combines multiple providers | All agents |

## RAG (Retrieval Augmented Generation)

RAG enables AI to answer questions about project files. When a user asks about code:

1. `RagSearchRepository` finds relevant files using `RagSearchRepositoryImpl`
2. `RagContextMemoryProvider` injects file contents into agent context
3. Agent can read and explain the code

```kotlin
// RagSearchRepository finds relevant files
interface RagSearchRepository {
    suspend fun searchFiles(query: String, limit: Int): List<String>
    suspend fun getFileContent(path: String): String?
}
```

## Data Storage

All memory data is persisted in Room database:

| Table | Entity | Purpose |
|-------|--------|---------|
| `user_profiles` | `UserProfileEntity` | User profiles |
| `ltm_facts` | `LongTermMemoryFactEntity` | Facts with categories |
| `ltm_groups` | `LTMGroupEntity` | Fact organization |
| `artifacts` | `ArtifactEntity` | Project artifacts/files |

## Key Components

### Domain Layer

#### Models

- [`UserProfile.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/model/UserProfile.kt)
- [`LongTermMemoryFact.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/model/LongTermMemoryFact.kt)
- [`LTMGroup.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/model/LTMGroup.kt)
- [`Artifact.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/model/Artifact.kt)

#### Memory Providers (`domain/provider/`)

Base interface: [`MemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/base/MemoryProvider.kt)

Implementations:
- [`AgentRulesMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AgentRulesMemoryProvider.kt)
- [`AgentSystemPromptMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AgentSystemPromptMemoryProvider.kt)
- [`AgentToolsMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AgentToolsMemoryProvider.kt)
- [`AutoRagMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/AutoRagMemoryProvider.kt)
- [`RagContextMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/RagContextMemoryProvider.kt)
- [`TaskStateMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/TaskStateMemoryProvider.kt)
- [`UserProfileMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/UserProfileMemoryProvider.kt)
- [`CompositeMemoryProvider.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/CompositeMemoryProvider.kt)

#### Repositories

- [`UserProfileRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/repository/UserProfileRepository.kt)
- [`LongTermMemoryRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/repository/LongTermMemoryRepository.kt)
- [`LTMGroupRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/repository/LTMGroupRepository.kt)
- [`ArtifactRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/repository/ArtifactRepository.kt)

#### RAG Repositories

- [`RagSearchRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/rag/RagSearchRepository.kt)
- [`ShortHistoryRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/rag/ShortHistoryRepository.kt)
- [`TaskStateRepository.kt`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/rag/TaskStateRepository.kt)

#### Use Cases

- `CreateUserProfileUseCase`, `GetCurrentUserProfileUseCase`, `GetAllProfilesUseCase`
- `BindUserProfileUseCase`, `UnbindUserProfileUseCase`
- `UpsertFactWithCategoryUseCase`, `DeleteProfileFactUseCase`
- `GenerateProfileAvatarUseCase`, `UpdateProfileAvatarUseCase`
- `GetFactsByChatGroupUseCase`, `GetLongTermMemoryByGroupUseCase`

### Data Layer

#### DAOs

- [`UserProfileDao.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/local/dao/UserProfileDao.kt)
- [`LongTermMemoryDao.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/local/dao/LongTermMemoryDao.kt)
- [`LTMGroupDao.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/local/dao/LTMGroupDao.kt)
- [`ArtifactDao.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/local/dao/ArtifactDao.kt)

#### Repository Implementations

- [`UserProfileRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/UserProfileRepositoryImpl.kt)
- [`LongTermMemoryRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/LongTermMemoryRepositoryImpl.kt)
- [`LTMGroupRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/LTMGroupRepositoryImpl.kt)
- [`ArtifactRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/ArtifactRepositoryImpl.kt)
- [`RagSearchRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/RagSearchRepositoryImpl.kt)
- [`ShortHistoryRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/ShortHistoryRepositoryImpl.kt)
- [`TaskStateRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/memory/data/repository/TaskStateRepositoryImpl.kt)

### DI

- [`MemoryCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/memory/di/MemoryCoreFeatureModule.kt)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MemoryProvider (base)                     │
│  Provides memory content to agents                          │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                  Concrete Providers                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │ User    │ │  LTM   │ │  RAG   │ │ TaskState      │  │
│  │ Profile │ │ Facts  │ │ Context│ │ Memory         │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Repositories                            │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │ User    │ │  LTM   │ │  RAG   │ │ Artifact       │  │
│  │ Profile │ │ Group  │ │ Search │ │                │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                        Room Database                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │ User    │ │ LTM    │ │ LTM    │ │ Project        │  │
│  │ Profile │ │ Fact   │ │ Group  │ │ Artifact       │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
core/core_features/memory/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── mapper/
│   │   └── model/
│   └── repository/
├── di/
│   └── MemoryCoreFeatureModule.kt
└── domain/
    ├── MemoryProvider.kt
    ├── model/
    ├── provider/
    │   ├── base/
    │   └── rag/
    ├── repository/
    └── usecase/
```
