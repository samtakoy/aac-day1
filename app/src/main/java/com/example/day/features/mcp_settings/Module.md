# MCP Settings Feature Module

**Package:** `com.example.day.features.mcp_settings`  
**Module:** `:app`  
**Type:** Android Feature Module

MCP (Model Context Protocol) server management feature for configuring and connecting to MCP servers.

## Overview

The MCP Settings feature provides:
- MCP server configuration (name, URL, transport type)
- Server connection management
- Tool listing from connected servers
- Server enable/disable toggle

## Purpose

The MCP Settings feature allows users to **configure external MCP servers** that provide additional tools to AI agents. MCP (Model Context Protocol) is a standardized way for AI models to interact with external tools and services.

When a server is connected, its tools become available to AI agents through the `McpToolProvider`, enabling:
- Code analysis and file operations
- Git operations
- Custom tool execution via remote servers

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `MainActivity` | Accesses via `appComponent.getMcpSettingsFeatureEntry()` |
| `McpCoreFeature` | Stores server configurations via `McpRepository` |
| `ConsoleFeature` | Uses MCP tools via `GetMcpToolsUseCase` when chatting |
| AI Agents | Execute tools via `McpTools` interface |

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ McpSettingsScreen (UI)                                           │
│  ├── AddMcpServerDialog  → ConnectToMcpServerUseCase          │
│  ├── McpServerCard       → Shows connection status             │
│  └── McpToolList        → GetMcpToolsUseCase                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ McpSettingsViewModel                                             │
│  └── Manages server list, connection state, available tools    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ McpCoreFeature                                                   │
│  ├── McpRepository        → Stores server configs (Room)      │
│  ├── McpProtocol          → Communicates with MCP server     │
│  └── McpToolsImpl         → Provides tools to agents          │
└─────────────────────────────────────────────────────────────────┘
```

## Server Configuration Fields

| Field | Description | Example |
|-------|-------------|---------|
| Name | Display name for the server | "Code Analyzer" |
| URL | Server endpoint URL | `http://10.0.2.2:3000` (emulator localhost) |
| Transport | Protocol type | `STREAMABLE_HTTP` |
| URL Path | MCP endpoint path | `/mcp` (default) |
| Auth Token | Optional Bearer token | `secret123` |

## Transport Types

### STREAMABLE_HTTP

The primary transport type supported. Uses HTTP POST requests for tool calls with JSON-RPC 2.0 format. This is the standard MCP transport for web-based servers.

## Key Classes

### Entry Point

- [`McpSettingsFeatureEntry.kt`](app/src/main/java/com/example/day/features/mcp_settings/api/McpSettingsFeatureEntry.kt) - Feature entry interface

```kotlin
interface McpSettingsFeatureEntry {
    @Composable
    fun EntryPoint(
        modifier: Modifier = Modifier,
        onNavigateBack: () -> Unit
    )
}
```

### UI

- [`McpSettingsScreen.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/McpSettingsScreen.kt) - Main settings screen

**Components:**
- [`AddMcpServerDialog.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/components/AddMcpServerDialog.kt) - Add/edit server dialog
- [`McpServerCard.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/components/McpServerCard.kt) - Server display card
- [`McpToolList.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/components/McpToolList.kt) - Tool list display

### ViewModel

- [`McpSettingsViewModelImpl.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/viewmodel/McpSettingsViewModelImpl.kt)

### UI State

- [`McpSettingsUiState.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/ui/model/McpSettingsUiState.kt)

### Dependency Injection

- [`McpSettingsFeatureComponent.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/di/McpSettingsFeatureComponent.kt)
- [`McpSettingsFeatureDeps.kt`](app/src/main/java/com/example/day/features/mcp_settings/impl/di/McpSettingsFeatureDeps.kt)

## Feature Dependencies

| UseCase | Purpose |
|---------|---------|
| `GetMcpServersUseCase` | List configured servers |
| `ConnectToMcpServerUseCase` | Connect to a server |
| `GetMcpToolsUseCase` | Get tools from server |
| `McpRepository` | Server storage |

## Configuration

### Transport Types

- `STREAMABLE_HTTP` - Streamable HTTP transport
- Custom transports can be added

### Server Configuration

| Field | Description |
|-------|-------------|
| Name | Display name for server |
| URL | Server URL (e.g., `http://10.0.2.2:3000` for emulator) |
| Transport | Transport type (STREAMABLE_HTTP) |
| URL Path | MCP endpoint path (default: `/mcp`) |
| Auth Token | Optional authentication token |

## Usage

### Navigation

```kotlin
val mcpEntry = appComponent.getMcpSettingsFeatureEntry()
mcpEntry.EntryPoint(
    modifier = Modifier,
    onNavigateBack = { /* handle back */ }
)
```

## Module Structure

```
features/mcp_settings/
├── api/
│   └── McpSettingsFeatureEntry.kt        # Entry interface
├── impl/
│   ├── McpSettingsFeatureEntryImpl.kt    # Entry implementation
│   ├── di/
│   │   ├── McpSettingsFeatureComponent.kt
│   │   ├── McpSettingsFeatureDeps.kt     # Dependencies
│   │   ├── McpSettingsFeatureModule.kt
│   │   └── McpSettingsFeatureScope.kt
│   └── ui/
│       ├── McpSettingsScreen.kt          # Main screen
│       ├── components/
│       │   ├── AddMcpServerDialog.kt
│       │   ├── McpServerCard.kt
│       │   └── McpToolList.kt
│       ├── model/
│       │   └── McpSettingsUiState.kt
│       └── viewmodel/
│           └── McpSettingsViewModelImpl.kt
```
