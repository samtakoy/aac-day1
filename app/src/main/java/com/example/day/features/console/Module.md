# Console Feature Module

**Package:** `com.example.day.features.console`  
**Module:** `:app`  
**Type:** Android Feature Module

Main AI chat console feature with support for multiple chat modes (LLM, RAG, Planner, Agents).

## Overview

The Console feature provides:
- AI chat interface with LLM integration
- Multiple talk delegates for different modes
- Command processing (`@@simple`, `@@steps`, `@@prompt`, `@@team`, `@@talk`)
- Chat settings (model, temperature, max tokens)
- Memory inspection
- Stage creation for planner mode

## Purpose

The Console feature is the **main AI chat interface** of the application. It provides different "modes" of interaction via **Talk Delegates**, each optimized for different use cases:

| TalkDelegate | Mode | When to Use |
|-------------|------|-------------|
| `LlmTalkDelegate` | Direct LLM | Simple questions, quick answers |
| `RagTalkDelegate` | RAG-enhanced | Questions about project files/context |
| `PlannerTalkDelegate` | Multi-stage planning | Complex tasks requiring steps |
| `AgentsTalkDelegate` | Multi-agent | Tasks needing multiple AI perspectives |

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `ChatsFeatureEntry` | Navigates to console when user selects a chat |
| `MainActivity` | Accesses via `appComponent.getConsoleFeatureEntry()` |
| `ChatCoreFeature` | Stores messages via `ChatRepository` |
| `AgentCoreFeature` | Processes commands via workers |
| `MemoryCoreFeature` | Provides context via MemoryProviders |
| `McpCoreFeature` | Executes tools via `McpTools` |

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ ConsoleScreen (UI)                                               │
│  ├── ChatListView       → Displays messages                    │
│  ├── ChatBarView        → User input                           │
│  └── ChatSettingsDialog → Model configuration                  │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│ ConsoleViewModel                                                 │
│  ├── messageState       → Flow<List<ChatMessageUiModel>>        │
│  ├── talkDelegate      → Selected mode (LLM/RAG/Planner/etc)   │
│  └── sendMessage()     → Processes user input                 │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│ TalkDelegate (Strategy Pattern)                                  │
│  ├── LlmTalkDelegate    → Direct LLM requests                  │
│  ├── RagTalkDelegate    → RAG context augmentation             │
│  ├── PlannerTalkDelegate → TaskWorker with stages              │
│  └── AgentsTalkDelegate  → TeamWorker with multiple agents     │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│ AgentCoreFeature (Workers)                                       │
│  ├── SimpleWorker (@@simple)   → Basic execution               │
│  ├── StepWorker (@@steps)      → Step-by-step reasoning        │
│  ├── PromptWorker (@@prompt)    → Prompt generation             │
│  ├── TeamWorker (@@team)       → Multi-agent collaboration     │
│  ├── TalkWorker (@@talk)       → Context-aware chat            │
│  └── TaskWorker (planner)      → State machine execution       │
└─────────────────────────────────────────────────────────────────┘
```

## Command System

Commands are prefixed with `@@` and trigger different worker behaviors:

| Command | Worker | Purpose |
|---------|--------|---------|
| `@@simple <query>` | SimpleWorker | Direct LLM execution without special processing |
| `@@steps <task>` | StepWorker | Breaks task into steps, executes sequentially |
| `@@prompt <goal>` | PromptWorker | Generates a prompt template |
| `@@team <task>` | TeamWorker | Creates multiple agents to work on task |
| `@@talk <query>` | TalkWorker | Context-aware conversation with memory |
| `@@compare <options>` | CompareWorker | Compares different approaches |

## Key Classes

### Entry Points

| Entry | Purpose |
|-------|---------|
| [`ConsoleFeatureEntry.kt`](app/src/main/java/com/example/day/features/console/api/ConsoleFeatureEntry.kt) | Base entry |
| [`RagConsoleFeatureEntry.kt`](app/src/main/java/com/example/day/features/console/api/RagConsoleFeatureEntry.kt) | RAG-enabled chat |
| [`PlannerConsoleFeatureEntry.kt`](app/src/main/java/com/example/day/features/console/api/PlannerConsoleFeatureEntry.kt) | Multi-stage planner |
| [`AgentsConsoleFeatureEntry.kt`](app/src/main/java/com/example/day/features/console/api/AgentsConsoleFeatureEntry.kt) | Multi-agent chat |

### UI

- [`ConsoleScreen.kt`](app/src/main/java/com/example/day/features/console/impl/ui/ConsoleScreen.kt) - Main console screen

**Components:**
- [`ChatSettingsDialog.kt`](app/src/main/java/com/example/day/features/console/impl/ui/components/ChatSettingsDialog.kt) - Chat settings
- [`ChatSettingsView.kt`](app/src/main/java/com/example/day/features/console/impl/ui/components/ChatSettingsView.kt) - Settings UI
- [`ModelSettingsView.kt`](app/src/main/java/com/example/day/features/console/impl/ui/components/ModelSettingsView.kt) - Model configuration
- [`MemoryInspectorView.kt`](app/src/main/java/com/example/day/features/console/impl/ui/components/MemoryInspectorView.kt) - Memory debug view
- [`StageCreationDialog.kt`](app/src/main/java/com/example/day/features/console/impl/ui/components/StageCreationDialog.kt) - Planner stage creation

### ViewModel

- [`ConsoleViewModel.kt`](app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModel.kt) - Interface
- [`ConsoleViewModelImpl.kt`](app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt) - Implementation

### Talk Delegates

Base interface and implementations for different chat modes:

- [`TalkDelegate.kt`](app/src/main/java/com/example/day/features/console/impl/ui/delegates/TalkDelegate.kt) - Base interface
- [`LlmTalkDelegate.kt`](app/src/main/java/com/example/day/features/console/impl/ui/delegates/LlmTalkDelegate.kt) - Direct LLM
- [`RagTalkDelegate.kt`](app/src/main/java/com/example/day/features/console/impl/ui/delegates/RagTalkDelegate.kt) - RAG-enhanced
- [`PlannerTalkDelegate.kt`](app/src/main/java/com/example/day/features/console/impl/ui/delegates/PlannerTalkDelegate.kt) - Multi-stage
- [`AgentsTalkDelegate.kt`](app/src/main/java/com/example/day/features/console/impl/ui/delegates/AgentsTalkDelegate.kt) - Multi-agent

### Domain

- [`ChatCommand.kt`](app/src/main/java/com/example/day/features/console/impl/domain/ChatCommand.kt) - Command parsing
- [`AgMessageHandler.kt`](app/src/main/java/com/example/day/features/console/impl/domain/agents/AgMessageHandler.kt) - Agent message handling

### Dependency Injection

- [`ConsoleFeatureComponent.kt`](app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureComponent.kt)
- [`ConsoleFeatureDeps.kt`](app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureDeps.kt)

## Workers

The feature uses workers from the agent core for command processing:

| Worker | Command | Purpose |
|--------|---------|---------|
| `RejectWorker` | — | Invalid command rejection |
| `CompareWorker` | `@@compare` | Compare approaches |
| `SimpleWorker` | `@@simple` | Direct execution |
| `StepWorker` | `@@steps` | Step-by-step reasoning |
| `PromptWorker` | `@@prompt` | Prompt generation |
| `TeamWorker` | `@@team` | Multi-agent collaboration |
| `TalkWorker` | `@@talk` | Context-aware conversation |
| `TaskWorker` | — | Task state machine |
| `RagWorker` | — | RAG context chat |

## Feature Dependencies

### Chat Use Cases

| UseCase | Purpose |
|---------|---------|
| `GetChatMessagesAsFlowUseCase` | Observe messages |
| `GetChatMessagesWithStatusUseCase` | Messages with status |
| `AddChatMessageUseCase` | Send message |
| `ChangeMessageStatusUseCase` | Update status |
| `UpdateChatSettingsUseCase` | Configure chat |
| `CreateChatUseCase` | Create new chat |
| `GetOrCreateChatUseCase` | Get or create |
| `UpdateChatTitleUseCase` | Rename chat |

### LLM & MCP

| Component | Purpose |
|-----------|---------|
| `LlmRequestUseCase` | LLM API calls |
| `McpRepository` | MCP server storage |
| `McpTools` | MCP tool execution |

### Memory & Agents

| Component | Purpose |
|-----------|---------|
| `LongTermMemoryRepository` | LTM storage |
| `ArtifactRepository` | Project artifacts |
| `ChatTools` | Chat operations |
| `GetAgentContextUseCase` | Agent context |

## Usage

### Navigation

```kotlin
val consoleEntry = appComponent.getConsoleFeatureEntry()
consoleEntry.EntryPoint(
    chatId = chatId,
    modifier = Modifier
)
```

### Command Examples

```
@@simple Explain this code
@@steps Implement a sorting algorithm
@@prompt Generate a README
@@team Analyze and review
@@talk(rag --gentest) Run RAG test
```

## Module Structure

```
features/console/
├── api/
│   ├── ConsoleFeatureEntry.kt
│   ├── RagConsoleFeatureEntry.kt
│   ├── PlannerConsoleFeatureEntry.kt
│   └── AgentsConsoleFeatureEntry.kt
├── impl/
│   ├── ConsoleFeatureEntryIml.kt
│   ├── RagConsoleFeatureEntryImpl.kt
│   ├── PlannerConsoleFeatureEntryImpl.kt
│   ├── AgentsConsoleFeatureEntryImpl.kt
│   ├── di/
│   │   ├── ConsoleFeatureComponent.kt
│   │   ├── ConsoleFeatureDeps.kt
│   │   ├── ConsoleFeatureModule.kt
│   │   └── ConsoleFeatureScope.kt
│   ├── domain/
│   │   ├── ChatCommand.kt
│   │   └── agents/
│   │       └── AgMessageHandler.kt
│   └── ui/
│       ├── ConsoleScreen.kt
│       ├── components/
│       │   ├── ChatSettingsDialog.kt
│       │   ├── ChatSettingsView.kt
│       │   ├── ModelSettingsView.kt
│       │   ├── MemoryInspectorView.kt
│       │   ├── SliderTextField.kt
│       │   └── StageCreationDialog.kt
│       ├── delegates/
│       │   ├── TalkDelegate.kt
│       │   ├── LlmTalkDelegate.kt
│       │   ├── RagTalkDelegate.kt
│       │   ├── PlannerTalkDelegate.kt
│       │   └── AgentsTalkDelegate.kt
│       └── viewmodel/
│           ├── ConsoleViewModel.kt
│           └── ConsoleViewModelImpl.kt
```
