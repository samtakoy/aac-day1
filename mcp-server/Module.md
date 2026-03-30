# MCP Server Module

**Package:** `com.example.day.mcpserver`  
**Module:** `:mcp-server`  
**Type:** Backend Service (Kotlin/JVM)

MCP (Model Context Protocol) Server provides GitHub integration tools via MCP protocol for AI agents.

## Overview

The MCP Server implements the Model Context Protocol and provides:
- GitHub Issues API integration (CRUD operations)
- Git file investigation tools
- Echo tool for testing

## Key Components

### Main Entry Point

- [`McpServer.kt`](src/main/kotlin/com/example/day/mcpserver/McpServer.kt) - Main server with Ktor and MCP integration

### Tools (`tools/`)

- [`McpTools.kt`](src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt) - MCP tool registration and handlers

### GitHub Integration (`github/`)

- [`GitHubApiClient.kt`](src/main/kotlin/com/example/day/mcpserver/github/GitHubApiClient.kt) - GitHub API HTTP client

## MCP Tools

### Issue Management

| Tool | Description | Parameters |
|------|-------------|------------|
| `get_issue` | Get issue by number | `issueNumber` (required) |
| `list_issues` | List repository issues | `state`, `labels`, `per_page`, `page`, `include_prs` |
| `get_issue_comments` | Get comments for issue | `issueNumber` (required) |
| `create_issue` | Create new issue | `title` (required), `body`, `labels` |
| `create_comment` | Create issue comment | `issueNumber`, `body` (required) |

### User Information

| Tool | Description | Parameters |
|------|-------------|------------|
| `get_user` | Get GitHub user info | `username` (required) |

### File Investigation (Day 19)

| Tool | Description | Parameters |
|------|-------------|------------|
| `get_git_file_list` | List all files in repository | — |
| `get_file_content` | Get file content by path | `file_path` (required) |
| `reset_git_file_list_cache` | Reset file list cache | — |

### Debug

| Tool | Description | Parameters |
|------|-------------|------------|
| `echo` | Echo back input text | `text` (required) |

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GITHUB_TOKEN` | **Yes** | — | GitHub Personal Access Token |
| `GITHUB_OWNER` | No | — | Default repository owner |
| `GITHUB_REPO` | No | — | Default repository name |
| `PORT` | No | `3000` | Server port |

### Required Token Scopes

| Scope | Use Case |
|-------|----------|
| `public_repo` | Public repositories |
| `repo` | Private repositories |

## HTTP API

### MCP Endpoint

```
POST /mcp
```

Handles all MCP protocol operations:
- `initialize` - Initialize connection
- `tools/list` - List available tools
- `tools/call` - Execute a tool

### Health Check

```
POST /message
```

Redirects to `/mcp`.

## Usage Example

### Starting the Server

```bash
export GITHUB_TOKEN="ghp_..."
export GITHUB_OWNER="myorg"
export GITHUB_REPO="myrepo"

./gradlew :mcp-server:run
```

Server starts on `http://0.0.0.0:3000`

### Via MCP Protocol

```bash
# Initialize
curl -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'

# List tools
curl -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# Call tool
curl -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"echo","arguments":{"text":"Hello"}}}'
```

### From Android Application

```kotlin
// Via @@mcp command
@@mcp echo text=Hello

// Via @@talk with automatic tool calling
@@talk Create an issue titled "Bug found" describing the problem
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      McpServer.kt                           │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │ Ktor Netty      │  │ MCP Server (SDK)               │  │
│  │ POST /mcp       │  │ • initialize                    │  │
│  │ POST /message   │  │ • tools/list                    │  │
│  └─────────────────┘  │ • tools/call                    │  │
│                       └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      McpTools.kt                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ │
│  │   echo   │ │  Issues  │ │   User   │ │ File Tools   │ │
│  │  (debug) │ │   CRUD   │ │   Info   │ │ (Day 19)    │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    GitHubApiClient.kt                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ HTTP Client (Ktor)                                  │   │
│  │ • GET /repos/{owner}/{repo}/issues                  │   │
│  │ • POST /repos/{owner}/{repo}/issues                 │   │
│  │ • GET /repos/{owner}/{repo}/contents/{path}         │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
mcp-server/
├── build.gradle.kts
├── Dockerfile
├── mcp_runbook.md
└── src/main/kotlin/com/example/day/mcpserver/
    ├── McpServer.kt              # Main entry point
    ├── github/
    │   └── GitHubApiClient.kt    # GitHub API client
    └── tools/
        └── McpTools.kt          # Tool registration
```
