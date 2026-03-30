# RAG Server Module

**Package:** `com.example.day.ragserver`  
**Module:** `:rag-server`  
**Type:** Backend Service (Kotlin/JVM)

RAG (Retrieval Augmented Generation) Server provides semantic code search capabilities with support for multiple retrieval strategies, reranking, and query optimization.

## Overview

The RAG Server indexes Kotlin/Markdown source files and provides:
- **MCP Tools** for AI agents (search_codebase, search_codebase_smart, etc.)
- **HTTP REST API** for Android application (`/search`, `/evaluate`, `/task-state/update`)

## Key Components

### Main Entry Point

- [`RagServer.kt`](src/main/kotlin/com/example/day/ragserver/RagServer.kt) - Main server with Ktor HTTP and MCP integration

### Indexing (`indexing/`)

- [`IndexingService.kt`](src/main/kotlin/com/example/day/ragserver/indexing/IndexingService.kt) - Orchestrates code indexing
- [`ChunkingStrategy.kt`](src/main/kotlin/com/example/day/ragserver/indexing/ChunkingStrategy.kt) - Code chunking strategies (structural, fixed)
- [`MetadataExtractor.kt`](src/main/kotlin/com/example/day/ragserver/indexing/MetadataExtractor.kt) - LLM-based metadata extraction
- [`FileScanner.kt`](src/main/kotlin/com/example/day/ragserver/indexing/FileScanner.kt) - File discovery

### Search (`search/`)

- [`TwoStageSearchService.kt`](src/main/kotlin/com/example/day/ragserver/search/TwoStageSearchService.kt) - Two-stage retrieval (classes → methods)
- [`SearchService.kt`](src/main/kotlin/com/example/day/ragserver/search/SearchService.kt) - Base search service
- [`QueryOptimizer.kt`](src/main/kotlin/com/example/day/ragserver/search/QueryOptimizer.kt) - Query rewrite + translation

### Reranking (`search/rerank/`)

- [`Reranker.kt`](src/main/kotlin/com/example/day/ragserver/search/rerank/Reranker.kt) - Interface
- [`LlmReranker.kt`](src/main/kotlin/com/example/day/ragserver/search/rerank/LlmReranker.kt) - LLM-based reranking
- [`HeuristicReranker.kt`](src/main/kotlin/com/example/day/ragserver/search/rerank/HeuristicReranker.kt) - Keyword overlap boosting
- [`NoopReranker.kt`](src/main/kotlin/com/example/day/ragserver/search/rerank/NoopReranker.kt) - Pass-through

### Pipeline (`pipeline/`)

- [`PipelineExecutor.kt`](src/main/kotlin/com/example/day/ragserver/pipeline/PipelineExecutor.kt) - Pipeline orchestration
- [`PipelineConfig.kt`](src/main/kotlin/com/example/day/ragserver/pipeline/PipelineConfig.kt) - Configuration dataclass
- [`PipelinePreset.kt`](src/main/kotlin/com/example/day/ragserver/pipeline/PipelinePreset.kt) - Named presets (baseline, filtered, reranked_*)

### Context Formatting (`search/context/`)

- [`ContextFormatter.kt`](src/main/kotlin/com/example/day/ragserver/search/context/ContextFormatter.kt) - Formats results for LLM
- [`ContextPacker.kt`](src/main/kotlin/com/example/day/ragserver/search/context/ContextPacker.kt) - Groups chunks by file/class

### Embedding (`embedding/`)

- [`EmbeddingProvider.kt`](src/main/kotlin/com/example/day/ragserver/embedding/EmbeddingProvider.kt) - Interface
- [`OllamaEmbeddingProvider.kt`](src/main/kotlin/com/example/day/ragserver/embedding/OllamaEmbeddingProvider.kt) - Ollama implementation
- [`OpenRouterEmbeddingProvider.kt`](src/main/kotlin/com/example/day/ragserver/embedding/OpenRouterEmbeddingProvider.kt) - OpenRouter implementation

### Database (`db/`)

- [`CodeDatabase.kt`](src/main/kotlin/com/example/day/ragserver/db/CodeDatabase.kt) - SQLite database wrapper
- [`ChunkEntity.kt`](src/main/kotlin/com/example/day/ragserver/db/ChunkEntity.kt) - Chunk data model

### Agent Context (`agent_context/`)

- [`TaskStateUpdaterService.kt`](src/main/kotlin/com/example/day/ragserver/agent_context/TaskStateUpdaterService.kt) - Task state management via LLM
- [`TaskStateModels.kt`](src/main/kotlin/com/example/day/ragserver/agent_context/TaskStateModels.kt) - Data models

### Evaluation (`evaluation/`)

- [`EvaluationService.kt`](src/main/kotlin/com/example/day/ragserver/evaluation/EvaluationService.kt) - Automated testing across presets

### Logging (`logging/`)

- [`SessionLogger.kt`](src/main/kotlin/com/example/day/ragserver/logging/SessionLogger.kt) - Session logging to markdown

## MCP Tools

| Tool | Description | Use Case |
|------|-------------|----------|
| `search_codebase` | Hybrid search on structural blocks | Specific class or method by name |
| `search_codebase_fixed` | Hybrid search on fixed-size chunks | Wide contextual search |
| `search_codebase_smart` | 2-Stage: classes first, then methods | Conceptual questions |
| `get_index_status` | Index status check | Verify readiness before search |

## HTTP REST API

### GET /search

Search the code base with configurable pipeline.

```
GET /search?query=<text>&[pipeline params]
```

**Parameters:**
- `query` (required) - Search query
- `preset` - Named preset (baseline, filtered, reranked_heuristic, reranked_llm)
- `retrieval_strategy` - `two_stage` or `hybrid`
- `retrieval_topK` - Candidates before filtering (default: 10)
- `threshold` - Similarity threshold (0.5-0.65 recommended)
- `rerank_strategy` - `none`, `heuristic`, `llm`
- `final_topK` - Results after reranking (default: 5)
- `enable_query_optimize` - Enable query rewrite (requires `TRANSLATE_QUERIES=true`)
- `task_state` - JSON TaskState for context-aware rewrite
- `history` - Short dialogue history

**Example:**
```bash
curl "http://localhost:3001/search?query=how+does+ContextPacker+work&preset=reranked_llm"
```

### POST /task-state/update

Update task memory via LLM (used by Android client).

```bash
curl -X POST http://localhost:3001/task-state/update \
  -H "Content-Type: application/json" \
  -d '{"currentState": "...", "lastMessages": [...]}'
```

### POST /evaluate

Run automated tests across pipeline presets.

```bash
curl -X POST http://localhost:3001/evaluate \
  -H "Content-Type: application/json" \
  -d '{"questions": ["How does X work?"], "presets": ["all"]}'
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CODE_PATH` | (required) | Path to source files |
| `DB_PATH` | `./rag_index.db` | SQLite database path |
| `EMBEDDING_PROVIDER` | `ollama` | `ollama` or `openrouter` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama URL |
| `EMBEDDING_MODEL` | `nomic-embed-text` | Embedding model |
| `EXTRACT_METADATA` | `false` | Enable LLM metadata extraction |
| `LLM_MODEL` | `qwen2.5-coder:7b-instruct` | LLM for metadata |
| `TRANSLATE_QUERIES` | `false` | Enable query optimization |
| `RERANKER_LLM_MODEL` | (from LLM_MODEL) | LLM for reranking |
| `SEARCH_TOP_K` | `5` | Default top-K for MCP tools |

### Pipeline Presets

| Preset | retrieval_topK | threshold | rerank | final_topK |
|--------|----------------|------------|--------|------------|
| `baseline` | 10 | — | none | 5 |
| `filtered` | 15 | 0.65 | none | 5 |
| `reranked_heuristic` | 15 | 0.50 | heuristic | 5 |
| `reranked_llm` | 15 | 0.50 | llm | 5 |

## Usage Example

### Starting the Server

```bash
export CODE_PATH="/path/to/project/src"
export EXTRACT_METADATA=true
export LLM_MODEL=qwen2_5-coder_7b-instruct
export TRANSLATE_QUERIES=true

java -jar rag-server/build/libs/rag-server.jar
```

### From Android Application

```kotlin
// Via AI Gateway or direct HTTP
val response = httpClient.get("http://10.0.2.2:3001/search") {
    parameter("query", "how does ContextPacker work")
    parameter("preset", "reranked_llm")
}
```

### Via MCP Protocol

```bash
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc": "2.0", "id": 1, "method": "tools/call",
       "params": {"name": "search_codebase_smart", "arguments": {"query": "authorization"}}}'
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      RagServer.kt                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Ktor HTTP   │  │ MCP Server  │  │ TaskStateUpdater    │ │
│  │ /search     │  │ /mcp        │  │ /task-state/update  │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼───────────────────┼─────────────┘
          │                │                   │
          ▼                ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                    PipelineExecutor                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │ Query   │→ │Retrieval│→ │ Rerank  │→ │  TopK   │        │
│  │ Optimize│  │  Step   │  │  Step   │  │  Step   │        │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
└─────────────────────────────────────────────────────────────┘
          │                │                   │
          ▼                ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      Services                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ QueryOptimizer  │  │TwoStageSearch   │  │  Reranker  │ │
│  │ (rewrite+trans) │  │  Service        │  │ (LLM/Heur) │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  CodeDatabase   │
                    │   (SQLite)      │
                    └─────────────────┘
```
