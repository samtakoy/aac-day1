# JetBrains Koog — Agents Framework

> Kotlin-based framework for building and running AI agents

## Overview

Koog is a multiplatform Kotlin framework for creating AI agents with support for:
- Tool calling (native + MCP)
- MCP (Model Context Protocol) server integration
- Multiple LLM providers (OpenAI, Ollama, Google, Bedrock, etc.)
- Various agent architectures (basic, functional, graph-based, planner)
- Streaming responses
- Chat memory management

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         AIAgent                             │
│  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ PromptExecutor │  │ ToolRegistry │  │    Strategy    │  │
│  │    (LLM)       │  │              │  │ (singleRun/    │  │
│  └────────────────┘  └──────────────┘  │  functional/   │  │
│                                        │  graph-based)  │  │
│                                        └────────────────┘  │
└─────────────────────────────────────────────────────────────┘
         │                   │
         ▼                   ▼
┌─────────────────┐  ┌─────────────────┐
│    ToolSet      │  │ McpToolProvider │──► MCP Server
│  @Tool methods  │  │                 │    (stdio/SSE)
└─────────────────┘  └─────────────────┘
```

---

## Agent Types

### 1. Basic Agents

Simple agents with predefined strategy for common use cases.

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
    systemPrompt = "You are an expert in internet memes.",
    llmModel = OpenAIModels.Chat.GPT4o
)
```

### 2. Functional Agents

Custom logic defined as lambda functions.

```kotlin
val strategy = functionalStrategy<String, String> { input ->
    var responses = requestLLMMultiple(input)
    while (responses.containsToolCalls()) {
        val pendingCalls = extractToolCalls(responses)
        val results = executeMultipleTools(pendingCalls)
        responses = sendMultipleToolResults(results)
    }
    responses.single().asAssistantMessage().content
}

val mathAgent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    strategy = strategy
)
```

### 3. Graph-Based Agents

Complex workflows as directed graphs with nodes and edges.

```kotlin
val strategy = strategy<String, String>("banking assistant") {

    val classifyRequest by subgraph<String, ClassifiedBankRequest> {
        val requestClassification by nodeLLMRequestStructured<ClassifiedBankRequest>(...)
        // ... nodes and edges definition
    }

    edge(nodeStart forwardTo classifyRequest)
    edge(classifyRequest forwardTo transferMoney
        onCondition { it.requestType == RequestType.Transfer })
    edge(classifyRequest forwardTo transactionAnalysis
        onCondition { it.requestType == RequestType.Analytics })
}
```

### 4. Planner Agents

Iteratively build and execute plans until desired state is achieved.

---

## Agent Configuration

### Basic Configuration

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7
)
```

### With Tools

```kotlin
val toolRegistry = ToolRegistry {
    tools(MathTools())
}

val agent = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You are a helpful assistant.",
    toolRegistry = toolRegistry
)
```

### Advanced Config with AIAgentConfig

```kotlin
val agentConfig = AIAgentConfig(
    prompt = prompt(id = "banking assistant") {
        system("$bankingAssistantSystemPrompt\n$transactionAnalysisPrompt")
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 50
)

val agent = AIAgent<String, String>(
    promptExecutor = openAIExecutor,
    strategy = strategy,
    agentConfig = agentConfig,
    toolRegistry = toolRegistry,
)
```

### Builder Pattern (Java-friendly)

```kotlin
val agent = AIAgent.builder()
    .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
    .systemPrompt("You are an expert in internet memes.")
    .llmModel(OpenAIModels.Chat.GPT4o)
    .temperature(0.7)
    .build()
```

---

## Tool Calling

### Define Tools with ToolSet

```kotlin
@LLMDescription("Tools for performing math operations")
class MathTools : ToolSet {
    @Tool
    @LLMDescription("Adds two numbers")
    fun add(
        @LLMDescription("First number") a: Int,
        @LLMDescription("Second number") b: Int
    ): Int = a + b

    @Tool
    @LLMDescription("Multiplies two numbers")
    fun multiply(
        @LLMDescription("First number") a: Int,
        @LLMDescription("Second number") b: Int
    ): Int = a * b
}
```

### Register Tools

```kotlin
val mathTools = MathTools()
val toolRegistry = ToolRegistry {
    tools(mathTools)
}

// Or register single tool
val toolRegistry = ToolRegistry {
    tool(MathTools()::multiply)
}
```

### Execute Tools Directly

```kotlin
val addTool = registry.findTool("add")
val args = addTool.decodeArgsFromString("""{"a": 10, "b": 20}""")

runBlocking {
    val (result, stringResult) = addTool.executeAndSerialize(args, enabler)
    println("Result: $stringResult")
}
```

### Tool Annotations

| Annotation | Target | Purpose |
|------------|--------|---------|
| `@Tool` | Method | Marks method as callable tool |
| `@LLMDescription` | Method/Parameter | Description for LLM (affects tool selection and parameter understanding) |

### Tool Calling Flow

```
LLM Request → Agent → ToolRegistry.findTool("toolName")
                              ↓
                    Tool.decodeArgsFromString(json)
                              ↓
                    Tool.executeAndSerialize(args)
                              ↓
                    Returns result to LLM for final response
```

---

## MCP Server Integration

### Connection Methods

#### SSE (Server-Sent Events)

```kotlin
install(Koog) {
    agentConfig {
        mcp {
            sse("https://your-mcp-server.com/sse")
        }
    }
}
```

#### Process

```kotlin
install(Koog) {
    agentConfig {
        mcp {
            process(Runtime.getRuntime().exec("your-mcp-binary ..."))
        }
    }
}
```

#### Existing Client

```kotlin
install(Koog) {
    agentConfig {
        mcp {
            client(existingMcpClient)
        }
    }
}
```

### Integrate MCP Tools with Agent

```kotlin
val toolRegistry = McpToolRegistryProvider.fromClient(
    mcpClient = existingMcpClient,
    serverInfo = McpServerInfo(url = "http://localhost:8931")
)

val agent = AIAgent(
    promptExecutor = executor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
```

### MCP Lifecycle

```
1. Connect to MCP server (stdio/SSE)
        ↓
2. Retrieve available tools list
        ↓
3. Transform MCP tools → Koog Tool interface
        ↓
4. Register in ToolRegistry
        ↓
5. LLM dynamically calls tools with arguments
```

---

## User in the Loop

### Interactive Chat Loop

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    systemPrompt = "You are a helpful assistant.",
    toolRegistry = toolRegistry,
) {
    install(ChatMemory) {
        windowSize(20)
    }
}

while (true) {
    print("You: ")
    val input = readln().trim()
    if (input == "/bye") break
    if (input.isEmpty()) continue

    val reply = agent.run(input, sessionId)
    println("Assistant: $reply\n")
}
```

### Streaming Responses

```kotlin
// Check if agent supports streaming
if (client.cachedAgentCard()?.capabilities?.streaming == true) {
    client.sendMessageStreaming(request).collect { response ->
        when (val event = response.data) {
            is Message -> {
                val text = event.parts
                    .filterIsInstance<TextPart>()
                    .joinToString { it.text }
                print(text)
            }
            is TaskStatusUpdateEvent -> {
                if (event.final) {
                    println("\nTask completed")
                }
            }
        }
    }
}
```

### LLM Streaming

```kotlin
get("/stream") {
    val flow = llm().executeStreaming(
        prompt("streaming") { user("Stream this response, please") },
        OpenRouterModels.GPT4o
    )

    val sb = StringBuilder()
    flow.collect { chunk -> sb.append(chunk) }
    call.respondText(sb.toString())
}
```

---

## ChatMemory (Context Management)

```kotlin
install(ChatMemory) {
    windowSize(20)  // Keep last 20 messages
}
```

**Options:**
- `windowSize(n)` — sliding window with n messages
- Full history
- Summarization (for long contexts)

---

## Strategy Pattern

### singleRunStrategy

One-shot request, no iteration.

```kotlin
val strategy = singleRunStrategy()
```

### functionalStrategy

Custom logic as lambda function.

```kotlin
val strategy = functionalStrategy<String, String> { input ->
    // Custom processing
}
```

### Graph-Based Strategy

Complex multi-node workflows.

```kotlin
val strategy = strategy<String, String>("name") {
    // Graph definition with nodes and edges
}
```

---

## Supported LLM Providers

| Provider | Executor | Models |
|----------|----------|--------|
| OpenAI | `simpleOpenAIExecutor()` | GPT-4o, GPT-4o-mini, etc. |
| Ollama | `simpleOllamaAIExecutor()` | Llama 3.2, Mistral, etc. |
| Google | `simpleGoogleAIExecutor()` | Gemini 2.5 Pro, etc. |
| Bedrock | `bedrockAIExecutor()` | Claude 3.5 Sonnet, etc. |
| OpenRouter | `simpleOpenRouterAIExecutor()` | Various |

---

## Key Modules

| Module | Purpose |
|--------|---------|
| `agents-core` | AIAgent, Strategy, ToolRegistry base |
| `agents-tools` | ToolSet, @Tool, @LLMDescription annotations |
| `agents-mcp` | MCP server integration |
| `agents-planner` | Planner agent implementation |
| `agents-ext` | Extensions (subgraphs, etc.) |
| `prompt-executor-*` | LLM executors (OpenAI, Ollama, Google, Bedrock, etc.) |

---

## Comparison with Day Project

| Koog Concept | Day Project Equivalent |
|--------------|------------------------|
| `AIAgent` | `AIAgent` |
| `ToolRegistry` + `ToolSet` | `ToolProvider` / `ToolCallOrchestrator` |
| `@Tool` / `@LLMDescription` | Tool definitions in `AgentTools` |
| `McpToolRegistryProvider` | `McpToolProvider` |
| Strategy (singleRun/functional/graph) | Workers (`SimpleWorker`, `TalkWorker`, etc.) |
| `ChatMemory` | `AgentContextRepository` |
| `functionalStrategy` | Custom `Worker` implementations |
| Graph-based strategy | `BranchManager` / branching |
| `AIAgentConfig` | Configuration in `AgentConfig` |

---

## Summary

Koog provides a clean, Kotlin-idiomatic API for building AI agents:

1. **Simple setup**: Just provide executor, model, system prompt
2. **Flexible strategies**: From single-run to complex graph workflows
3. **Powerful tools**: Native ToolSet + full MCP integration
4. **Memory management**: Built-in ChatMemory with configurable windows
5. **Multi-provider**: Support for OpenAI, Ollama, Google, Bedrock, and more
6. **Streaming**: First-class support for streaming responses

The framework emphasizes:
- Pure Kotlin implementation
- Type safety
- Functional approach (lambda-based strategies)
- Graph-based workflows for complex scenarios
- Seamless MCP integration for extending agent capabilities
