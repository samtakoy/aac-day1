# Evaluation: RERANKED_HEURISTIC | 2026-03-19_09-56
Config: retrievalTopK=15, threshold=0.5, rerank=HEURISTIC, finalTopK=5

---
## Query 1: "какие основные возможности агента, какой класс реализует"
**Optimized:** "what are the main agent capabilities, which class implements"
**Metrics:** Retrieved: 10 → Filtered: 10 → Reranked: 10 → Final: 5
**Timings:** query_optimize=663ms, retrieve=414ms, filter=0ms, rerank=2ms, top_k=0ms, pack=0ms
**Top score:** 0,70 | Avg score: 0,67

### RAG Context:
Found 5 relevant class(es) | ~8712 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentService
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentService.kt
Score: 0,704
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
[CLASS] AgentLifecycleHandlersCollector
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
Score: 0,701
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt
Score: 0,675
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentRunSessionImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
Score: 0,630
Declarations: handles

--- handles ---
// File: AIAgentRunSessionImpl.kt
@file:OptIn(InternalAgentsApi::class)

package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.AIAgentState.NotStarted
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.with
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.agent.session.AIAgentRunSession
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import ai.koog.agents.core.utils.runCatchingCancellable
import ai.koog.serialization.typeToken
import io.github.oshai.kotlinlogging.KLogger

/**
 * Internal implementation of [AIAgentRunSession] that manages the execution lifecycle of an AI agent.
 *
 * This class handles the complete execution flow of an agent run, including:
 * - State management throughout the agent's lifecycle
 * - Pipeline preparation and cleanup
 * - Strategy execution with proper error handling
 * - Event notifications to the pipeline at each stage
 *
 * The session maintains internal state tracking the progress of the agent execution from
 * [AIAgentState.NotStarted] through [AIAgentState.Starting], [AIAgentState.Running],
 * and finally to either [AIAgentState.Finished] or [AIAgentState.Failed].
 *
 * @param Input the type of input data required by the agent's strategy.
 * @param Output the type of output data produced by the agent's strategy.
 * @param TContext the type of context used during execution, extending [AIAgentContext].
 * @property id the unique identifier of the agent this session belongs to.
 * @property logger the logger instance used for logging execution details and errors.
 * @property agent the AI agent instance being executed in this session.
 * @property strategy the execution strategy that defines how the agent processes input and produces output.
 */
internal class AIAgentRunSessionImpl<Input, Output, TContext : AIAgentContext>(
    private val id: String,
    private val logger: KLogger,
    private val agent:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] InternalAgentToolsApi
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/annotations/InternalAgentToolsApi.kt
Score: 0,625
Declarations: InternalAgentToolsApi

--- InternalAgentToolsApi ---
// File: InternalAgentToolsApi.kt
package ai.koog.agents.core.tools.annotations

/**
 * Marks elements of the agent tools API that are internal and primarily intended for use within
 * the implementation of tools and agents. APIs marked with this annotation are not considered stable
 * and may change or be removed without warning in any release.
 *
 * Opting into these APIs indicates an understanding that they may undergo breaking changes, and should
 * be used with caution in any external implementations or client code.
 */
@RequiresOptIn
public annotation class InternalAgentToolsApi



---
## Query 2: "как конфигурируется агент"
**Optimized:** "how-to-configure-agent"
**Metrics:** Retrieved: 10 → Filtered: 10 → Reranked: 10 → Final: 5
**Timings:** query_optimize=135ms, retrieve=28ms, filter=0ms, rerank=1ms, top_k=0ms, pack=0ms
**Top score:** 0,75 | Avg score: 0,72

### RAG Context:
Found 4 relevant class(es) | ~10120 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] FunctionalAIAgent
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt
Score: 0,748
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
[CLASS] PlannerAIAgent
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt
Score: 0,722
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilder.kt
Score: 0,712
Declarations: AIAgentBuilder

--- AIAgentBuilder ---
// File: AIAgentBuilder.kt
ons,
     * and the strategy for handling missing tools based on the given `AIAgentConfig`.
     *
     * @param config The `AIAgentConfig` instance containing the agent's configuration,
     *               including prompt settings, model, iteration limits, and tool handling strategies.
     * @return The current instance of `GraphAgentBuilder<Input, Output>` for method chaining.
     */
    public fun agentConfig(config: AIAgentConfig): GraphAgentBuilder<Input, Output> = apply {
        this.config = config
    }

    /**
     * Installs a specified feature into the current context and applies its configuration.
     *
     * @param TConfig The type of configuration required by the feature, extending [FeatureConfig].
     * @param feature The feature to install, represented by an implementation of [AIAgentGraphFeature].
     * @param configure A lambda used to customize the configuration of the feature.
     * @return The current [GraphAgentBuilder] instance, enabling further configurations.
     */
    public fun <TConfig : FeatureConfig> install(
        feature: AIAgentGraphFeature<TConfig, *>,
        configure: ConfigureAction<TConfig>
    ): GraphAgentBuilder<Input, Output> = apply {
        this.featureInstallers += {
            install(feature) {
                configure.configure(this)
            }
        }
    }

    /**
     * Builds and returns an instance of `AIAgent` configured using the parameters
     * provided to the `GraphAgentBuilder`.
     *
     * @return an instance of `AIAgent` initialized with the specified input and output types,
     *         strategy, tool registry, prompt executor, model configuration, and other optional settings.
     */
    public fun build(): AIAgent<Input, Output> {
        return GraphAIAgent(
            inputType = inputType,
            outputType = outputType,
            strategy = strategy,
            promptExecutor = requireNotNull(promptExecutor) { "promptExecutor must be set" },
            toolReg

--- AIAgentBuilder ---
// File: AIAgentBuilder.kt
setup of an agent's functionality and behavior
 * based on the provided configuration and tools.
 *
 * @param State The type representing the state handled by the AI agent.
 * @param strategy The planning strategy used by the agent to process and execute tasks.
 * @param promptExecutor The executor responsible for handling AI prompts.
 * @param toolRegistry The registry of tools available for use by the agent. Defaults to an empty tool registry.
 * @param id The optional identifier of the agent.
 * @param config [AIAgentConfig] containing initial agent configuration for the builder
 * @param clock The clock instance used to track time-related operations for the agent. Defaults to the system clock.
 * @param featureInstallers A list of feature installers that enhance the agent's behavior with additional functionality.
 */
public class PlannerAgentBuilder<Input, Output>(
    private val strategy: AIAgentPlannerStrategy<Input, Output, *>,
    private var promptExecutor: PromptExecutor? = null,
    private var toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
    private var id: String? = null,
    private var config: AIAgentConfig,
    private var clock: Clock = Clock.System,
    private var featureInstallers: MutableList<PlannerAIAgent.FeatureContext.() -> Unit> = mutableListOf(),
) {

    /**
     * Sets the `PromptExecutor` instance to be used by the `PlannerAgentBuilder`.
     *
     * @param promptExecutor An instance of `PromptExecutor` that will handle prompt execution logic.
     * @return The current instance of `PlannerAgentBuilder<Input, Output>` for method chaining.
     */
    public fun promptExecutor(promptExecutor: PromptExecutor): PlannerAgentBuilder<Input, Output> = apply {
        this.promptExecutor = promptExecutor
    }

    /**
     * Sets the Large Language Model (LLM) to be used by the `PlannerAgentBuilder`.
     *
     * @param model The instance of [LLModel] representing the Large Language Model to be configured.
     * @return The current ins

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] FEATURES.md
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md
Score: 0,704
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



---
## Query 3: "как агент работает с историей сообщений"
**Optimized:** "kotlin agent message history management"
**Metrics:** Retrieved: 13 → Filtered: 13 → Reranked: 13 → Final: 5
**Timings:** query_optimize=132ms, retrieve=26ms, filter=0ms, rerank=1ms, top_k=0ms, pack=0ms
**Top score:** 0,74 | Avg score: 0,72

### RAG Context:
Found 4 relevant class(es) | ~9508 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentContextData
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AgentContextData.kt
Score: 0,742
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
[CLASS] ChatMemory
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
Score: 0,737
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] HistoryCompressionStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
Score: 0,725
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] JdbcChatHistoryProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-chat-history-jdbc/src/main/kotlin/ai/koog/agents/features/chathistory/jdbc/JdbcChatHistoryProvider.kt
Score: 0,699
Declarations: JdbcChatHistoryProvider

--- JdbcChatHistoryProvider ---
// File: JdbcChatHistoryProvider.kt
package ai.koog.agents.features.chathistory.jdbc

import ai.koog.agents.features.chatmemory.sql.SQLChatHistoryProvider
import ai.koog.agents.features.chatmemory.sql.SQLChatHistorySchemaMigrator
import ai.koog.prompt.message.Message
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.sql.Types
import javax.sql.DataSource
import kotlin.time.Clock

/**
 * Abstract pure JDBC implementation of [SQLChatHistoryProvider] for managing chat conversation
 * history in SQL databases
 *
 * Subclasses provide the database-specific upsert SQL via [upsertSql].
 *
 * @param dataSource The JDBC DataSource for obtaining database connections.
 *   The caller is responsible for managing the DataSource lifecycle
 *   (e.g., closing a connection pool). This provider does not close or otherwise manage the DataSource.
 * @param migrator Schema migrator for creating/updating the table
 * @param tableName Name of the table to store chat history (default: "chat_history")
 * @param ttlSeconds Optional TTL for history entries in seconds (null = no expiration)
 * @param ioDispatcher Coroutine dispatcher for I/O operations (default: [Dispatchers.IO])
 */
public abstract class JdbcChatHistoryProvider @JvmOverloads constructor(
    protected val dataSource: DataSource,
    migrator: SQLChatHistorySchemaMigrator,
    ttlSeconds: Long? = null,
    tableName: String = "chat_history",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SQLChatHistoryProvider(
    tableName = tableName,
    ttlSeconds = ttlSeconds,
    migrator = migrator
) {
    protected open fun serializeMessages(messages: List<Message>): String =
        defaultJson.encodeToString(ListSerializer(Message.serializer()), messages)

    protected open fun deserializeMessages(json: String): List<Message> =
        defaultJson.decodeFromString(ListS



---
## Query 4: "как реализованы стратегии компактизации контекста"
**Optimized:** "how contextual compactification strategies are implemented in kotlin codebase"
**Metrics:** Retrieved: 13 → Filtered: 13 → Reranked: 13 → Final: 5
**Timings:** query_optimize=167ms, retrieve=30ms, filter=0ms, rerank=0ms, top_k=0ms, pack=0ms
**Top score:** 0,70 | Avg score: 0,66

### RAG Context:
Found 5 relevant class(es) | ~9524 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] HistoryCompressionStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
Score: 0,698
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ContextualAgentEnvironment
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/ContextualAgentEnvironment.kt
Score: 0,692
Declarations: acts

--- acts ---
// File: ContextualAgentEnvironment.kt
package ai.koog.agents.core.environment

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.feature.model.toAgentError
import ai.koog.prompt.message.Message
import ai.koog.serialization.JSONObject
import ai.koog.serialization.kotlinx.toKoogJSONObject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents an AI agent environment that operates within the context of a specific agent framework.
 *
 * This class acts as a decorator over an existing [AIAgentEnvironment], augmenting operations with contextual
 * processing using the provided [AIAgentContext].
 *
 * @constructor Constructs a new instance of [ContextualAgentEnvironment] with a decorated [environment] and a
 * contextual [context].
 *
 * @param environment The underlying agent environment responsible for managing tool execution.
 * @param context The context that augments the environment with additional behavioral and execution information.
 */
@InternalAgentsApi
public class ContextualAgentEnvironment(
    private val environment: AIAgentEnvironment,
    private val context: AIAgentContext,
) : AIAgentEnvironment {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun executeTool(toolCall: Message.Tool.Call): ReceivedToolResult {
        @OptIn(ExperimentalUuidApi::class)
        val eventId = Uuid.random().toString()
        val toolDescription = context.llm.toolRegistry.getToolOrNull(toolCall.tool)?.descriptor?.description

        val toolArgs = try {
            toolCall.contentJson.toKoogJSONObject()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error { "Failed to execute tool call with i

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentContextData
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AgentContextData.kt
Score: 0,677
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
[CLASS] AIAgentGraphPipelineImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipelineImpl.kt
Score: 0,612
Declarations: AIAgentGraphPipelineImpl

--- AIAgentGraphPipelineImpl ---
// File: AIAgentGraphPipelineImpl.kt
@file:OptIn(InternalAgentsApi::class)

package ai.koog.agents.core.feature.pipeline

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentSubgraphBase
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.handler.AgentLifecycleEventType
import ai.koog.agents.core.feature.handler.node.NodeExecutionCompletedContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionFailedContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionStartingContext
import ai.koog.agents.core.feature.handler.subgraph.SubgraphExecutionCompletedContext
import ai.koog.agents.core.feature.handler.subgraph.SubgraphExecutionFailedContext
import ai.koog.agents.core.feature.handler.subgraph.SubgraphExecutionStartingContext
import ai.koog.serialization.TypeToken
import kotlin.time.Clock

internal class AIAgentGraphPipelineImpl(
    agentConfig: AIAgentConfig,
    clock: Clock = Clock.System,
    private val basePipelineDelegate: AIAgentPipelineImpl
) : AIAgentGraphPipelineAPI, AIAgentPipelineAPI by basePipelineDelegate {

    //region Trigger Node Handlers

    public override suspend fun onNodeExecutionStarting(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        node: AIAgentNodeBase<*, *>,
        context: AIAgentGraphContextBase,
        input: Any?,
        inputType: TypeToken
    ) {
        basePipelineDelegate.invokeRegisteredHandlersForEvent(
            eventType = AgentLifecycleEventType.NodeExecutionStarting,
            context = NodeExecutionStartingContext(eventId, executionInfo, node, context, input, inputType)
        )
    }

    public override suspend fun onNodeExecutionCompleted(
        eventId: String,
        exec

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] NodeExecutionEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/node/NodeExecutionEventContext.kt
Score: 0,612
Declarations: NodeExecutionEventContext

--- NodeExecutionEventContext ---
// File: NodeExecutionEventContext.kt
package ai.koog.agents.core.feature.handler.node

import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.feature.handler.AgentLifecycleEventContext
import ai.koog.agents.core.feature.handler.AgentLifecycleEventType
import ai.koog.serialization.TypeToken

/**
 * Represents the context for handling node-specific events within the framework.
 */
public interface NodeExecutionEventContext : AgentLifecycleEventContext

/**
 * Represents the context for handling a before node execution event.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property node The node that is about to be executed.
 * @property context The stage context in which the node is being executed.
 * @property input The input data for the node execution.
 * @property inputType [TypeToken] representing the type of the [input].
 */
public data class NodeExecutionStartingContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val node: AIAgentNodeBase<*, *>,
    val context: AIAgentGraphContextBase,
    val input: Any?,
    val inputType: TypeToken,
) : NodeExecutionEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.NodeExecutionStarting
}

/**
 * Represents the context for handling an after node execution event.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property node The node that was executed.
 * @property context The stage context in which the node was executed.
 * @property input The input data that was provided to the node.
 * @property inputType [TypeToken] representing the type of the [input].
 * @property output The output data produced by the node execution.
 * @property outputType [TypeToken] representing the type of the [output].
 */
pu



---
## Query 5: "как реализован tool calling"
**Optimized:** "how-tool-calling-implemented-kotlin-codebase"
**Metrics:** Retrieved: 13 → Filtered: 13 → Reranked: 13 → Final: 5
**Timings:** query_optimize=157ms, retrieve=28ms, filter=0ms, rerank=1ms, top_k=0ms, pack=0ms
**Top score:** 0,78 | Avg score: 0,75

### RAG Context:
Found 5 relevant class(es) | ~10128 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolResult
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolResult.kt
Score: 0,780
Declarations: implemented

--- implemented ---
// File: ToolResult.kt
package ai.koog.agents.core.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmInline

/**
 * Represents a result produced by a tool operation. This is a marker interface implemented by various result types.
 */
@Deprecated("Extending ToolResult is no longer required. Tool results are entirely handled by KotlinX Serialization.")
public interface ToolResult {
    /**
     * Companion object for the enclosing class.
     *
     * Provides utility functionalities, including methods to handle and interact with
     * objects of types implementing the `TextSerializable` interface. It includes support
     * for creating a text-based serializer for the objects using the `AsTextSerializer`
     * class and a pre-configured `Json` instance for serialization with customizable options.
     */
    public companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
            prettyPrint = true
        }
    }

    /**
     * Provides a string representation of the implementing instance with default formatting.
     *
     * @return A string representation of the object.
     */
    public fun toStringDefault(): String

    /**
     * Result implementation representing a simple tool result, just a string.
     */
    @Deprecated(
        "Extending ToolResult.Text is no longer required (just use plain String class instead). " +
            "Tool results are entirely handled by KotlinX Serialization."
    )
    @Serializable
    @JvmInline
    public value class Text(public val text: String) : JSONSerializable<Text> {
        override fun getSerializer(): KSerializer<Text> = serializer()

        /**
         * Constructs a [Text] instance with a message generated from the given exception.
         *
         * The message is built using the exception's class name and its message.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] MockExecutorDSLBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/MockExecutorDSLBuilder.kt
Score: 0,759
Declarations: is

--- is ---
// File: MockExecutorDSLBuilder.kt
package ai.koog.agents.testing.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolResult
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.tokenizer.Tokenizer
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.kotlinx.KotlinxSerializer
import ai.koog.serialization.kotlinx.toKoogJSONObject
import kotlin.jvm.JvmName
import kotlin.time.Clock

/**
 * Represents a condition for a tool call and its corresponding result.
 *
 * This class is used to define how a tool should respond to specific inputs during testing.
 * It encapsulates the tool, a condition to check if the tool call matches, and a function
 * to produce the result when the condition is satisfied.
 *
 * @param Args The type of arguments the tool accepts
 * @param Result The type of result the tool produces
 * @property tool The tool to be mocked
 * @property serializer The JSON serializer to use for encoding and decoding args/results
 * @property argsCondition A function that determines if the tool call matches this condition
 * @property produceResult A function that produces the result when the condition is satisfied
 */
public class ToolCondition<Args, Result>(
    public val tool: Tool<Args, Result>,
    public val serializer: JSONSerializer,
    public val argsCondition: suspend (Args) -> Boolean,
    public val produceResult: suspend (Args) -> Result
) {
    /**
     * Checks if this condition applies to the given tool call.
     *
     * @param toolCall The tool call to check
     * @return True if the tool name matches and the arguments satisfy the condition
     */
    internal suspend fun satisfies(toolCall: Message.Tool.Call) =
        tool.name == toolCall.tool && argsCondition(tool.decodeArgs(toolCall.contentJson.toKoogJSONObject(), serializer))

    /**
     * Invokes the tool with the argument

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ReceivedToolResult
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/ReceivedToolResult.kt
Score: 0,755
Declarations: ReceivedToolResult

--- ReceivedToolResult ---
// File: ReceivedToolResult.kt
package ai.koog.agents.core.environment

import ai.koog.prompt.dsl.PromptBuilder
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONObject
import ai.koog.serialization.kotlinx.toKoogJSONElement
import ai.koog.serialization.kotlinx.toKoogJSONObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

/**
 * Represents the result or response received from a tool operation.
 *
 * @property id An optional identifier for the tool result.
 * @property tool The name or type of the tool that generated the result.
 * @property toolArgs The arguments provided to the tool during execution.
 * @property toolDescription An optional description of the tool's functionality.
 * @property content The main content or message associated with the tool result.
 * @property resultKind The kind of result produced by the tool, indicating success, failure, or validation error.
 * @property result The result produced by the tool.
 */
@Serializable
public data class ReceivedToolResult(
    val id: String?,
    val tool: String,
    val toolArgs: JSONObject,
    val toolDescription: String?,
    val content: String,
    val resultKind: ToolResultKind,
    val result: JSONElement?
) {
    @Deprecated("Use the constructor with JSONElement instead of JsonElement")
    public constructor(
        id: String?,
        tool: String,
        toolArgs: JsonObject,
        toolDescription: String?,
        content: String,
        resultKind: ToolResultKind,
        result: JsonElement?
    ) : this(
        id = id,
        tool = tool,
        toolArgs = toolArgs.toKoogJSONObject(),
        toolDescription = toolDescription,
        content = content,
        resultKind = resultKind,
        result = result?.toKoogJSONElement()
    )

    /**
     * Converts the current `ReceivedToolResult`

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolCallDescriber
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/ToolCallDescriber.kt
Score: 0,731
Declarations: ToolCallDescriber

--- ToolCallDescriber ---
// File: ToolCallDescriber.kt
package ai.koog.agents.core.agent.config

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.Message.Assistant
import ai.koog.prompt.message.Message.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Describes the way to reformat tool call/tool result messages,
 * in case real tool call/tool result messages cannot be used
 */
public interface ToolCallDescriber {
    /**
     * Composes a description of a tool call message.
     *
     * @param message The tool call message to be described. Must be an instance of Message.Tool.Call.
     * @return A Message instance containing the description of the tool call.
     */
    public fun describeToolCall(message: Message.Tool.Call): Message

    /**
     * Describes the tool result by transforming it into a user-readable message object.
     *
     * @param message The tool result message to be described. It contains the tool call id, tool name, and content details.
     * @return A transformed message representing the description of the tool result.
     */
    public fun describeToolResult(message: Message.Tool.Result): Message

    /**
     * JSON object implementing the `ToolCallDescriber` interface.
     * This object is responsible for describing tool calls and results by converting them into a structured JSON-based format.
     */
    public object JSON : ToolCallDescriber {
        /**
         * A configuration of the kotlinx.serialization.Json instance tailored for serializing and
         * deserializing JSON data.
         *
         * This specific instance has the following options configured:
         * - `encodeDefaults` set to `true`: Ensures that default values are encoded during serialization.
         * - `explicitNulls` set to `false`: Avoids including `null` values explicitly in the resulting JSON output.
         *
         * It is used internally for encoding and decoding JSON represen

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] McpTool
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpTool.kt
Score: 0,729
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



---
## Summary
| Метрика | Значение |
|---------|---------|
| Всего вопросов | 5 |
| Avg top score | 0,73 |
| retrievalTopK | 15 |
| threshold | 0.5 |
| rerankStrategy | HEURISTIC |
| finalTopK | 5 |
