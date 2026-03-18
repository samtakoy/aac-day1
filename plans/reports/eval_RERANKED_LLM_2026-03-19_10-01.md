# Evaluation: RERANKED_LLM | 2026-03-19_10-01
Config: retrievalTopK=15, threshold=0.5, rerank=LLM, finalTopK=5

---
## Query 1: "какие основные возможности агента, какой класс реализует"
**Optimized:** "what are the main agent capabilities, which class implements"
**Metrics:** Retrieved: 10 → Filtered: 10 → Reranked: 10 → Final: 5
**Timings:** query_optimize=279ms, retrieve=92ms, filter=1ms, rerank=7545ms, top_k=0ms, pack=1ms
**Top score:** 0,85 | Avg score: 0,75

### RAG Context:
Found 4 relevant class(es) | ~9760 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt
Score: 0,850
Declarations: for

--- for ---
// File: AIAgentFeature.kt
package ai.koog.agents.core.feature

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentFunctionalPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline

/**
 * A class for Agent Feature that can be added to an agent pipeline,
 * The feature stands for providing specific functionality and configuration capabilities.
 *
 * @param TConfig The type representing the configuration for this feature.
 * @param TFeatureImpl The type of the feature implementation.
 */
public interface AIAgentFeature<TConfig : FeatureConfig, TFeatureImpl : Any> {

    /**
     * A key used to uniquely identify a feature of type [TFeatureImpl] within the local agent storage.
     */
    public val key: AIAgentStorageKey<TFeatureImpl>

    /**
     * Creates and returns an initial configuration for the feature.
     *
     * @param agentConfig The config of the agent this feature config is being created for.
     */
    public fun createInitialConfig(
        agentConfig: AIAgentConfig,
    ): TConfig
}

/**
 * Represents a graph-specific AI agent feature that can be installed into an [AIAgentGraphPipeline].
 *
 * @param TConfig The type of configuration required for the feature, extending [FeatureConfig].
 * @param TFeatureImpl The type representing the concrete implementation of the feature.
 */
public interface AIAgentGraphFeature<TConfig : FeatureConfig, TFeatureImpl : Any> : AIAgentFeature<TConfig, TFeatureImpl> {
    /**
     * Installs the feature into the specified [pipeline].
     * @return The implementation of the feature.
     */
    public fun install(config: TConfig, pipeline: AIAgentGraphPipeline): TFeatureImpl
}

/**
 * Represents a functional-specific AI agent feature that can be installed into an [AIAgentFunctionalPipeline].
 *

--- for ---
// File: AIAgentFeature.kt
nfig, TFeatureImpl : Any> : AIAgentFeature<TConfig, TFeatureImpl> {
    /**
     * Installs the feature into the specified [pipeline].
     * @return The implementation of the feature.
     */
    public fun install(config: TConfig, pipeline: AIAgentGraphPipeline): TFeatureImpl
}

/**
 * Represents a functional-specific AI agent feature that can be installed into an [AIAgentFunctionalPipeline].
 *
 * @param TConfig The type of configuration required for the feature, extending [FeatureConfig].
 * @param TFeatureImpl The type representing the concrete implementation of the feature.
 */
public interface AIAgentFunctionalFeature<TConfig : FeatureConfig, TFeatureImpl : Any> : AIAgentFeature<TConfig, TFeatureImpl> {
    /**
     * Installs the feature into the specified [pipeline].
     * @return The implementation of the feature.
     */
    public fun install(config: TConfig, pipeline: AIAgentFunctionalPipeline): TFeatureImpl
}

/**
 * Represents a planner-specific AI agent feature that can be installed into an [ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline].
 *
 * @param TConfig The type of configuration required for the feature, extending [FeatureConfig].
 * @param TFeatureImpl The type representing the concrete implementation of the feature.
 */
public interface AIAgentPlannerFeature<TConfig : FeatureConfig, TFeatureImpl : Any> : AIAgentFeature<TConfig, TFeatureImpl> {
    /**
     * Installs the feature into the specified [pipeline].
     * @return The implementation of the feature.
     */
    public fun install(config: TConfig, pipeline: AIAgentPlannerPipeline): TFeatureImpl
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentService
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentService.kt
Score: 0,750
Declarations: for

--- for ---
// File: AIAgentService.kt
 for managing and interacting with AI agents.
 *
 * The class maintains a list of AI agents under its management and defines
 * common operations, such as creation, removal, querying, and shutting down agents.
 * Concrete implementations are expected to define the behavior for creating managed agents.
 *
 * @param Input the input type expected by the AI agents.
 * @param Output the output type expected from the AI agents.
 */
public abstract class AIAgentServiceBase<Input, Output, TAgent : AIAgent<Input, Output>> :
    AIAgentService<Input, Output, TAgent>() {
    /**
     * A mutable map that holds the agents currently managed by the AIAgentServiceBase instance.
     *
     * This list is used to track all agents created and managed by the service, allowing for operations
     * such as addition, removal, and querying of agents based on their state or lifecycle.
     */
    private val managedAgents: MutableMap<String, TAgent> = mutableMapOf()
    private val managedAgentsMutex = Mutex()

    override suspend fun createAgentAndRun(
        agentInput: Input,
        id: String?,
        additionalToolRegistry: ToolRegistry,
        agentConfig: AIAgentConfig,
        clock: Clock
    ): Output = createAgent(id, additionalToolRegistry, agentConfig, clock).run(agentInput, null)

    /**
     * Creates and registers a new managed AI agent with the specified configuration and tool registry.
     *
     * @param id An optional unique identifier for the AI agent. If null, a default identifier will be generated.
     * @param additionalToolRegistry A tool registry with additional tools available to the AI agent.
     * @param agentConfig The configuration for the AI agent, including settings for its behavior and capabilities.
     * @param clock The clock instance used for managing time-related operations. Defaults to the system clock.
     * @return A new instance of `AIAgent` initialized with the specified parameters.
     */
    @InternalAgentsApi
    public abstract f

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentBase
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt
Score: 0,700
Declarations: representing

--- representing ---
// File: AIAgentBase.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(InternalAgentsApi::class)

package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.agent.session.AIAgentRunSession
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import io.github.oshai.kotlinlogging.KLogger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abstract base class representing a single-use AI agent with state.
 *
 * This AI agent is designed to execute a specific long-running strategy only once, and provides API to monitor and manage it's state.
 *
 * It maintains internal states including its running status, whether it was started, its result (if available), and
 * the root context associated with its execution. The class enforces safe state transitions and provides
 * thread-safe operations via a mutex.
 *
 * @param Input the type of the input accepted by the agent.
 * @param Output the type of the output produced by the agent.
 * @param TContext the type of the context used during the agent's execution, extending [AIAgentContext].
 * @property logger the logger used for logging execution details and errors.
 * @param id the unique identifier for the agent. Random UUID will be generated if set to null.
 */
public abstract class AIAgentBase<Input, Output, TContext : AIAgentContext> constructor(
    logger: KLogger,
    id: String? = null,
) : AIAgent<Input, Output>() {
    /**
     * Logger instance used for logging messages and events specific to this agent.
     */
    internal open val logger: KLogger = logger

    @OptIn(ExperimentalUuidApi::class)
    final override val id: String by lazy { id ?: Uuid.random().toString() }

    /**
     * The execution strategy defining how the agent processes input and produces output.
     */
    public abstract val strategy: AIAgentS

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentLifecycleHandlersCollector
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
Score: 0,650
Declarations: serves

--- serves ---
// File: AgentLifecycleHandlersCollector.kt
package ai.koog.agents.core.feature.handler

import ai.koog.agents.core.agent.entity.AIAgentStorageKey

/**
 * Collects and manages lifecycle event handlers associated with AI agents and features.
 *
 * This class serves as a centralized registry for associating handlers with specific
 * lifecycle event types and their corresponding features. Handlers are grouped by the
 * associated feature key and further categorized by the event type they handle.
 */
internal class AgentLifecycleHandlersCollector {

    /**
     * The internal class maintains a mapping between event types and their corresponding handlers, enabling
     * the addition and retrieval of event handlers for different agent lifecycle events.
     *
     * @property featureKey The key representing the feature associated with these event handlers.
     */
    private class FeatureEventHandlers(
        val featureKey: AIAgentStorageKey<*>
    ) {
        private val handlersByEventType = mutableMapOf<AgentLifecycleEventType, MutableList<AgentLifecycleEventHandler<*, *>>>()

        fun <TContext : AgentLifecycleEventContext, TReturn : Any> addHandler(
            eventType: AgentLifecycleEventType,
            handler: AgentLifecycleEventHandler<TContext, TReturn>
        ) {
            handlersByEventType.getOrPut(eventType) { mutableListOf() }
                .add(handler)
        }

        fun <TContext : AgentLifecycleEventContext, TReturn : Any> getHandlers(
            eventType: AgentLifecycleEventType
        ): List<AgentLifecycleEventHandler<TContext, TReturn>> {
            return handlersByEventType[eventType]?.mapNotNull { handler ->
                @Suppress("UNCHECKED_CAST")
                handler as? AgentLifecycleEventHandler<TContext, TReturn>
            } ?: emptyList()
        }
    }

    private val featureToHandlersMap = mutableMapOf<AIAgentStorageKey<*>, FeatureEventHandlers>()

    internal fun <TContext : AgentLifecycleEventContext, TReturn : Any> addHandlerForFeature(



---
## Query 2: "как конфигурируется агент"
**Optimized:** "how-to-configure-agent"
**Metrics:** Retrieved: 10 → Filtered: 10 → Reranked: 10 → Final: 5
**Timings:** query_optimize=147ms, retrieve=111ms, filter=1ms, rerank=1916ms, top_k=0ms, pack=0ms
**Top score:** 0,98 | Avg score: 0,94

### RAG Context:
Found 4 relevant class(es) | ~9832 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] FEATURES.md
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md
Score: 0,980
Declarations: to

--- to ---
// File: FEATURES.md
# AIAgent Features

This document describes how to use and implement custom features for AIAgent.

## Table of Contents

- [Introduction](#introduction)
- [Installing Features](#installing-features)
    - [Using FeatureMessageProcessor](#using-featuremessageprocessor)
- [Message Processors](#message-processors)
    - [Using FeatureMessageLogWriter](#using-featuremessagelogwriter)
    - [Using FeatureMessageFileWriter](#using-featuremessagefilewriter)
    - [Using FeatureMessageRemoteWriter](#using-featuremessageremotewriter)
- [Configuring Features](#configuring-features)
- [Implementing Custom Features](#implementing-custom-features)
    - [Basic Feature Structure](#basic-feature-structure)
    - [Pipeline Interceptors](#pipeline-interceptors)
    - [Advanced Interceptors](#advanced-interceptors)
- [Available Features](#available-features)
    - [Debugger](#debugger)

## Introduction

AIAgent features provide a way to extend and enhance the functionality of AI agents. Features can:

- Add new capabilities to agents
- Intercept and modify agent behavior
- Provide access to external systems and resources
- Log and monitor agent execution

Features are designed to be modular and composable, allowing you to mix and match them according to your needs.

## Installing Features

Features are installed when creating a AIAgent instance using the `install` method in the agent constructor:

```kotlin
val agent = AIAgent(
    localEngine = localEngine,
    toolRegistry = toolRegistry,
    strategy = strategy,
    agentConfig = agentConfig
) {
    // Install features here
    install(MyFeature) {
        // Configure the feature
        someProperty = "value"
    }

    install(AnotherFeature) {
        // Configure another feature
        anotherProperty = 42
    }

    // Install a feature with FeatureMessageProcessor
    install(TraceFeature) {
        // Configure the feature
        someProperty = "value"
        // Add message processor
        addMessageProcessor(myFeatur

--- to ---
// File: FEATURES.md
Feature) {
        // Configure the feature
        someProperty = "value"
    }

    install(AnotherFeature) {
        // Configure another feature
        anotherProperty = 42
    }

    // Install a feature with FeatureMessageProcessor
    install(TraceFeature) {
        // Configure the feature
        someProperty = "value"
        // Add message processor
        addMessageProcessor(myFeatureMessageProcessor)
    }
}
```

## Filtering agent events with setEventFilter

In addition to per-processor message filtering, you can filter which agent events a feature will handle using FeatureConfig.setEventFilter. This filter works for any feature and is evaluated before events are passed to any FeatureMessageProcessor.

Key points:
- The predicate receives an EventHandlerContext and must return true to let the event be handled; false will skip it.
- EventHandlerContext exposes eventType and has useful subtypes you can match on (e.g., LLMEventHandlerContext, NodeEventHandlerContext, ToolEventHandlerContext, StrategyEventHandlerContext).
- If you do not set a filter, all events are allowed (default behavior).
- You can change the filter at runtime by calling setEventFilter again; the new predicate is applied to subsequent events.
- This event-level filter composes with per-processor setMessageFilter. Both must allow the item for it to be processed and emitted.

### Disabling event filtering for a feature

Some features rely on receiving the complete sequence of agent lifecycle events to function correctly. For such features, the `setEventFilter` method can be overridden in the feature's configuration class to prevent event filtering.

To disable event filtering for a custom feature:

```kotlin

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] FunctionalAIAgent
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt
Score: 0,950
Declarations: FunctionalAIAgent

--- FunctionalAIAgent ---
// File: FunctionalAIAgent.kt
        /**
         * Installs and configures a feature into the current AI agent context.
         *
         * @param feature the feature to be added, defined by an implementation of [AIAgentFeature], which provides specific functionality
         * @param configure an optional lambda to customize the configuration of the feature, where the provided [Config] can be modified
         */
        public fun <Config : FeatureConfig, Feature : Any> install(
            feature: AIAgentFunctionalFeature<Config, Feature>,
            configure: Config.() -> Unit = {}
        ) {
            agent.pipeline.install(feature, configure)
        }
    }

    init {
        FeatureContext(this).installFeatures()
    }

    override suspend fun prepareContext(agentInput: Input, runId: String, eventId: String): AIAgentFunctionalContext {
        val environment = GenericAgentEnvironment(
            agentId = id,
            logger = logger,
            toolRegistry = toolRegistry,
            serializer = agentConfig.serializer,
        )

        val initialLLMContext = AIAgentLLMContext(
            tools = toolRegistry.tools.map { it.descriptor },
            toolRegistry = toolRegistry,
            prompt = agentConfig.prompt,
            model = agentConfig.model,
            responseProcessor = agentConfig.responseProcessor,
            promptExecutor = promptExecutor,
            environment = environment,
            config = agentConfig,
            clock = clock
        )

        val executionInfo = AgentExecutionInfo(parent = null, partName = id)
        val preparedEnvironment = prepareEnvironment()

        // Context
        val initialAgentContext = AIAgentFunctionalContext(
            environment = preparedEnvironment,
            agentId = id,
            runId = runId,
            agentInput = agentInput,
            config = agentConfig,
            llm = initialLLMContext,
            stateManager = AIAgentStateManager(),
            storage = AIAgentStor

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilder.kt
Score: 0,920
Declarations: AIAgentBuilder

--- AIAgentBuilder ---
// File: AIAgentBuilder.kt
vided [TConfig] can be modified.
     * @return the current [FunctionalAgentBuilder] instance for chaining further configurations.
     */
    public fun <TConfig : FeatureConfig> install(
        feature: AIAgentFunctionalFeature<TConfig, *>,
        configure: ConfigureAction<TConfig>
    ): FunctionalAgentBuilder<Input, Output> = apply {
        this.featureInstallers += {
            install(feature) {
                configure.configure(this)
            }
        }
    }

    /**
     * Builds and returns an instance of `AIAgent<Input, Output>` based on the current configuration
     * of the `FunctionalAgentBuilder`. This method ensures that all required fields are set,
     * and applies any configured feature installers to the agent.
     *
     * @return an instance of `AIAgent<Input, Output>` created using the provided configuration.
     * @throws IllegalArgumentException if required fields, such as `promptExecutor` or `llmModel`, are not set.
     */
    public fun build(): AIAgent<Input, Output> {
        return FunctionalAIAgent(
            strategy = strategy,
            promptExecutor = requireNotNull(promptExecutor) { "promptExecutor must be set" },
            toolRegistry = toolRegistry,
            id = id,
            agentConfig = validateConfig(config),
            clock = clock
        ) {
            featureInstallers.forEach { install ->
                install()
            }
        }
    }
}

/**
 * Builds an AI-based planning agent by configuring various parameters and defining custom behaviors
 * for the agent. This builder allows flexible setup of an agent's functionality and behavior
 * based on the provided configuration and tools.
 *
 * @param State The type representing the state handled by the AI agent.
 * @param strategy The planning strategy used by the agent to process and execute tasks.
 * @param promptExecutor The executor responsible for handling AI prompts.
 * @param toolRegistry The registry of tools available for use

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] PlannerAIAgent
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt
Score: 0,900
Declarations: PlannerAIAgent

--- PlannerAIAgent ---
// File: PlannerAIAgent.kt
  */
    public class FeatureContext internal constructor(private val agent: PlannerAIAgent<*, *>) {
        /**
         * Installs and configures a feature into the current AI agent context.
         *
         * @param feature the feature to be added, defined by an implementation of [AIAgentFeature], which provides specific functionality
         * @param configure an optional lambda to customize the configuration of the feature, where the provided [Config] can be modified
         */
        public fun <Config : FeatureConfig, Feature : Any> install(
            feature: AIAgentPlannerFeature<Config, Feature>,
            configure: Config.() -> Unit = {}
        ) {
            agent.pipeline.install(feature, configure)
        }
    }

    init {
        FeatureContext(this).installFeatures()
    }

    override suspend fun prepareContext(agentInput: Input, runId: String, eventId: String): AIAgentPlannerContext {
        val environment = GenericAgentEnvironment(
            agentId = this.id,
            logger = logger,
            toolRegistry = toolRegistry,
            serializer = agentConfig.serializer,
        )

        val initialLLMContext = AIAgentLLMContext(
            tools = toolRegistry.tools.map { it.descriptor },
            toolRegistry = toolRegistry,
            prompt = agentConfig.prompt,
            model = agentConfig.model,
            responseProcessor = agentConfig.responseProcessor,
            promptExecutor = promptExecutor,
            environment = environment,
            config = agentConfig,
            clock = clock
        )

        val executionInfo = AgentExecutionInfo(parent = null, partName = id)

        // Context
        val initialAgentContext = AIAgentPlannerContext(
            environment = environment,
            agentId = id,
            runId = runId,
            agentInput = agentInput,
            config = agentConfig,
            llm = initialLLMContext,
            stateManager = AIAgentStateManager(),



---
## Query 3: "как агент работает с историей сообщений"
**Optimized:** "kotlin agent message history management"
**Metrics:** Retrieved: 13 → Filtered: 13 → Reranked: 13 → Final: 5
**Timings:** query_optimize=133ms, retrieve=43ms, filter=0ms, rerank=2567ms, top_k=0ms, pack=0ms
**Top score:** 0,95 | Avg score: 0,91

### RAG Context:
Found 4 relevant class(es) | ~9524 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentContextData
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AgentContextData.kt
Score: 0,950
Declarations: AgentContextData

--- AgentContextData ---
// File: AgentContextData.kt
@file:Suppress("MissingKDocForPublicAPI")

package ai.koog.agents.core.agent.context

import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.prompt.message.Message
import ai.koog.serialization.JSONElement

@InternalAgentsApi
public class AgentContextData(
    internal val messageHistory: List<Message>,
    internal val nodePath: String,
    @Deprecated("Use lastOutput instead, lastOutput will be removed in future versions")
    internal val lastInput: JSONElement? = null,
    internal val lastOutput: JSONElement? = null,
    internal val rollbackStrategy: RollbackStrategy,
    internal val additionalRollbackActions: suspend (AIAgentContext) -> Unit = {}
) {
    init {
        require(lastInput == null || lastOutput == null) { "`lastInput` and `lastOutput` cannot be both set" }
        require(lastInput != null || lastOutput != null) { "`lastInput` (until 0.6.0) or `lastOutput` (since 0.6.1) must be set" }
    }
}

public enum class RollbackStrategy {
    /**
     * Rollback state of the agent to the last saved state in full.
     * Meaning restore the entire context, including message history and any other stateful data.
     */
    Default,

    /**
     * Rollback only the message history to the last saved state.
     * Agent starts from the first node with saved message history.
     */
    MessageHistoryOnly,
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SingleRunStrategyWithHistoryCompression
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/SingleRunStrategyWithHistoryCompression.kt
Score: 0,950
Declarations: HistoryCompressionConfig

--- HistoryCompressionConfig ---
// File: SingleRunStrategyWithHistoryCompression.kt
onal [LLModel] to use for compression (defaults to agent's model)
 */
public data class HistoryCompressionConfig(
    val isHistoryTooBig: (Prompt) -> Boolean,
    val compressionStrategy: HistoryCompressionStrategy,
    val retrievalModel: LLModel? = null
)

/**
 * Creates a single-run agent strategy with automatic conversation history compression.
 *
 * Works like [ai.koog.agents.core.agent.singleRunStrategy] but adds a compression step after each tool execution:
 * if the conversation history becomes too large (based on [HistoryCompressionConfig.isHistoryTooBig]),
 * it compresses the message list to essential facts before continuing.
 *
 * @param config specifies when to trigger compression (size threshold), how to compress
 *   (fact extraction strategy), and optionally which model to use for compression
 * @param runMode how tools are executed: [ToolCalls.SINGLE_RUN_SEQUENTIAL] (one tool per LLM call),
 *   [ToolCalls.SEQUENTIAL] (multiple tools per call, executed sequentially), or [ToolCalls.PARALLEL]
 *   (multiple tools per call, executed concurrently)
 * @return [AIAgentGraphStrategy] that compresses conversation history when needed
 */
public fun singleRunStrategyWithHistoryCompression(
    config: HistoryCompressionConfig,
    runMode: ToolCalls = ToolCalls.SINGLE_RUN_SEQUENTIAL
): AIAgentGraphStrategy<String, String> =
    when (runMode) {
        ToolCalls.SEQUENTIAL -> singleRunWithHistoryCompressionParallelAbility(config, false)
        ToolCalls.PARALLEL -> singleRunWithHistoryCompressionParallelAbility(config, true)
        ToolCalls.SINGLE_RUN_SEQUENTIAL -> singleRunWithHistoryCompressionModeStrategy(config)
    }

private fun singleRunWithHistoryCompressionParallelAbility(
    config: HistoryCompressionConfig,
    parallelTools: Boolean
) = strategy("single_run_with_history_compression_sequential") {
    val nodeCallLLM by nodeLLMRequestMultiple()
    val nodeExecuteTool by nodeExecuteMultipleTools(parallelTools = parallelTools)
    val nodeSendT

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] HistoryCompressionStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
Score: 0,900
Declarations: HistoryCompressionStrategy

--- HistoryCompressionStrategy ---
// File: HistoryCompressionStrategy.kt
mpt suitable for language model interactions.
         *
         * This strategy preserves all system messages as well as the first user message
         * (if presented) and memory messages (if provided) and then appends
         * tldr of the whole original history (except trailing tool calls).
         *
         * [System, User, Assistant, ToolCall1, ToolResult, ToolCall2]
         * ->
         * [System, User, Memory, TLDR(System, User, Assistant, ToolCall1, ToolResult)]
         */
        @JvmField
        public val WholeHistory: HistoryCompressionStrategy = WholeHistoryCompressionStrategy

        /**
         * [WholeHistoryMultipleSystemMessages] is a concrete implementation of the [HistoryCompressionStrategy]
         * that handles scenarios where the conversation history contains multiple system messages.
         *
         * This strategy:
         * 1. Splits the history into blocks based on system message boundaries
         * 2. Processes each block separately to generate TL;DR summaries
         * 3. Maintains the chronological order of system messages while compressing the conversation
         * 4. Preserves memory messages only in the first block to maintain context
         *
         * [System1, User1, Assistant, ToolCall, ToolResult, System2, User2, Assistant, User3, System3, Assistant, System4 ]
         * ->
         * [System1, User1, Memory, TLDR(System1, User1, Assistant, ToolCall, ToolResult),
         * System2, User2, TLDR(System2, User2, Assistant, User3),
         * System3, Assistant, TLDR(System3, Assistant)
         * System4, TLDR(System4)]
         */
        @JvmField
        public val WholeHistoryMultipleSystemMessages: HistoryCompressionStrategy =
            WholeCompressionStrategyWithMultipleSystemMessages

        /**
         * A strategy for compressing history by retaining only the last `n` messages in a session.
         *
         * This class removes all but the last `n` messages from the current prompt histor

--- HistoryCompressionStrategy ---
// File: HistoryCompressionStrategy.kt
ystem4)]
         */
        @JvmField
        public val WholeHistoryMultipleSystemMessages: HistoryCompressionStrategy =
            WholeCompressionStrategyWithMultipleSystemMessages

        /**
         * A strategy for compressing history by retaining only the last `n` messages in a session.
         *
         * This class removes all but the last `n` messages from the current prompt history and then
         * compresses the retained messages into a summary (TL;DR). It also allows integration of
         * specific memory messages back into the prompt if needed.
         *
         * @param n The number of most recent messages to retain during compression.
         */
        @JvmStatic
        @KtLintIgnoreNaming
        public fun FromLastNMessages(n: Int): HistoryCompressionStrategy =
            FromLastNMessagesHistoryCompressionStrategy(n)

        /**
         * A strategy for compressing message histories using a specified timestamp as a reference point.
         * This strategy removes messages that occurred before a given timestamp and creates a summarized
         * context for further interactions.
         *
         * This strategy preserves all system messages as well as the first user message
         * (if presented) and memory messages (if provided) and then appends
         * tldr of the subset of messages starting from the provided timestamp (except trailing tool calls).
         *
         * @param timestamp The timestamp indicating the earliest point to retain messages from.
         */
        @JvmStatic
        @KtLintIgnoreNaming
        public fun FromTimestamp(timestamp: Instant): HistoryCompressionStrategy =
            FromTimestampHistoryCompressionStrategy(timestamp)

        /**
         * A concrete implementation of the `HistoryCompressionStrategy` that splits the session's prompt
         * into chunks of a predefined size and generates summaries (TL;DR) for each chunk.
         *
         * This strategy preserves all syst

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ChatMemory
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
Score: 0,850
Declarations: ChatMemory

--- ChatMemory ---
// File: ChatMemory.kt
package ai.koog.agents.chatMemory.feature

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.feature.AIAgentFunctionalFeature
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.AIAgentPlannerFeature
import ai.koog.agents.core.feature.pipeline.AIAgentFunctionalPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline
import ai.koog.prompt.message.Message

/**
 * A feature that allows storing and loading conversation history between an agent and a user.
 *
 * ChatMemory enables agents to persist and retrieve past conversations, allowing for
 * continuity across multiple agent sessions.
 *
 * Example usage:
 * ```kotlin
 * val agent = AIAgent(...) {
 *     installChatMemory {
 *         chatHistoryProvider = MyChatHistoryProvider()
 *     }
 * }
 * ```
 *
 * Example with a sliding window to limit the number of stored messages:
 * ```kotlin
 * val agent = AIAgent(...) {
 *     installChatMemory {
 *         chatHistoryProvider = MyChatHistoryProvider()
 *         windowSize(20) // keep only the last 20 messages
 *     }
 * }
 * ```
 */
public class ChatMemory {

    /**
     * Companion object implementing agent feature, handling [ChatMemory] creation and installation.
     */
    public companion object Feature :
        AIAgentGraphFeature<ChatMemoryConfig, ChatMemory>,
        AIAgentFunctionalFeature<ChatMemoryConfig, ChatMemory>,
        AIAgentPlannerFeature<ChatMemoryConfig, ChatMemory> {

        override val key: AIAgentStorageKey<ChatMemory> =
            AIAgentStorageKey("agents-features-chat-memory")

        override fun createInitialConfig(
            agentConfig: AIAgentConfig,
        ): ChatMemoryConfig = ChatMemoryConfig()

        override fun install(
            config: ChatMemoryC



---
## Query 4: "как реализованы стратегии компактизации контекста"
**Optimized:** "how context compact strategies are implemented in kotlin codebase"
**Metrics:** Retrieved: 11 → Filtered: 11 → Reranked: 11 → Final: 5
**Timings:** query_optimize=159ms, retrieve=63ms, filter=0ms, rerank=2050ms, top_k=0ms, pack=0ms
**Top score:** 0,95 | Avg score: 0,87

### RAG Context:
Found 2 relevant class(es) | ~10024 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] HistoryCompressionStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
Score: 0,950
Declarations: HistoryCompressionStrategy

--- HistoryCompressionStrategy ---
// File: HistoryCompressionStrategy.kt
package ai.koog.agents.core.dsl.extension

import ai.koog.agents.annotations.KtLintIgnoreNaming
import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.prompt.Prompts.summarizeInTLDR
import ai.koog.prompt.message.Message
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.time.Instant

/**
 * Represents an abstract strategy for compressing the history of messages in a `AIAgentLLMWriteSession`.
 * Different implementations define specific approaches to reducing the context size while maintaining key information.
 *
 * Example implementations:
 * - [HistoryCompressionStrategy.WholeHistory]
 * - [HistoryCompressionStrategy.FromLastNMessages]
 * - [HistoryCompressionStrategy.FromTimestamp]
 * - [HistoryCompressionStrategy.Chunked]
 * - [ai.koog.agents.memory.feature.history.RetrieveFactsFromHistory]
 */
public abstract class HistoryCompressionStrategy {
    /**
     * Compresses a given collection of memory messages using a specified strategy.
     *
     * @param llmSession The current LLM session used for processing during compression.
     * @param memoryMessages A list of messages representing the memory to be compressed.
     */
    public abstract suspend fun compress(
        llmSession: AIAgentLLMWriteSession,
        memoryMessages: List<Message>
    )

    /**
     * Compresses the current conversation prompt into a concise "TL;DR" summary using the specified
     * AIAgentLLMWriteSession. The resulting summary will encapsulate the key details and context of the conversation
     * for further processing or continuation.
     *
     * @param llmSession The session used to interact with the language model, providing functionality to update the prompt
     *                   and request a response without utilizing external tools.
     * @return A list of language model responses containing the summarized "TL;DR" of the conversation.
     */
    protected suspend fun compressPromptIntoTLDR(llmSession: AIAgentL

--- HistoryCompressionStrategy ---
// File: HistoryCompressionStrategy.kt
mpt suitable for language model interactions.
         *
         * This strategy preserves all system messages as well as the first user message
         * (if presented) and memory messages (if provided) and then appends
         * tldr of the whole original history (except trailing tool calls).
         *
         * [System, User, Assistant, ToolCall1, ToolResult, ToolCall2]
         * ->
         * [System, User, Memory, TLDR(System, User, Assistant, ToolCall1, ToolResult)]
         */
        @JvmField
        public val WholeHistory: HistoryCompressionStrategy = WholeHistoryCompressionStrategy

        /**
         * [WholeHistoryMultipleSystemMessages] is a concrete implementation of the [HistoryCompressionStrategy]
         * that handles scenarios where the conversation history contains multiple system messages.
         *
         * This strategy:
         * 1. Splits the history into blocks based on system message boundaries
         * 2. Processes each block separately to generate TL;DR summaries
         * 3. Maintains the chronological order of system messages while compressing the conversation
         * 4. Preserves memory messages only in the first block to maintain context
         *
         * [System1, User1, Assistant, ToolCall, ToolResult, System2, User2, Assistant, User3, System3, Assistant, System4 ]
         * ->
         * [System1, User1, Memory, TLDR(System1, User1, Assistant, ToolCall, ToolResult),
         * System2, User2, TLDR(System2, User2, Assistant, User3),
         * System3, Assistant, TLDR(System3, Assistant)
         * System4, TLDR(System4)]
         */
        @JvmField
        public val WholeHistoryMultipleSystemMessages: HistoryCompressionStrategy =
            WholeCompressionStrategyWithMultipleSystemMessages

        /**
         * A strategy for compressing history by retaining only the last `n` messages in a session.
         *
         * This class removes all but the last `n` messages from the current prompt histor

--- HistoryCompressionStrategy ---
// File: HistoryCompressionStrategy.kt
ystem4)]
         */
        @JvmField
        public val WholeHistoryMultipleSystemMessages: HistoryCompressionStrategy =
            WholeCompressionStrategyWithMultipleSystemMessages

        /**
         * A strategy for compressing history by retaining only the last `n` messages in a session.
         *
         * This class removes all but the last `n` messages from the current prompt history and then
         * compresses the retained messages into a summary (TL;DR). It also allows integration of
         * specific memory messages back into the prompt if needed.
         *
         * @param n The number of most recent messages to retain during compression.
         */
        @JvmStatic
        @KtLintIgnoreNaming
        public fun FromLastNMessages(n: Int): HistoryCompressionStrategy =
            FromLastNMessagesHistoryCompressionStrategy(n)

        /**
         * A strategy for compressing message histories using a specified timestamp as a reference point.
         * This strategy removes messages that occurred before a given timestamp and creates a summarized
         * context for further interactions.
         *
         * This strategy preserves all system messages as well as the first user message
         * (if presented) and memory messages (if provided) and then appends
         * tldr of the subset of messages starting from the provided timestamp (except trailing tool calls).
         *
         * @param timestamp The timestamp indicating the earliest point to retain messages from.
         */
        @JvmStatic
        @KtLintIgnoreNaming
        public fun FromTimestamp(timestamp: Instant): HistoryCompressionStrategy =
            FromTimestampHistoryCompressionStrategy(timestamp)

        /**
         * A concrete implementation of the `HistoryCompressionStrategy` that splits the session's prompt
         * into chunks of a predefined size and generates summaries (TL;DR) for each chunk.
         *
         * This strategy preserves all syst

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] StrategyEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/strategy/StrategyEventContext.kt
Score: 0,850
Declarations: StrategyEventContext

--- StrategyEventContext ---
// File: StrategyEventContext.kt
package ai.koog.agents.core.feature.handler.strategy

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.feature.handler.AgentLifecycleEventContext
import ai.koog.agents.core.feature.handler.AgentLifecycleEventType
import ai.koog.serialization.TypeToken

/**
 * Defines the context specifically for handling strategy-related events within the AI agent framework.
 * Extends the base event handler context to include functionality and behavior dedicated to managing
 * the lifecycle and operations of strategies associated with AI agents.
 */
public interface StrategyEventContext : AgentLifecycleEventContext

/**
 * Represents the context for updating AI agent strategies during execution.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property strategy The strategy being updated, encapsulating the AI agent's workflow logic.
 * @property context The context associated with the strategy's execution.
 */
public class StrategyStartingContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    public val strategy: AIAgentStrategy<*, *, *>,
    public val context: AIAgentContext,
) : StrategyEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.StrategyStarting

    /**
     * The unique identifier for this run.
     * @deprecated Use runId property from a [context] instance instead.
     */
    @Deprecated(
        message = "Scheduled for removal. Please get runId from a context instance instead",
        replaceWith = ReplaceWith("context.runId")
    )
    public val runId: String
        get() = this.context.runId
}

/**
 * Represents the context associated with the completion of an AI agent strategy execution.
 *
 * @property executionInfo The execution information containing parentId and c

--- StrategyEventContext ---
// File: StrategyEventContext.kt
ated(
        message = "Scheduled for removal. Please get runId from a context instance instead",
        replaceWith = ReplaceWith("context.runId")
    )
    public val runId: String
        get() = this.context.runId
}

/**
 * Represents the context associated with the completion of an AI agent strategy execution.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property strategy The strategy being updated, encapsulating the AI agent's workflow logic.
 * @property context The context associated with the strategy's execution.
 * @property result Strategy result.
 * @property resultType [TypeToken] representing the type of the [result]
 */
public class StrategyCompletedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    public val strategy: AIAgentStrategy<*, *, *>,
    public val context: AIAgentContext,
    public val result: Any?,
    public val resultType: TypeToken,
) : StrategyEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.StrategyCompleted

    /**
     * The unique identifier for this run.
     * @deprecated Use runId property from a [context] instance instead.
     */
    @Deprecated(
        message = "Scheduled for removal. Please get runId from a [context] instance instead",
        replaceWith = ReplaceWith("context.runId")
    )
    public val runId: String
        get() = this.context.runId

    /**
     * The identifier for this agent.
     * @deprecated Use agentId property from a [context] instance instead.
     */
    @Deprecated(
        message = "Scheduled for removal. Please get agentId from a [context] instance instead",
        replaceWith = ReplaceWith("context.agentId")
    )
    public val agentId: String
        get() = this.context.agentId
}



---
## Query 5: "как реализован tool calling"
**Optimized:** "how/tool/calling/implemented/in/kotlin"
**Metrics:** Retrieved: 12 → Filtered: 12 → Reranked: 12 → Final: 5
**Timings:** query_optimize=149ms, retrieve=88ms, filter=0ms, rerank=2416ms, top_k=0ms, pack=0ms
**Top score:** 0,90 | Avg score: 0,85

### RAG Context:
Found 3 relevant class(es) | ~9492 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] McpTool
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpTool.kt
Score: 0,900
Declarations: serves

--- serves ---
// File: McpTool.kt
package ai.koog.agents.mcp

import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONObject
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.kotlinx.toKoogJSONElement
import ai.koog.serialization.kotlinx.toKotlinxJsonElement
import ai.koog.serialization.kotlinx.toKotlinxJsonObject
import ai.koog.serialization.typeToken
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * A Tool implementation that calls an MCP (Model Context Protocol) tool.
 *
 * This class serves as a bridge between the agent framework's Tool interface and the MCP SDK.
 * It allows MCP tools to be used within the agent framework by:
 * 1. Converting agent framework tool arguments to MCP tool arguments
 * 2. Calling the MCP tool through the MCP client
 * 3. Converting MCP tool results back to agent framework tool results
 */
@InternalAgentsApi
public class McpTool(
    private val mcpClient: Client,
    descriptor: ToolDescriptor,
    metadata: Map<String, String>,
) : Tool<JSONObject, CallToolResult?>(
    argsType = typeToken<JSONObject>(),
    resultType = typeToken<CallToolResult?>(),
    descriptor = descriptor,
    metadata = metadata,
) {
    /**
     * MCP SDK uses kotlinx.serialization for JSON serialization, so keep private instance to perform some
     * JSON serialization/deserialization operations.
     */
    private val json = Json.Default
    private val resultSerializer = CallToolResult.serializer().nullable

    /**
     * Executes the MCP tool with the given arguments.

--- serves ---
// File: McpTool.kt
scriptor = descriptor,
    metadata = metadata,
) {
    /**
     * MCP SDK uses kotlinx.serialization for JSON serialization, so keep private instance to perform some
     * JSON serialization/deserialization operations.
     */
    private val json = Json.Default
    private val resultSerializer = CallToolResult.serializer().nullable

    /**
     * Executes the MCP tool with the given arguments.
     *
     * This method calls the MCP tool through the MCP client and converts the result
     * to a Result object that can be used by the agent framework.
     *
     * @param args The arguments for the MCP tool call.
     * @return The result of the MCP tool call.
     */
    override suspend fun execute(args: JSONObject): CallToolResult {
        return mcpClient.callTool(
            name = descriptor.name,
            arguments = args.toKotlinxJsonObject()
        )
    }

    override fun decodeResult(rawResult: JSONElement, serializer: JSONSerializer): CallToolResult? {
        return json.decodeFromJsonElement(resultSerializer, rawResult.toKotlinxJsonElement())
    }

    override fun encodeResult(result: CallToolResult?, serializer: JSONSerializer): JSONElement {
        return json.encodeToJsonElement(resultSerializer, result).toKoogJSONElement()
    }

    /**
     * Postprocess result string representation for LLMs a bit, removing unnecessary meta fields.
     */
    override fun encodeResultToString(result: CallToolResult?, serializer: JSONSerializer): String {
        val preparedResultJson: JsonElement = result
            ?.let {
                JsonObject(
                    json.encodeToJsonElement(resultSerializer, result).jsonObject
                        // LLM doesn't need "meta" fields, leave only actual data
                        .filter { (key, _) -> key !in listOf("type", "_meta") }
                )
            }
            ?: JsonNull

        return serializer.encodeJSONElementToString(preparedResultJson.toKoogJSONElement())
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] toolExecutionEvents
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
Score: 0,850
Declarations: ToolCallStartingEvent

--- ToolCallStartingEvent ---
// File: toolExecutionEvents.kt
.simpleName.toString(),
        ),
        runId = runId,
        toolCallId = toolCallId,
        toolName = toolName,
        toolArgs = toolArgs,
        toolDescription = null,
        message = error,
        error = AIAgentError(error, "", null)
    )
}

/**
 * Captures an event where a tool call has failed during its execution.
 *
 * This event is typically used to log or handle situations where a tool could not execute
 * successfully due to an error. It includes relevant details about the failed tool call,
 * such as the tool's name, the arguments provided, and the specific error encountered.
 *
 * @property eventId A unique identifier for the event or a group of events;
 * @property executionInfo Provides contextual information about the execution associated with this event.
 * @property runId A unique identifier representing the specific run or instance of the tool call;
 * @property toolCallId A unique identifier for the tool call that failed;
 * @property toolName The name of the tool that failed;
 * @property toolArgs The arguments passed to the tool during the failed execution;
 * @property toolDescription A description of the tool that failed;
 * @property error The error encountered during the tool's execution;
 * @property timestamp The timestamp of the event, in milliseconds since the Unix epoch.
 */
@Serializable
public data class ToolCallFailedEvent(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val runId: String,
    val toolCallId: String?,
    val toolName: String,
    val toolArgs: JSONObject,
    val toolDescription: String?,
    val error: AIAgentError?,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) : DefinedFeatureEvent() {

    /**
     * @deprecated Use constructor with [executionInfo] parameter
     */
    @Deprecated(
        message = "Use constructor with executionInfo parameter",
        replaceWith = ReplaceWith("ToolCallFailedEvent(executionInfo, runId,

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolCallEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/tool/ToolCallEventContext.kt
Score: 0,850
Declarations: ToolCallEventContext

--- ToolCallEventContext ---
// File: ToolCallEventContext.kt
gentExecutionInfo,
    override val runId: String,
    override val toolCallId: String?,
    override val toolName: String,
    override val toolDescription: String?,
    override val toolArgs: JSONObject,
    override val context: AIAgentContext
) : ToolCallEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.ToolCallStarting
}

/**
 * Represents the context for handling validation errors that occur during the execution of a tool.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property message A message describing the validation error.
 * @property error The [AIAgentError] error describing the validation issue.
 */
public data class ToolValidationFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    override val runId: String,
    override val toolCallId: String?,
    override val toolName: String,
    override val toolDescription: String?,
    override val toolArgs: JSONObject,
    val message: String,
    val error: AIAgentError,
    override val context: AIAgentContext
) : ToolCallEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.ToolValidationFailed
}

/**
 * Represents the context provided to handle a failure during the execution of a tool.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property message A message describing the failure that occurred.
 * @property error The [AIAgentError] instance describing the tool call failure.
 */
public data class ToolCallFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    override val runId: String,
    override val toolCallId: String?,
    override val toolName: String,
    override val toolDescription: String?,
    override val toolArgs: JSONObject,
    val message: String,
    val error: AIAgentError?,
    override

--- ToolCallEventContext ---
// File: ToolCallEventContext.kt
call failure.
 */
public data class ToolCallFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    override val runId: String,
    override val toolCallId: String?,
    override val toolName: String,
    override val toolDescription: String?,
    override val toolArgs: JSONObject,
    val message: String,
    val error: AIAgentError?,
    override val context: AIAgentContext
) : ToolCallEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.ToolCallFailed
}

/**
 * Represents the context used when handling the result of a tool call.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property toolResult An optional result produced by the tool after execution can be null if not applicable.
 */
public data class ToolCallCompletedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    override val runId: String,
    override val toolCallId: String?,
    override val toolName: String,
    override val toolDescription: String?,
    override val toolArgs: JSONObject,
    val toolResult: JSONElement?,
    override val context: AIAgentContext
) : ToolCallEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.ToolCallCompleted
}



---
## Summary
| Метрика | Значение |
|---------|---------|
| Всего вопросов | 5 |
| Avg top score | 0,93 |
| retrievalTopK | 15 |
| threshold | 0.5 |
| rerankStrategy | LLM |
| finalTopK | 5 |
