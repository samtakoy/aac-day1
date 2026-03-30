# Chats Feature Module

**Package:** `com.example.day.features.chats`  
**Module:** `:app`  
**Type:** Android Feature Module

Chat list feature for displaying and managing chats within a chat group.

## Overview

The Chats feature provides:
- Chat list display for a specific group
- Chat creation
- Navigation to chat console
- Last message preview
- Chat deletion

## Purpose

The Chats feature displays **all chats within a selected group**. A chat group (e.g., "Project X") contains multiple chats, each representing a separate conversation thread. This is the **navigation hub** between group selection and individual chat consoles.

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `GroupChoiceFeatureEntry` | Navigates here when user selects a group |
| `MainActivity` | Accesses via `appComponent.getChatFeatureEntry()` |
| `ConsoleFeatureEntry` | Creates new chats via `CreateChatUseCase` |
| `ChatCoreFeature` | Stores chats via `ChatRepository` |
| `MemoryCoreFeature` | Provides artifacts via `ArtifactRepository` |

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ ChatsScreen (UI)                                                 │
│  ├── Chat list with last message preview                        │
│  ├── FAB for creating new chat                                  │
│  └── Click → Navigate to ConsoleFeatureEntry                    │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│ ChatsViewModel                                                   │
│  ├── chats: Flow<List<Chat>>         → GetChatsByGroupUseCase   │
│  ├── onCreateChat()                 → CreateChatUseCase         │
│  └── onDeleteChat()                 → DropChatUseCase          │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│ ChatCoreFeature                                                  │
│  ├── ChatDao (Room)                                              │
│  └── ChatRepository                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Navigation

```
GroupChoiceFeature ──(select group)──> ChatsFeature ──(select chat)──> ConsoleFeature
                                                     │
                                                     ├── RagConsoleFeatureEntry (RAG chats)
                                                     ├── PlannerConsoleFeatureEntry (Planner chats)
                                                     └── AgentsConsoleFeatureEntry (Multi-agent chats)
```

## Chat Types

The feature supports multiple chat types that determine which TalkDelegate is used:

| ChatType | Console Entry | Purpose |
|----------|---------------|---------|
| `LLM` | `ConsoleFeatureEntry` | Direct LLM conversation |
| `RAG` | `RagConsoleFeatureEntry` | RAG-enhanced with file context |
| `PLANNER` | `PlannerConsoleFeatureEntry` | Multi-stage task planning |
| `AGENTS` | `AgentsConsoleFeatureEntry` | Multi-agent collaboration |

## Key Classes

### Entry Point

- [`ChatsFeatureEntry.kt`](app/src/main/java/com/example/day/features/chats/api/ChatsFeatureEntry.kt) - Feature entry interface

```kotlin
interface ChatsFeatureEntry {
    @Composable
    fun EntryPoint(
        groupId: Long,
        modifier: Modifier = Modifier,
        onNavigateBack: (() -> Unit)? = null
    )
}
```

- [`ChatsFeatureEntryImpl.kt`](app/src/main/java/com/example/day/features/chats/impl/ChatsFeatureEntryImpl.kt) - Implementation

### UI

- [`ChatsScreen.kt`](app/src/main/java/com/example/day/features/chats/impl/ui/ChatsScreen.kt) - Main chats list screen

### ViewModel

- [`ChatsViewModel.kt`](app/src/main/java/com/example/day/features/chats/impl/ui/viewmodel/ChatsViewModel.kt) - Interface
- [`ChatsViewModelImpl.kt`](app/src/main/java/com/example/day/features/chats/impl/ui/viewmodel/ChatsViewModelImpl.kt) - Implementation

### Dependency Injection

- [`ChatsFeatureComponent.kt`](app/src/main/java/com/example/day/features/chats/impl/di/ChatsFeatureComponent.kt)
- [`ChatsFeatureDeps.kt`](app/src/main/java/com/example/day/features/chats/impl/di/ChatsFeatureDeps.kt)
- [`ChatsFeatureModule.kt`](app/src/main/java/com/example/day/features/chats/impl/di/ChatsFeatureModule.kt)

## Feature Dependencies

| UseCase | Purpose |
|---------|---------|
| `CreateChatUseCase` | Create new chat |
| `GetChatsByGroupUseCase` | Get chats for group |
| `ArtifactRepository` | Project artifact storage |

## Usage

### Navigation

```kotlin
val chatsEntry = appComponent.getChatFeatureEntry()
chatsEntry.EntryPoint(
    groupId = groupId,
    modifier = Modifier,
    onNavigateBack = { /* handle back */ }
)
```

## Module Structure

```
features/chats/
├── api/
│   └── ChatsFeatureEntry.kt              # Entry interface
├── impl/
│   ├── ChatsFeatureEntryImpl.kt          # Entry implementation
│   ├── di/
│   │   ├── ChatsFeatureComponent.kt
│   │   ├── ChatsFeatureDeps.kt          # Dependencies
│   │   ├── ChatsFeatureModule.kt
│   │   └── ChatsFeatureScope.kt
│   └── ui/
│       ├── ChatsScreen.kt                # Main screen
│       └── viewmodel/
│           ├── ChatsViewModel.kt
│           └── ChatsViewModelImpl.kt
```
