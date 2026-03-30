# MCP Core Feature Module

**Package:** `com.example.day.core.core_features.mcp`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

MCP (Model Context Protocol) client for Android - connects to MCP servers and provides tools to AI agents.

## Overview

The MCP feature provides:
- MCP server connection management
- Tool listing and calling via MCP protocol
- Local MCP tools (code analysis, reminders, git files)
- File analysis caching
- Git file caching

## Purpose

The MCP feature implements the **Model Context Protocol** client, allowing AI agents to use **external tools** hosted on remote MCP servers or local in-memory services. This extends agent capabilities beyond just LLM chat.

### Two Tool Sources

| Source | Description | Examples |
|--------|-------------|----------|
| Remote MCP Servers | External tools via HTTP | Code analysis, database queries, APIs |
| Local In-Memory | Built-in Android tools | SetReminderTool, AnalyzeCodeContentTool |

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `McpSettingsFeature` | Displays available servers and tools |
| `AgentCoreFeature` | Gets tools via `McpTools` interface for tool calling |
| `ToolCallOrchestrator` | Routes tool calls to appropriate MCP server |

## MCP Protocol Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ AI Agent (decides to call a tool)                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ ToolCallOrchestrator                                             │
│  └── Identifies which MCP server provides the tool              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ McpTools (Interface)                                             │
│  ├── listTools(serverId)    → Lists available tools            │
│  └── callTool(toolName, args) → Executes tool                  │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│ Remote MCP Server        │     │ Local In-Memory Service    │
│ (via McpProtocol)        │     │ (LocalMcpService)          │
│                         │     │                             │
│ POST /mcp               │     │ SetReminderTool            │
│ {method, args}          │     │ AnalyzeCodeContentTool     │
└─────────────────────────┘     │ GetFileAnalysisTool        │
                                │ InvestigateGitFileTool    │
                                └─────────────────────────────┘
```

## Local In-Memory Tools

These tools run locally without a remote server:

| Tool | Purpose |
|------|---------|
| `SetReminderTool` | Schedules reminders via `ReminderScheduler` |
| `AnalyzeCodeContentTool` | Parses and analyzes code files |
| `GetFileAnalysisTool` | Returns cached file analysis |
| `InvestigateGitFileTool` | Cached git file information |

## Server Configuration

MCP servers are configured via `McpServerEntity`:

```kotlin
data class McpServerEntity(
    val id: String,
    val name: String,
    val url: String,
    val transportType: String,  // "STREAMABLE_HTTP"
    val urlPath: String = "/mcp",
    val authToken: String? = null,
    val enabled: Boolean = true
)
```

## Key Components

### Domain Layer

#### Models

- [`McpModels.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/model/McpModels.kt) - Server, Tool, Transport models
- [`FileAnalysis.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/model/FileAnalysis.kt)
- [`GitFileCache.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/model/GitFileCache.kt)

#### Repositories

- [`McpRepository.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/McpRepository.kt) - Server storage
- [`FileAnalysisRepository.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/FileAnalysisRepository.kt)
- [`GitFileCacheRepository.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/GitFileCacheRepository.kt)

#### Tools

- [`McpTools.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/tools/McpTools.kt) - Interface for MCP tools

#### Use Cases

- [`ConnectToMcpServerUseCase.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/usecase/ConnectToMcpServerUseCase.kt)
- [`GetMcpServersUseCase.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/usecase/GetMcpServersUseCase.kt)
- [`GetMcpToolsUseCase.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/usecase/GetMcpToolsUseCase.kt)

### Data Layer

#### Remote

- [`McpProtocol.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/remote/McpProtocol.kt) - MCP protocol implementation
- [`McpTransport.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/remote/McpTransport.kt) - HTTP transport

#### Local In-Memory Tools

- [`LocalMcpService.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/LocalMcpService.kt)
- [`LocalMcpTool.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/LocalMcpTool.kt)
- [`SetReminderTool.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/SetReminderTool.kt)
- [`AnalyzeCodeContentTool.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/AnalyzeCodeContentTool.kt)
- [`GetFileAnalysisTool.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/GetFileAnalysisTool.kt)
- [`InvestigateGitFileTool.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/InvestigateGitFileTool.kt)

#### Implementations

- [`McpRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/McpRepositoryImpl.kt)
- [`McpToolsImpl.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/McpToolsImpl.kt)
- [`FileAnalysisRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/FileAnalysisRepositoryImpl.kt)
- [`GitFileCacheRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/GitFileCacheRepositoryImpl.kt)

#### Database

- [`McpServerDao.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/McpServerDao.kt)
- [`FileAnalysisDao.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/FileAnalysisDao.kt)
- [`GitFileCacheDao.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/GitFileCacheDao.kt)
- [`McpServerEntity.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/entity/McpServerEntity.kt)
- [`FileAnalysisEntity.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/entity/FileAnalysisEntity.kt)
- [`GitFileCacheEntity.kt`](app/src/main/java/com/example/day/core/core_features/mcp/data/local/entity/GitFileCacheEntity.kt)

### DI

- [`McpCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/mcp/di/McpCoreFeatureModule.kt)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ McpRepository│ │ McpTools   │  │  UseCases          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Remote      │  │ Local       │  │ Repository Impls   │ │
│  │ MCPProtocol│  │ InMemory    │  │                     │ │
│  │ McpTransport│  │ Tools       │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
core/core_features/mcp/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── inmemory/
│   │   │   ├── LocalMcpService.kt
│   │   │   ├── LocalMcpTool.kt
│   │   │   ├── SetReminderTool.kt
│   │   │   ├── AnalyzeCodeContentTool.kt
│   │   │   ├── GetFileAnalysisTool.kt
│   │   │   └── InvestigateGitFileTool.kt
│   │   └── SecretsVault.kt
│   ├── remote/
│   │   ├── McpProtocol.kt
│   │   └── McpTransport.kt
│   ├── FileAnalysisRepositoryImpl.kt
│   ├── GitFileCacheRepositoryImpl.kt
│   ├── McpRepositoryImpl.kt
│   └── McpToolsImpl.kt
├── di/
│   └── McpCoreFeatureModule.kt
└── domain/
    ├── McpConstants.kt
    ├── McpFormatting.kt
    ├── McpLocalConstants.kt
    ├── model/
    ├── repository/
    ├── tools/
    └── usecase/
```
