# Agent Core Feature Module

**Package:** `com.example.day.core.core_features.agent`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

AI Agent system with multi-worker architecture for handling different conversation modes.

## Overview

The Agent feature provides:
- AI agent creation and context management
- Multiple worker types for different commands
- Tool calling orchestration
- Task state machine for planning
- Context strategies (sliding window, summarization, sticky facts)
- Branching support for multi-agent collaboration

## Purpose

The Agent feature is the **brain** of the AI system. It orchestrates:
- **Workers** - Handle different command types (`@@simple`, `@@steps`, etc.)
- **Context** - Manages conversation history with strategies
- **Tools** - Coordinates MCP tool execution
- **Branching** - Supports multi-agent parallel execution

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `ConsoleFeature` | Uses agents via `AgentsTalkDelegate` |
| `PlannerTalkDelegate` | Uses `TaskWorker` for planning |
| `MemoryCoreFeature` | Reads context via `MemoryProvider` |
| `McpCoreFeature` | Executes tools via `McpTools` |
| `LlmCoreFeature` | Sends prompts via `LlmRequestUseCase` |

## Core Concept: AIAgent

`AIAgent` is the main interface that processes user input:

```kotlin
interface AIAgent {
    suspend fun process(
        input: String,
        chatId: String,
        agentId: String
    ): AIAgentResult
}
```

### AIAgentFactory

Creates agents configured for specific purposes:

```kotlin
interface AIAgentFactory {
    fun createAgent(config: AgentConfig): AIAgent
}

// Usage
val agent = factory.createAgent(
    AgentConfig(
        systemName = "assistant",
        chatId = chatId,
        isCommonAgent = false
    )
)
```

## Worker System

Workers process commands prefixed with `@@`:

### Command Routing

```
User input: "@@steps Implement login"
                │
                ▼
┌─────────────────────────────────────────────────────────────────┐
│ AWorker (base interface)                                         │
│  └── Each worker type handles a specific command pattern        │
└─────────────────────────────────────────────────────────────────┘
```

### Worker Types

| Worker | Command | Description |
|--------|---------|-------------|
| `SimpleWorker` | `@@simple` | Direct LLM call, no special processing |
| `StepWorker` | `@@steps` | Breaks task into numbered steps |
| `PromptWorker` | `@@prompt` | Generates structured prompt template |
| `TeamWorker` | `@@team` | Creates multiple sub-agents |
| `TalkWorker` | `@@talk` | Context-aware with memory |
| `CompareWorker` | `@@compare` | Compares multiple options |
| `TaskWorker` | (planner) | State machine for multi-stage tasks |
| `RagWorker` | (rag mode) | RAG context for file questions |
| `McpWorker` | (tools) | MCP tool calling |
| `RejectWorker` | (invalid) | Handles unrecognized commands |

## Context Strategy

`ContextStrategy` determines how conversation history is included in prompts:

| Strategy | Behavior | Use Case |
|----------|----------|----------|
| `ContextEmptyStrategy` | No history | Fresh start |
| `ContextFullStrategy` | All messages | Short conversations |
| `ContextSlidingWindowStrategy` | Last N messages | Long conversations |
| `ContextStickyFactsStrategy` | Keep important facts | User preferences |
| `ContextSummaryStrategy` | Summarize old messages | Very long threads |
| `ContextBranchingStrategy` | Branch-specific context | Multi-agent work |

## Task State Machine (Planner)

`TaskWorker` uses a state machine for multi-stage planning:

```
┌────────┐   plan    ┌──────────┐  execute  ┌───────────┐  verify  ┌────────┐
│  INIT  │ ───────▶ │ PLANNING │ ───────▶ │ EXECUTING │ ──────▶ │  DONE  │
└────────┘          └──────────┘          └───────────┘          └────────┘
                            │                   │
                            │                   ▼
                            │            ┌─────────────┐
                            └───────────▶│ VERIFYING  │
                                         └─────────────┘
```

### State Handlers

| Handler | State | Action |
|---------|-------|--------|
| `InitStateHandler` | INIT | Initialize planning |
| `PlanningStateHandler` | PLANNING | Generate step plan |
| `ExecutionStateHandler` | EXECUTING | Execute current step |
| `VerificationStateHandler` | VERIFYING | Check step result |
| `DoneStateHandler` | DONE | Complete task |

## Tool Calling

When an LLM requests a tool:

```
LLM Output: "I'll use the file_reader tool"
                │
                ▼
┌─────────────────────────────────────────────────────────────────┐
│ ToolCallOrchestrator                                             │
│  └── Parses tool name and arguments                             │
└─────────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────────┐
│ ToolProvider (McpToolProvider)                                   │
│  └── Routes to appropriate MCP server or local tool             │
└─────────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────────┐
│ McpTools / LocalMcpService                                       │
│  └── Executes tool, returns JSON result                         │
└─────────────────────────────────────────────────────────────────┘
```

## Key Components

### Domain Layer

#### Core Models

- [`AIAgent.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt) - Main agent interface
- [`AIAgentFactory.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgentFactory.kt) - Agent factory
- [`AgentConfig.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AgentConfig.kt)
- [`AIAgentResult.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AIAgentResult.kt)

#### Context Models

- [`AContext.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AContext.kt)
- [`AContextExt.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AContextExt.kt)
- [`AContextMessage.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AContextMessage.kt)
- [`AContextOwner.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AContextOwner.kt)
- [`InMemoryContextOwner.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/InMemoryContextOwner.kt)

#### Repositories

- [`AgentRepository.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AgentRepository.kt)
- [`AgentContextRepository.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/AgentContextRepository.kt)
- [`AgentMemoryRepository.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/repository/AgentMemoryRepository.kt)

#### Use Cases

- `GetOrCreateAgentUseCase`, `GetAgentContextUseCase`, `SaveAgentContextUseCase`, `ClearAgentContextUseCase`
- `CompleteStageUseCase`

### Workers (`domain/workers/`)

#### Base

- [`AWorker.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/AWorker.kt) - Base worker interface
- [`WorkerEvent.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/WorkerEvent.kt)

#### Concrete Workers (`workers/concrete/`)

| Worker | Command | Purpose |
|--------|---------|---------|
| `SimpleWorker` | `@@simple` | Direct execution |
| `StepWorker` | `@@steps` | Step-by-step reasoning |
| `PromptWorker` | `@@prompt` | Prompt generation |
| `TeamWorker` | `@@team` | Multi-agent collaboration |
| `TalkWorker` | `@@talk` | Context-aware conversation |
| `CompareWorker` | `@@compare` | Compare approaches |
| `RejectWorker` | — | Invalid command rejection |
| `TaskWorker` | — | Task state machine |
| `RagWorker` | — | RAG context chat |
| `JustWorkWorker` | — | General purpose |
| `McpWorker` | — | MCP tool calling |

#### Task State Machine (`workers/task/`)

- [`TaskStateConfig.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/TaskStateConfig.kt)
- [`TaskStateData.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/TaskStateData.kt)
- [`TaskStateStoreImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_store/TaskStateStoreImpl.kt)
- [`TaskContext.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_store/TaskContext.kt)

**State Handlers:**
- `InitStateHandler`, `PlanningStateHandler`, `ExecutionStateHandler`, `VerificationStateHandler`, `DoneStateHandler`

#### Inner Commands (`workers/innercommand/`)

- [`InnerCommandParser.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/innercommand/InnerCommandParser.kt)

**Command Handlers:**
- `AgentCommandHandler`, `AgentMemoryCommandHandler`, `ProfileCommandHandler`, `RagCommandHandler`
- `ContextCommandHandler`, `InfoCommandHandler`
- `SetupBranchingHandler`, `SetupSlidingWindowHandler`, `SetupStickyFactsHandler`, `SetupSummarizationHandler`

#### Branching (`workers/branch/`)

- `BranchManager`, `BranchCommandValidator`
- `NewBranchCommandHandler`, `SwitchBranchCommandHandler`, `DeleteBranchCommandHandler`, `ListBranchesCommandHandler`

### Strategy (`domain/strategy/`)

Base: `ContextStrategy`, `StrategyFactory`

Implementations:
- `ContextEmptyStrategy`
- `ContextFullStrategy`
- `ContextSlidingWindowStrategy`
- `ContextStickyFactsStrategy`
- `ContextSummaryStrategy`
- `ContextBranchingStrategy`

### Tools (`domain/tools/`)

- [`ToolProvider.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolProvider.kt)
- [`ToolCallOrchestrator.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallOrchestrator.kt)
- [`ToolResponseParser.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolResponseParser.kt)
- [`impl/ToolCallOrchestratorImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt)

### Agent Tools (`domain/workers/tools/`)

- [`AgentTools.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/tools/AgentTools.kt)
- [`AgentToolsImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/tools/AgentToolsImpl.kt)

### Prompt Builder

- [`PlannerPromptBuilder.kt`](app/src/main/java/com/example/day/core/core_features/agent/domain/prompt/PlannerPromptBuilder.kt)

### Data Layer

#### Repositories

- [`AgentRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/AgentRepositoryImpl.kt)
- [`AgentContextRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/AgentContextRepositoryImpl.kt)
- [`AgentMemoryRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/repository/AgentMemoryRepositoryImpl.kt)

#### DAOs

- [`AgentDao.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/local/dao/AgentDao.kt)
- [`AgentContextMemoryDao.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/local/dao/AgentContextMemoryDao.kt)
- [`AgentMemoryDao.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/local/dao/AgentMemoryDao.kt)
- [`AgentToChatDao.kt`](app/src/main/java/com/example/day/core/core_features/agent/data/local/dao/AgentToChatDao.kt)

### DI

- [`AgentCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/agent/di/AgentCoreFeatureModule.kt)
- [`CommandHandlerModule.kt`](app/src/main/java/com/example/day/core/core_features/agent/di/CommandHandlerModule.kt)
- [`TaskStateMachineModule.kt`](app/src/main/java/com/example/day/core/core_features/agent/di/TaskStateMachineModule.kt)
- [`BranchingStrategyModule.kt`](app/src/main/java/com/example/day/core/core_features/agent/di/BranchingStrategyModule.kt)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      AIAgent                                │
│  Orchestrates workers and context strategies               │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Workers                                │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐ │
│  │ Simple  │ │  Step   │ │  Team   │ │ Talk            │ │
│  │ Worker  │ │ Worker  │ │ Worker  │ │ Worker          │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────────────┘ │
│  ┌─────────┐ ┌─────────┐ ┌─────────────────────────────┐  │
│  │  Task   │ │   Rag   │ │ Compare                   │  │
│  │ Worker  │ │ Worker  │ │ Worker                    │  │
│  └─────────┘ └─────────┘ └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Context Strategies                       │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐ │
│  │  Empty  │ │  Full   │ │ Sliding │ │ StickyFacts    │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────────────┘ │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Tool Orchestration                        │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────────────┐ │
│  │ ToolProvider │  │ ToolCall   │  │ ToolResponse      │ │
│  │ (MCP, etc) │  │ Orchestrator│  │ Parser            │ │
│  └─────────────┘  └─────────────┘  └────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
core/core_features/agent/
├── data/
│   ├── AgentContextRepositoryImpl.kt
│   ├── AgentRepositoryImpl.kt
│   ├── local/
│   │   ├── dao/
│   │   ├── mapper/
│   │   ├── model/
│   │   └── repository/
│   ├── repository/
│   │   └── AgentMemoryRepositoryImpl.kt
│   └── tools/
│       └── McpToolProvider.kt
├── di/
│   ├── AgentCoreFeatureModule.kt
│   ├── BranchingStrategyModule.kt
│   ├── CommandHandlerModule.kt
│   └── TaskStateMachineModule.kt
└── domain/
    ├── AIAgent.kt
    ├── AIAgentFactory.kt
    ├── AgentContextRepository.kt
    ├── AgentRepository.kt
    ├── model/
    ├── prompt/
    ├── repository/
    ├── strategy/
    ├── tools/
    ├── usecase/
    ├── utils/
    └── workers/
        ├── base/
        ├── branch/
        ├── concrete/
        ├── innercommand/
        ├── task/
        └── tools/
```
