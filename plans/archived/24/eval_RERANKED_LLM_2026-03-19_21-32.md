# Evaluation: RERANKED_LLM | 2026-03-19_21-32
Config: retrievalTopK=15, threshold=0.33, rerank=LLM, finalTopK=5

---
## Query 1: "какие основные возможности агента, какой класс реализует"
**Optimized:** "what are the main agent capabilities, which class implements"
**Metrics:** Retrieved: 12 → Filtered: 12 → Final: 5
**Timings:** query_optimize=316ms, retrieve=147ms, filter=1ms, rerank=2546ms, top_k=0ms, pack=6ms
**Top score:** 0,75 | Avg score: 0,64

### RAG Context:
Found 4 relevant class(es) | ~9748 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt
Score: 0,750
Responsibility: Provides specific functionality and configuration capabilities for AI agents.
Key methods: createInitialConfig(agentConfig: AIAgentConfig)

--- for [ИСТОЧНИК 1] (line 1) ---
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

--- for [ИСТОЧНИК 2] (line 40) ---
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
Score: 0,650
Responsibility: Manages creation, removal, and management of AI agents.
Key methods: promptExecutor(), agentConfig(), toolRegistry()

--- for [ИСТОЧНИК 3] (line 226) ---
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
Score: 0,600
Responsibility: Represents a single-use AI agent with state.
Key methods: id(), strategy(), pipeline()

--- representing [ИСТОЧНИК 4] (line 1) ---
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
[CLASS] AIAgentRunSessionImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
Score: 0,500
Responsibility: Manages the lifecycle of an AI agent's execution.
Key methods: pipeline(), context(), run(input: Input)

--- handles [ИСТОЧНИК 5] (line 1) ---
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
## Источники
[ИСТОЧНИК 1] AIAgentFeature.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt
[ИСТОЧНИК 2] AIAgentFeature.kt · for · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt
[ИСТОЧНИК 3] AIAgentService.kt · for · line 226 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentService.kt
[ИСТОЧНИК 4] AIAgentBase.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt
[ИСТОЧНИК 5] AIAgentRunSessionImpl.kt · handles · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt


---
## Query 2: "как конфигурируется агент"
**Optimized:** "configure agent configuration"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=113ms, retrieve=41ms, filter=0ms, rerank=2267ms, top_k=0ms, pack=6ms
**Top score:** 0,92 | Avg score: 0,85

### RAG Context:
Found 5 relevant class(es) | ~6740 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] RegisteredFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
Score: 0,920
Responsibility: Represents a configured and installed agent feature implementation along with its configuration.
Key methods: featureImpl(), featureConfig()

--- RegisteredFeature [ИСТОЧНИК 1] (line 1) ---
// File: RegisteredFeature.kt
package ai.koog.agents.core.feature.pipeline

import ai.koog.agents.core.feature.config.FeatureConfig

/**
 * Represents configured and installed agent feature implementation along with its configuration.
 * @param featureImpl The feature implementation
 * @param featureConfig The feature configuration
 */
@Suppress("RedundantVisibilityModifier") // have to put public here, explicitApi requires it
public class RegisteredFeature(
    public val featureImpl: Any,
    public val featureConfig: FeatureConfig
)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] FEATURES.md
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md
Score: 0,880

--- to [ИСТОЧНИК 2] (line 45) ---
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
[CLASS] AIAgentServiceBuilderAPI
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt
Score: 0,850
Responsibility: Builds configuration for AI agent services.
Key methods: promptExecutor(promptExecutor: PromptExecutor), llmModel(model: LLModel), toolRegistry(toolRegistry: ToolRegistry), systemPrompt(systemPrompt: String), prompt(prompt: Prompt)

--- AIAgentServiceBuilderAPI [ИСТОЧНИК 3] (line 75) ---
// File: AIAgentServiceBuilderAPI.kt
nfigured for the AI agent service.
     * @return The instance of AIAgentServiceBuilder with the number of choices configured.
     */
    public fun numberOfChoices(numberOfChoices: Int): AIAgentServiceBuilderAPI

    /**
     * Sets the maximum number of iterations for the AI agent's process.
     *
     * @param maxIterations the maximum number of iterations to be performed
     * @return the updated instance of AIAgentServiceBuilder
     */
    public fun maxIterations(maxIterations: Int): AIAgentServiceBuilderAPI

    /**
     * Configures the AI agent service builder using the specified agent configuration.
     *
     * This method applies the parameters defined in the provided `AIAgentConfig` object
     * to the current instance of the `AIAgentServiceBuilder`. It sets the prompt, language
     * model, maximum number of iterations, and the strategy for handling missing tools during execution.
     *
     * @param config The configuration object containing the settings to be applied, including
     *        the prompt, model, maximum agent iterations, and missing tools conversion strategy.
     * @return The current instance of `AIAgentServiceBuilder` for method chaining.
     */
    @JavaAPI
    public fun agentConfig(config: AIAgentConfig): AIAgentServiceBuilderAPI

    /**
     * Configure a graph strategy and continue with a graph service builder.
     */
    public fun <Input, Output> graphStrategy(
        strategy: AIAgentGraphStrategy<Input, Output>
    ): GraphAgentServiceBuilder<Input, Output>

    /**
     * Configure a functional strategy and continue with a functional service builder.
     */
    public fun <Input, Output> functionalStrategy(
        strategy: AIAgentFunctionalStrategy<Input, Output>
    ): FunctionalAgentServiceBuilder<Input, Output>

    /**
     * Convenience build for GraphAIAgentService<String, String> using singleRunStrategy.
     */
    public fun build(): GraphAIAgentService<String, String>
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentBuilderImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilderImpl.kt
Score: 0,820
Responsibility: Builds and configures AI agent instances.
Key methods: promptExecutor(promptExecutor: PromptExecutor), llmModel(model: LLModel), toolRegistry(toolRegistry: ToolRegistry), graphStrategy(strategy: AIAgentGraphStrategy<Input, Output>)

--- AIAgentBuilderImpl [ИСТОЧНИК 4] (line 130) ---
// File: AIAgentBuilderImpl.kt
uilderAPI = apply {
        this.config = config
    }

    override fun <TConfig : FeatureConfig> install(
        feature: AIAgentGraphFeature<TConfig, *>,
        configure: ConfigureAction<TConfig>
    ): GraphAgentBuilder<String, String> = GraphAgentBuilder(
        strategy = singleRunStrategy(),
        inputType = typeToken<String>(),
        outputType = typeToken<String>(),
        promptExecutor = this.promptExecutor,
        id = this.id,
        config = config,
        clock = this.clock,
        toolRegistry = this.toolRegistry,
        featureInstallers = mutableListOf({
            install(feature) {
                configure.configure(this)
            }
        })
    )

    override fun build(): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = requireNotNull(promptExecutor) { "promptExecutor must be set" },
            strategy = singleRunStrategy(),
            toolRegistry = toolRegistry,
            id = id,
            agentConfig = validateConfig(config),
            clock = clock
        )
    }
}

internal fun validateConfig(config: AIAgentConfig): AIAgentConfig = when (config.model) {
    ModelNotSet -> throw IllegalArgumentException("model must be set, plase use .model() on AIAgentBuilder or set AIAgentConfig")
    else -> config
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentConfigBase
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/AIAgentConfigBase.kt
Score: 0,780
Responsibility: Defines configuration for AI agents.

--- for [ИСТОЧНИК 5] (line 1) ---
// File: AIAgentConfigBase.kt
package ai.koog.agents.core.agent.config

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.llm.LLModel

/**
 * Base interface for AI agent configs.
 */
public interface AIAgentConfigBase {

    /**
     * Defines the `Prompt` to be used in the AI agent's configuration.
     *
     * The `prompt` serves as the input structure for generating outputs from the language model and consists
     * of a list of messages, a unique identifier, and optional parameters. This property plays a role
     * in managing conversational state, input prompts, and configurations for the language model.
     */
    public val prompt: Prompt

    /**
     * Specifies the Large Language Model (LLM) used by the AI agent for generating responses.
     *
     * The model defines configurations such as the specific LLM provider, its identifier,
     * and supported capabilities (e.g., temperature control, tool usage). It plays a
     * vital role in determining how the AI agent processes and generates outputs
     * in response to user prompts and tasks.
     */
    public val model: LLModel
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] RegisteredFeature.kt · RegisteredFeature · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
[ИСТОЧНИК 2] FEATURES.md · to · line 45 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md
[ИСТОЧНИК 3] AIAgentServiceBuilderAPI.kt · AIAgentServiceBuilderAPI · line 75 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt
[ИСТОЧНИК 4] AIAgentBuilderImpl.kt · AIAgentBuilderImpl · line 130 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilderImpl.kt
[ИСТОЧНИК 5] AIAgentConfigBase.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/AIAgentConfigBase.kt


---
## Query 3: "как агент работает с историей сообщений"
**Optimized:** "agent messaging history interactions"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=120ms, retrieve=49ms, filter=0ms, rerank=2605ms, top_k=0ms, pack=5ms
**Top score:** 0,90 | Avg score: 0,85

### RAG Context:
Found 4 relevant class(es) | ~8076 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentContextData
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AgentContextData.kt
Score: 0,900
Responsibility: Stores context data for an agent, including message history and rollback strategy.
Key methods: init(messageHistory: List<Message>, nodePath: String, lastInput: JSONElement?, lastOutput: JSONElement?, rollbackStrategy: RollbackStrategy), <constructor>(messageHistory: List<Message>, nodePath: String, lastInput: JSONElement?, lastOutput: JSONElement?, rollbackStrategy: RollbackStrategy, additionalRollbackActions: suspend (AIAgentContext) -> Unit)

--- AgentContextData [ИСТОЧНИК 1] (line 1) ---
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
[CLASS] AIAgentLLMActions
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
Score: 0,850
Responsibility: Manages and manipulates message history in AI Agent LLM sessions.
Key methods: clearHistory(), leaveLastNMessages(n: Int, preserveSystemMessages: Boolean), dropLastNMessages(n: Int, preserveSystemMessages: Boolean), leaveMessagesFromTimestamp(timestamp: Instant, preserveSystemMessages: Boolean), setToolChoice(toolChoice: LLMParams.ToolChoice?)

--- AIAgentLLMWriteSession [ИСТОЧНИК 2] (line 1) ---
// File: AIAgentLLMActions.kt
package ai.koog.agents.core.dsl.extension

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import kotlin.time.Instant

/**
 * Clears the history of messages in the current AI Agent LLM Write Session.
 *
 * This method resets the message history by setting it to an empty list.
 * It is useful when you want to start a new conversation or reset the session's context.
 */
public fun AIAgentLLMWriteSession.clearHistory() {
    prompt = prompt.withMessages { emptyList() }
}

/**
 * Keeps only the last N messages in the session's prompt by removing all earlier messages.
 *
 * @param n The number of most recent messages to retain in the session's prompt.
 */
public fun AIAgentLLMWriteSession.leaveLastNMessages(n: Int, preserveSystemMessages: Boolean = true) {
    prompt = prompt.withMessages {
        val thresholdIndex = it.size - n
        it.filterIndexed { index, message ->
            index >= thresholdIndex || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Removes the last `n` messages from the current prompt in the write session.
 *
 * @param n The number of messages to remove from the end of the current message list.
 */
public fun AIAgentLLMWriteSession.dropLastNMessages(n: Int, preserveSystemMessages: Boolean = true) {
    prompt = prompt.withMessages {
        val thresholdIndex = it.size - n
        it.filterIndexed { index, message ->
            index < thresholdIndex || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Removes all messages from the current session's prompt that have a timestamp
 * earlier than the specified timestamp.
 *
 * @param timestamp The threshold timestamp. Messages with a timestamp earlier than this will be removed.
 */
public fun AIAgentLLMWriteSession.leaveMessagesFromTimestamp(
    timestamp: Instant,
    preserveSystemMessages: Boolean = true
) {
    prompt = prompt.withMessag

--- AIAgentLLMWriteSession [ИСТОЧНИК 3] (line 46) ---
// File: AIAgentLLMActions.kt
**
 * Removes all messages from the current session's prompt that have a timestamp
 * earlier than the specified timestamp.
 *
 * @param timestamp The threshold timestamp. Messages with a timestamp earlier than this will be removed.
 */
public fun AIAgentLLMWriteSession.leaveMessagesFromTimestamp(
    timestamp: Instant,
    preserveSystemMessages: Boolean = true
) {
    prompt = prompt.withMessages {
        it.filter { message ->
            message.metaInfo.timestamp >= timestamp || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Sets the [ai.koog.prompt.params.LLMParams.ToolChoice] for this LLM session.
 */
public fun AIAgentLLMWriteSession.setToolChoice(toolChoice: LLMParams.ToolChoice?) {
    prompt = prompt.withUpdatedParams { this.toolChoice = toolChoice }
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.Auto] to make LLM automatically decide between calling tools and generating text
 */
public fun AIAgentLLMWriteSession.setToolChoiceAuto() {
    setToolChoice(LLMParams.ToolChoice.Auto)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.Required] to make LLM always call tools
 */
public fun AIAgentLLMWriteSession.setToolChoiceRequired() {
    setToolChoice(LLMParams.ToolChoice.Required)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.None] to make LLM never call tools
 */
public fun AIAgentLLMWriteSession.setToolChoiceNone() {
    setToolChoice(LLMParams.ToolChoice.None)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.None] to make LLM call one specific tool [toolName]
 */
public fun AIAgentLLMWriteSession.setToolChoiceNamed(toolName: String) {
    setToolChoice(LLMParams.ToolChoice.Named(toolName))
}

/**
 * Unset the [ai.koog.prompt.params.LLMParams.ToolChoice].
 * Mostly, if left unsp

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SingleRunStrategyWithHistoryCompression
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/SingleRunStrategyWithHistoryCompression.kt
Score: 0,850
Responsibility: Adds conversation history compression to a single-run agent strategy.
Key methods: singleRunStrategyWithHistoryCompression(config: HistoryCompressionConfig, runMode: ToolCalls), isHistoryTooBig(prompt: Prompt)

--- HistoryCompressionConfig [ИСТОЧНИК 4] (line 32) ---
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
[CLASS] ChatMemory
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
Score: 0,850
Responsibility: Stores and loads conversation history between an agent and a user.
Key methods: createInitialConfig(agentConfig: AIAgentConfig), install(config: ChatMemoryConfig, pipeline: AIAgentGraphPipeline), install(config: ChatMemoryConfig, pipeline: AIAgentFunctionalPipeline), install(config: ChatMemoryConfig, pipeline: AIAgentPlannerPipeline), applyPreProcessors(messages: List<Message>, preProcessors: List<ChatMemoryPreProcessor>)

--- ChatMemory [ИСТОЧНИК 5] (line 92) ---
// File: ChatMemory.kt
ryProvider.load(it.context.runId)
                val processed = applyPreProcessors(history, config.preprocessors)

                it.context.llm.writeSession {
                    prompt = prompt.withMessages { processed }
                }
            }

            pipeline.interceptStrategyCompleted(this) {
                val history = it.context.llm.prompt.messages
                val processed = applyPreProcessors(history, config.preprocessors)
                config.chatHistoryProvider.store(it.context.runId, processed)
            }
        }
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AgentContextData.kt · AgentContextData · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AgentContextData.kt
[ИСТОЧНИК 2] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
[ИСТОЧНИК 3] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
[ИСТОЧНИК 4] SingleRunStrategyWithHistoryCompression.kt · HistoryCompressionConfig · line 32 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/SingleRunStrategyWithHistoryCompression.kt
[ИСТОЧНИК 5] ChatMemory.kt · ChatMemory · line 92 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt


---
## Query 4: "как реализованы стратегии компактизации контекста"
**Optimized:** "how context compact strategies are implemented in kotlin codebase"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=161ms, retrieve=54ms, filter=0ms, rerank=2624ms, top_k=0ms, pack=4ms
**Top score:** 0,95 | Avg score: 0,80

### RAG Context:
Found 3 relevant class(es) | ~9628 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] StrategyEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/strategy/StrategyEventContext.kt
Score: 0,950
Responsibility: Manages the lifecycle and operations of AI agent strategies.
Key methods: eventId(), executionInfo(), strategy(), context(), eventType()

--- StrategyEventContext [ИСТОЧНИК 1] (line 1) ---
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

--- StrategyEventContext [ИСТОЧНИК 2] (line 36) ---
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] DeprecatedStrategyEventHandlerContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/DeprecatedStrategyEventHandlerContext.kt
Score: 0,920
Responsibility: Represents the context for handling strategy-related events within the AI agent framework.

--- StrategyEventHandlerContext [ИСТОЧНИК 3] (line 1) ---
// File: DeprecatedStrategyEventHandlerContext.kt
package ai.koog.agents.core.feature.handler

/**
 * Defines the context specifically for handling strategy-related events within the AI agent framework.
 * Extends the base event handler context to include functionality and behavior dedicated to managing
 * the lifecycle and operations of strategies associated with AI agents.
 */
@Deprecated(
    message = "Use StrategyEventContext instead",
    replaceWith = ReplaceWith(
        expression = "StrategyEventContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.strategy.StrategyEventContext")
    )
)
public typealias StrategyEventHandlerContext = ai.koog.agents.core.feature.handler.strategy.StrategyEventContext

/**
 * Represents the context for starting AI agent strategies during execution.
 */
@Deprecated(
    message = "Use StrategyStartingContext instead",
    replaceWith = ReplaceWith(
        expression = "StrategyStartingContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.strategy.StrategyStartingContext")
    )
)
public typealias StrategyStartContext = ai.koog.agents.core.feature.handler.strategy.StrategyStartingContext

/**
 * Represents the context associated with the completion of an AI agent strategy execution.
 */
@Deprecated(
    message = "Use StrategyCompletedContext instead",
    replaceWith = ReplaceWith(
        expression = "StrategyCompletedContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.strategy.StrategyCompletedContext")
    )
)
public typealias StrategyFinishedContext = ai.koog.agents.core.feature.handler.strategy.StrategyCompletedContext

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentPipelineImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentPipelineImpl.kt
Score: 0,880
Responsibility: Manages AI agent pipelines and lifecycle events.
Key methods: onAgentStarted(), onAgentCompleted(), onLLMCallStarted(), onLLMCallCompleted(), onToolCallStarted()

--- AIAgentPipelineImpl [ИСТОЧНИК 4] (line 27) ---
// File: AIAgentPipelineImpl.kt
llm.LLMCallCompletedContext
import ai.koog.agents.core.feature.handler.llm.LLMCallStartingContext
import ai.koog.agents.core.feature.handler.strategy.StrategyCompletedContext
import ai.koog.agents.core.feature.handler.strategy.StrategyStartingContext
import ai.koog.agents.core.feature.handler.streaming.LLMStreamingCompletedContext
import ai.koog.agents.core.feature.handler.streaming.LLMStreamingFailedContext
import ai.koog.agents.core.feature.handler.streaming.LLMStreamingFrameReceivedContext
import ai.koog.agents.core.feature.handler.streaming.LLMStreamingStartingContext
import ai.koog.agents.core.feature.handler.tool.ToolCallCompletedContext
import ai.koog.agents.core.feature.handler.tool.ToolCallFailedContext
import ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext
import ai.koog.agents.core.feature.handler.tool.ToolValidationFailedContext
import ai.koog.agents.core.feature.model.AIAgentError
import ai.koog.agents.core.system.getEnvironmentVariableOrNull
import ai.koog.agents.core.system.getVMOptionOrNull
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONObject
import ai.koog.serialization.TypeToken
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.safeCast
import kotlin.time.Clock

/**
 * Default implementation of [AIAgentPipelineAPI]
 */
public class AIAgentPipelineImpl(
    override val config: AIAgentConfig,
    public override val clock: Clock
) : AIAgentPipelineAPI {

    // Notes on suppressed warnings used in this class:
    // - Some members are annotated with @Suppress to satisfy explicit API requirements
    //   (e.g., explicit public visibility) or to keep implementation details concise.
    //   These suppressions are intent

--- AIAgentPipelineImpl [ИСТОЧНИК 5] (line 188) ---
// File: AIAgentPipelineImpl.kt
 String,
        runId: String,
        result: Any?,
        context: AIAgentContext
    ) {
        invokeRegisteredHandlersForEvent(
            eventType = AgentLifecycleEventType.AgentCompleted,
            context = AgentCompletedContext(eventId, executionInfo, agentId, runId, result, context)
        )
    }

    @InternalAgentsApi
    public override suspend fun onAgentExecutionFailed(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        agentId: String,
        runId: String,
        throwable: Throwable,
        context: AIAgentContext
    ) {
        invokeRegisteredHandlersForEvent(
            eventType = AgentLifecycleEventType.AgentExecutionFailed,
            context = AgentExecutionFailedContext(eventId, executionInfo, agentId, runId, throwable, context)
        )
    }

    @InternalAgentsApi
    public override suspend fun onAgentClosing(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        agentId: String
    ) {
        invokeRegisteredHandlersForEvent(
            eventType = AgentLifecycleEventType.AgentClosing,
            context = AgentClosingContext(eventId, executionInfo, agentId, config)
        )
    }

    @InternalAgentsApi
    public override suspend fun onAgentEnvironmentTransforming(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        agent: GraphAIAgent<*, *>,
        baseEnvironment: AIAgentEnvironment
    ): AIAgentEnvironment {
        return invokeRegisteredHandlersForEvent(
            eventType = AgentLifecycleEventType.AgentEnvironmentTransforming,
            context = AgentEnvironmentTransformingContext(eventId, executionInfo, agent, config),
            entity = baseEnvironment
        )
    }

    //endregion Invoke Agent Handlers

    //region Invoke Strategy Handlers

    @InternalAgentsApi
    public override suspend fun onStrategyStarting(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        strategy: AIAgentStrategy<*, *, *

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] StrategyEventContext.kt · StrategyEventContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/strategy/StrategyEventContext.kt
[ИСТОЧНИК 2] StrategyEventContext.kt · StrategyEventContext · line 36 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/strategy/StrategyEventContext.kt
[ИСТОЧНИК 3] DeprecatedStrategyEventHandlerContext.kt · StrategyEventHandlerContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/DeprecatedStrategyEventHandlerContext.kt
[ИСТОЧНИК 4] AIAgentPipelineImpl.kt · AIAgentPipelineImpl · line 27 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentPipelineImpl.kt
[ИСТОЧНИК 5] AIAgentPipelineImpl.kt · AIAgentPipelineImpl · line 188 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentPipelineImpl.kt


---
## Query 5: "как реализован tool calling"
**Optimized:** "how-tool-calling-implemented-in-kotlin-codebase"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=164ms, retrieve=50ms, filter=0ms, rerank=2173ms, top_k=0ms, pack=4ms
**Top score:** 0,80 | Avg score: 0,73

### RAG Context:
Found 3 relevant class(es) | ~9540 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolCallEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/tool/ToolCallEventContext.kt
Score: 0,800
Responsibility: Represents the context for handling tool call events.

--- ToolCallEventContext [ИСТОЧНИК 1] (line 53) ---
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

--- ToolCallEventContext [ИСТОЧНИК 2] (line 91) ---
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

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] toolExecutionEvents
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
Score: 0,750
Responsibility: Tracks and logs tool calls within the system.
Key methods: ToolCallStartingEvent(eventId: String, executionInfo: AgentExecutionInfo, runId: String, toolCallId: String?, toolName: String, toolArgs: JSONObject, timestamp: Long), ToolCallStartingEvent(runId: String, toolCallId: String?, toolName: String, toolArgs: JSONObject, timestamp: Long)

--- ToolCallStartingEvent [ИСТОЧНИК 3] (line 113) ---
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

--- ToolCallStartingEvent [ИСТОЧНИК 4] (line 193) ---
// File: toolExecutionEvents.kt
ut the execution associated with this event.
 * @property runId A unique identifier representing the specific run or instance of the tool call;
 * @property toolCallId A unique identifier for the tool call that was executed;
 * @property toolName The name of the tool that was executed;
 * @property toolArgs The arguments used for executing the tool;
 * @property toolDescription A description of the tool that was executed;
 * @property result The result of the tool execution, which may be null if no result was produced or an error occurred;
 * @property timestamp The timestamp of the event, in milliseconds since the Unix epoch.
 */
@Serializable
public data class ToolCallCompletedEvent(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val runId: String,
    val toolCallId: String?,
    val toolName: String,
    val toolArgs: JSONObject,
    val toolDescription: String?,
    val result: JSONElement?,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) : DefinedFeatureEvent() {

    /**
     * @deprecated Use constructor with [executionInfo] parameter
     */
    @Deprecated(
        message = "Use constructor with executionInfo parameter",
        replaceWith = ReplaceWith("ToolCallCompletedEvent(executionInfo, runId, toolCallId, toolName, toolArgs, result, timestamp)")
    )
    public constructor(
        runId: String,
        toolCallId: String?,
        toolName: String,
        toolArgs: JSONObject,
        result: String?,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : this(
        eventId = ToolCallCompletedEvent::class.simpleName.toString(),
        executionInfo = AgentExecutionInfo(
            parent = null,
            partName = ToolCallCompletedEvent::class.simpleName.toString(),
        ),
        runId = runId,
        toolCallId = toolCallId,
        toolName = toolName,
        toolArgs = toolArgs,
        toolDescription = null,
        result = JSONPri

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] DeprecatedExecuteToolEventHandlerContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/DeprecatedExecuteToolEventHandlerContext.kt
Score: 0,680
Responsibility: Represents the context for handling tool-specific events within the framework.

--- ToolEventHandlerContext [ИСТОЧНИК 5] (line 1) ---
// File: DeprecatedExecuteToolEventHandlerContext.kt
package ai.koog.agents.core.feature.handler

/**
 * Represents the context for handling tool-specific events within the framework.
 */
@Deprecated(
    message = "Use ToolCallEventContext instead",
    replaceWith = ReplaceWith(
        expression = "ToolCallEventContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.tool.ToolCallEventContext")
    )
)
public typealias ToolEventHandlerContext = ai.koog.agents.core.feature.handler.tool.ToolCallEventContext

/**
 * Represents the context for handling a tool call event.
 */
@Deprecated(
    message = "Use ToolCallStartingContext instead",
    replaceWith = ReplaceWith(
        expression = "ToolCallStartingContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext")
    )
)
public typealias ToolCallContext = ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext

/**
 * Represents the context for handling validation errors that occur during the execution of a tool.
 */
@Deprecated(
    message = "Use ToolValidationFailedContext instead",
    replaceWith = ReplaceWith(
        expression = "ToolValidationFailedContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.tool.ToolValidationFailedContext")
    )
)
public typealias ToolValidationErrorContext = ai.koog.agents.core.feature.handler.tool.ToolValidationFailedContext

/**
 * Represents the context provided to handle a failure during the execution of a tool.
 */
@Deprecated(
    message = "Use ToolCallFailedContext instead",
    replaceWith = ReplaceWith(
        expression = "ToolCallFailedContext",
        imports = arrayOf("ai.koog.agents.core.feature.handler.tool.ToolCallFailedContext")
    )
)
public typealias ToolCallFailureContext = ai.koog.agents.core.feature.handler.tool.ToolCallFailedContext

/**
 * Represents the context used when handling the result of a tool call.
 */
@Deprecated(
    message = "Use ToolCallCompletedContext instead",
    replaceWith = ReplaceWith(

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] ToolCallEventContext.kt · ToolCallEventContext · line 53 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/tool/ToolCallEventContext.kt
[ИСТОЧНИК 2] ToolCallEventContext.kt · ToolCallEventContext · line 91 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/tool/ToolCallEventContext.kt
[ИСТОЧНИК 3] toolExecutionEvents.kt · ToolCallStartingEvent · line 113 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 4] toolExecutionEvents.kt · ToolCallStartingEvent · line 193 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 5] DeprecatedExecuteToolEventHandlerContext.kt · ToolEventHandlerContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/DeprecatedExecuteToolEventHandlerContext.kt


---
## Query 6: "как реализован механизм user in the loop"
**Optimized:** "how-user-loop-implemented-in-kotlin-codebase"
**Metrics:** Retrieved: 12 → Filtered: 12 → Final: 5
**Timings:** query_optimize=163ms, retrieve=52ms, filter=0ms, rerank=2460ms, top_k=0ms, pack=4ms
**Top score:** 0,90 | Avg score: 0,74

### RAG Context:
Found 2 relevant class(es) | ~9124 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFunctionalStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt
Score: 0,900
Responsibility: Defines a strategy for implementing AI agent behavior in a loop-based manner.
Key methods: execute(context: AIAgentFunctionalContext, input: Input), functionalStrategy(name: String = "funStrategy", func: suspend AIAgentFunctionalContext.(input: Input) -> Output)

--- allows [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentFunctionalStrategy.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.context.AIAgentFunctionalContext
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * A strategy for implementing AI agent behavior that operates in a loop-based manner.
 *
 * The [AIAgentFunctionalStrategy] class allows for the definition of a custom looping logic
 * that processes input and produces output by utilizing an [ai.koog.agents.core.agent.context.AIAgentFunctionalContext]. This strategy
 * can be used to define iterative decision-making or execution processes for AI agents.
 *
 * @param TInput The type of input data processed by the strategy.
 * @param TOutput The type of output data produced by the strategy.
 * @property name The name of the strategy, providing a way to identify and describe the strategy.
 * @property func A suspending function representing the loop logic for the strategy. It accepts
 * input data of type [TInput] and an [ai.koog.agents.core.agent.context.AIAgentFunctionalContext] to execute the loop and produce the output.
 */
public interface AIAgentFunctionalStrategy<TInput, TOutput> :
    AIAgentStrategy<TInput, TOutput, AIAgentFunctionalContext> {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}

/**
 * Creates an [AIAgentFunctionalStrategy] with the specified loop logic and name.
 *
 * This function allows the definition of custom looping strategies for AI agents, where
 * the provided logic defines how the agent processes input and produces output within
 * its execution context.
 *
 * @param name The name of the strategy, used to identify and describe the strategy. Defaults to "funStrategy".
 * @param func A suspending function representing the loop logic of the strategy. It accepts an input of type [Input]
 * and is executed within an [AIAgentFunctionalContext], producing an output of type [O

--- allows [ИСТОЧНИК 2] (line 34) ---
// File: AIAgentFunctionalStrategy.kt
 the agent processes input and produces output within
 * its execution context.
 *
 * @param name The name of the strategy, used to identify and describe the strategy. Defaults to "funStrategy".
 * @param func A suspending function representing the loop logic of the strategy. It accepts an input of type [Input]
 * and is executed within an [AIAgentFunctionalContext], producing an output of type [Output].
 * @return An instance of [AIAgentFunctionalStrategy] configured with the given loop logic and name.
 */
public fun <Input, Output> functionalStrategy(
    name: String = "funStrategy",
    func: suspend AIAgentFunctionalContext.(input: Input) -> Output
): AIAgentFunctionalStrategy<Input, Output> = object : AIAgentFunctionalStrategy<Input, Output> {
    override val name: String = name
    override suspend fun execute(context: AIAgentFunctionalContext, input: Input): Output {
        return context.func(input)
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFunctionalContextBase
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt
Score: 0,700
Responsibility: Represents the execution context for an AI agent operating in a loop.
Key methods: environment(), agentId(), runId(), agentInput(), config()

--- allowing [ИСТОЧНИК 3] (line 1) ---
// File: AIAgentFunctionalContextBase.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.context

import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStateManager
import ai.koog.agents.core.agent.entity.AIAgentStorage
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.ext.agent.CriticResult
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.processor.ResponseProcessor
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.structure.StructureDefinition
import ai.koog.prompt.structure.StructuredResponse
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Represents the execution context for an AI agent operating in a loop.
 * It provides access to critical parts such as the environment, configuration, large language model (LLM) context,
 * state management, and storage. Additionally, it enables the agent to store, retrieve, and manage context-specific data
 * during its execution lifecycle.
 *
 * @property environment The environment interface allowing the agent to interact with the external world,
 * including executing tools and reporting problems.
 * @property agentId A unique identifier for the agent, differentiating it from other agents in the system.
 * @property runId A unique

--- allowing [ИСТОЧНИК 4] (line 34) ---
// File: AIAgentFunctionalContextBase.kt
les the agent to store, retrieve, and manage context-specific data
 * during its execution lifecycle.
 *
 * @property environment The environment interface allowing the agent to interact with the external world,
 * including executing tools and reporting problems.
 * @property agentId A unique identifier for the agent, differentiating it from other agents in the system.
 * @property runId A unique identifier for the current run or instance of the agent's operation.
 * @property agentInput The input data passed to the agent, which can be of any type, depending on the agent's context.
 * @property config The configuration settings for the agent, including its prompt and model details,
 * as well as operational constraints like iteration limits.
 * @property llm The context for interacting with the large language model used by the agent, enabling message history
 * retrieval and processing.
 * @property stateManager The state management component responsible for tracking and updating the agent's state during its execution.
 * @property storage A storage interface providing persistent storage capabilities for the agent's data.
 * @property strategyName The name of the agent's strategic approach or operational method, determining its behavior
 * during execution.
 */
@OptIn(InternalAgentsApi::class)
@Suppress("UNCHECKED_CAST", "MissingKDocForPublicAPI")
public expect abstract class AIAgentFunctionalContextBase<Pipeline : AIAgentPipeline> internal constructor(
    delegate: AIAgentFunctionalContextBaseImpl<Pipeline>
) : AIAgentFunctionalContextBaseAPI<Pipeline> {

    internal val delegate: AIAgentFunctionalContextBaseImpl<Pipeline>

    override val environment: AIAgentEnvironment
    override val agentId: String
    override val runId: String
    override val agentInput: Any?
    override val config: AIAgentConfig
    override val llm: AIAgentLLMContext
    override val stateManager: AIAgentStateManager
    override val storage: AIAgentStorage
    override val strategyN

--- allowing [ИСТОЧНИК 5] (line 57) ---
// File: AIAgentFunctionalContextBase.kt
l delegate: AIAgentFunctionalContextBaseImpl<Pipeline>

    override val environment: AIAgentEnvironment
    override val agentId: String
    override val runId: String
    override val agentInput: Any?
    override val config: AIAgentConfig
    override val llm: AIAgentLLMContext
    override val stateManager: AIAgentStateManager
    override val storage: AIAgentStorage
    override val strategyName: String
    override val pipeline: Pipeline
    override var executionInfo: AgentExecutionInfo
    override val parentContext: AIAgentContext?

    override fun store(key: AIAgentStorageKey<*>, value: Any)

    override fun <T> get(key: AIAgentStorageKey<*>): T?

    override fun remove(key: AIAgentStorageKey<*>): Boolean

    override suspend fun getHistory(): List<Message>

    public override suspend fun requestLLM(
        message: String,
        allowToolCalls: Boolean
    ): Message.Response

    public override fun onAssistantMessage(
        response: Message.Response,
        action: (Message.Assistant) -> Unit
    )

    public override fun Message.Response.asAssistantMessageOrNull(): Message.Assistant?

    public override fun Message.Response.asAssistantMessage(): Message.Assistant

    public override fun onMultipleToolCalls(
        response: List<Message.Response>,
        action: (List<Message.Tool.Call>) -> Unit
    )

    public override fun extractToolCalls(
        response: List<Message.Response>
    ): List<Message.Tool.Call>

    public override fun onMultipleAssistantMessages(
        response: List<Message.Response>,
        action: (List<Message.Assistant>) -> Unit
    )

    public override suspend fun latestTokenUsage(): Int

    public suspend inline fun <reified T> requestLLMStructured(
        message: String,
        examples: List<T> = emptyList(),
        fixingParser: StructureFixingParser? = null
    ): Result<StructuredResponse<T>>

    public override suspend fun requestLLMStreaming(
        message: String,
        structureDefini

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentFunctionalStrategy.kt · allows · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt
[ИСТОЧНИК 2] AIAgentFunctionalStrategy.kt · allows · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt
[ИСТОЧНИК 3] AIAgentFunctionalContextBase.kt · allowing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt
[ИСТОЧНИК 4] AIAgentFunctionalContextBase.kt · allowing · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt
[ИСТОЧНИК 5] AIAgentFunctionalContextBase.kt · allowing · line 57 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt


---
## Query 7: "как агенты работают с памятью и  сколько слоев используют в"
**Optimized:** "agent memory implementation layers"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=122ms, retrieve=96ms, filter=0ms, rerank=2148ms, top_k=0ms, pack=4ms
**Top score:** 0,95 | Avg score: 0,90

### RAG Context:
Found 2 relevant class(es) | ~8736 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentMemoryProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
Score: 0,950
Responsibility: Core interface for managing an agent's persistent memory system.
Key methods: save(fact: Fact, subject: MemorySubject, scope: MemoryScope), load(concept: Concept, subject: MemorySubject, scope: MemoryScope)

--- for [ИСТОЧНИК 1] (line 1) ---
// File: AgentMemoryProvider.kt
package ai.koog.agents.memory.providers

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

/**
 * Core interface for managing an agent's persistent memory system.
 * This interface defines the fundamental operations for storing and retrieving
 * knowledge in a structured, context-aware manner.
 *
 * Key features:
 * - Structured knowledge storage using concepts and facts
 * - Context-aware memory organization (subjects and scopes)
 * - Flexible storage backend support (local/remote)
 * - Semantic search capabilities
 *
 * Usage example:
 * ```
 * val provider: AgentMemoryProvider = LocalFileMemoryProvider(
 *     config = LocalMemoryConfig("memory"),
 *     storage = EncryptedStorage(fs, encryption),
 *     fs = JVMFileSystemProvider,
 *     root = basePath
 * )
 *
 * // Store project information
 * provider.save(
 *     fact = SingleFact(
 *         concept = Concept("build-system", "Project build configuration", FactType.SINGLE),
 *         timestamp = currentTime,
 *         value = "Gradle 8.0"
 *     ),
 *     subject = MemorySubject.Project,
 *     scope = MemoryScope.Product("my-app")
 * )
 *
 * // Retrieve environment information
 * val envFacts = provider.loadByDescription(
 *     description = "system environment",
 *     subject = MemorySubject.Machine,
 *     scope = MemoryScope.Agent("env-analyzer")
 * )
 * ```
 */
public interface AgentMemoryProvider {
    /**
     * Persists a fact in the agent's memory system.
     * This operation ensures:
     * - Atomic storage of the fact
     * - Proper scoping and subject categorization
     * - Consistent storage format
     *
     * @param fact Knowledge unit to store (can be SingleFact or MultipleFacts)
     * @param subject Context category (e.g., MACHINE, PROJECT)

--- for [ИСТОЧНИК 2] (line 50) ---
// File: AgentMemoryProvider.kt
interface AgentMemoryProvider {
    /**
     * Persists a fact in the agent's memory system.
     * This operation ensures:
     * - Atomic storage of the fact
     * - Proper scoping and subject categorization
     * - Consistent storage format
     *
     * @param fact Knowledge unit to store (can be SingleFact or MultipleFacts)
     * @param subject Context category (e.g., MACHINE, PROJECT)
     * @param scope Visibility boundary (e.g., Agent, Feature)
     * @throws IOException if storage operation fails
     */
    public suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope)

    /**
     * Retrieves facts associated with a specific concept.
     * This operation provides:
     * - Direct concept-based knowledge retrieval
     * - Context-aware fact filtering
     * - Ordered fact list (typically by timestamp)
     *
     * @param concept Knowledge category to retrieve
     * @param subject Context to search within
     * @param scope Visibility boundary to consider
     * @return List of matching facts, empty if none found
     */
    public suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact>

    /**
     * Retrieves all facts within a specific context.
     * This operation is useful for:
     * - Building comprehensive context understanding
     * - Memory analysis and debugging
     * - Data migration between storage backends
     *
     * @param subject Context to retrieve from
     * @param scope Visibility boundary to consider
     * @return All available facts in the context
     */
    public suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact>

    /**
     * Performs semantic search across stored facts.
     * This operation enables:
     * - Natural language queries
     * - Fuzzy concept matching
     * - Context-aware search results
     *
     * Implementation considerations:
     * - May use different matching algorithms
     * - Could integrate with LLM for better

--- for [ИСТОЧНИК 3] (line 137) ---
// File: AgentMemoryProvider.kt
ptional encryption support
 *
 * Usage example:
 * ```
 * val config = LocalMemoryConfig(
 *     storageDirectory = "agent-memory",
 *     defaultScope = MemoryScope.Agent("assistant")
 * )
 * ```
 *
 * @property storageDirectory Base directory for memory files
 * @property defaultScope Default visibility scope, typically agent-specific
 */
@Serializable
@SerialName("local")
public data class LocalMemoryConfig @JvmOverloads constructor(
    val storageDirectory: String,
    override val defaultScope: MemoryScope = MemoryScope.CrossProduct,
) : MemoryProviderConfig

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] LocalFileMemoryProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt
Score: 0,890
Responsibility: Provides persistent storage of agent memory using a hierarchical file system structure.
Key methods: save(fact: Fact, subject: MemorySubject, scope: MemoryScope), load(concept: Concept, subject: MemorySubject, scope: MemoryScope), delete(subject: MemorySubject, scope: MemoryScope), listSubjects(scope: MemoryScope), exists(subject: MemorySubject, scope: MemoryScope)

--- LocalFileMemoryProvider [ИСТОЧНИК 4] (line 1) ---
// File: LocalFileMemoryProvider.kt
package ai.koog.agents.memory.providers

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.storage.Storage
import ai.koog.rag.base.files.FileSystemProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * File-based implementation of [AgentMemoryProvider] that provides persistent storage of agent memory
 * using a hierarchical file system structure. This implementation is designed for durability,
 * thread safety, and human readability of stored data.
 *
 * Key features:
 * - Thread-safe operations using mutex locks
 * - Hierarchical storage structure for efficient organization
 * - JSON-based storage with pretty printing for human readability
 * - Support for multiple memory scopes and subjects
 * - Atomic read/write operations
 *
 * Storage Structure:
 * ```
 * root/
 *   storageDirectory/
 *     agent/                    # For MemoryScope.Agent
 *       [agent-name]/
 *         subject/
 *           [subject-name]/
 *             facts.json
 *     feature/                  # For MemoryScope.Feature
 *       [feature-id]/
 *         subject/
 *           [subject-name]/
 *             facts.json
 *     product/                  # For MemoryScope.Product
 *       [product-name]/
 *         subject/
 *           [subject-name]/
 *             facts.json
 *     organization/             # For MemoryScope.CrossProduct
 *       subject/
 *         [subject-name]/
 *           facts.json
 * ```
 *
 * Usage example:
 * ```
 * val provider = LocalFileMemoryProvider(
 *     config = LocalMemoryConfig("memory-storage"),
 *     storage = EncryptedStorage(fileSystem),
 *     fs = JVMFileSystemProvider,
 *     root = Path("path/to/root")
 * )
 *

--- LocalFileMemoryProvider [ИСТОЧНИК 5] (line 45) ---
// File: LocalFileMemoryProvider.kt
acts.json
 *     organization/             # For MemoryScope.CrossProduct
 *       subject/
 *         [subject-name]/
 *           facts.json
 * ```
 *
 * Usage example:
 * ```
 * val provider = LocalFileMemoryProvider(
 *     config = LocalMemoryConfig("memory-storage"),
 *     storage = EncryptedStorage(fileSystem),
 *     fs = JVMFileSystemProvider,
 *     root = Path("path/to/root")
 * )
 *
 * // Store environment information
 * provider.save(
 *     fact = environmentFact,
 *     subject = MemorySubject.Machine,
 *     scope = MemoryScope.Agent("env-analyzer")
 * )
 *
 * // Retrieve project dependencies
 * val dependencies = provider.load(
 *     concept = dependenciesConcept,
 *     subject = MemorySubject.Project,
 *     scope = MemoryScope.Product("my-product")
 * )
 * ```
 *
 * @param Path The type representing file system paths (platform-specific)
 * @property config Configuration for local storage including base directory and options
 * @property storage Implementation of storage operations (can be encrypted or plain)
 * @property fs Platform-specific file system provider for path manipulations
 * @property root Root directory where all memory storage will be located
 */
public data class LocalFileMemoryProvider<Path>(
    private val config: LocalMemoryConfig,
    private val storage: Storage<Path>,
    private val fs: FileSystemProvider.ReadWrite<Path>,
    private val root: Path,
) : AgentMemoryProvider {
    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    /**
     * Mutex for ensuring thread-safe access to fact storage.
     * This lock prevents race conditions during concurrent read/write operations by:
     * - Ensuring atomic updates to fact collections
     * - Preventing concurrent modifications to the same file
     * - Maintaining consistency between memory and disk state
     */
    private val mutex = Mutex()

    /**
     * JSON serializer configuration optimized for memory storage.
     * Configu

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 2] AgentMemoryProvider.kt · for · line 50 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 3] AgentMemoryProvider.kt · for · line 137 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 4] LocalFileMemoryProvider.kt · LocalFileMemoryProvider · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt
[ИСТОЧНИК 5] LocalFileMemoryProvider.kt · LocalFileMemoryProvider · line 45 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt


---
## Query 8: "как агенты работают с mcp"
**Optimized:** "agent interactions with mcp"
**Metrics:** Retrieved: 14 → Filtered: 14 → Final: 5
**Timings:** query_optimize=125ms, retrieve=48ms, filter=0ms, rerank=3001ms, top_k=0ms, pack=3ms
**Top score:** 0,90 | Avg score: 0,77

### RAG Context:
Found 2 relevant class(es) | ~10164 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] McpToolRegistryProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
Score: 0,900
Responsibility: Facilitates the integration of Model Context Protocol (MCP) tools into the agent framework.
Key methods: defaultSseTransport(url: String, baseClient: HttpClient), fromClient(mcpClient: Client, mcpToolParser: McpToolDescriptorParser)

--- facilitates [ИСТОЧНИК 1] (line 1) ---
// File: McpToolRegistryProvider.kt
package ai.koog.agents.mcp

import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.metadata.McpMetadataKeys
import ai.koog.agents.mcp.metadata.McpServerInfo
import ai.koog.agents.mcp.metadata.McpTransportType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.Url
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.LATEST_PROTOCOL_VERSION
import io.modelcontextprotocol.kotlin.sdk.types.Tool

/**
 * A provider for creating tool registries that connect to Model Context Protocol (MCP) servers.
 *
 * This class facilitates the integration of MCP tools into the agent framework by:
 * 1. Connecting to MCP servers through various transport mechanisms (stdio, SSE)
 * 2. Retrieving available tools from the MCP server
 * 3. Transforming MCP tools into the agent framework's Tool interface
 * 4. Registering the transformed tools in a ToolRegistry
 */
public object McpToolRegistryProvider {
    private val logger = KotlinLogging.logger {}

    /**
     * Default name for the MCP client when connecting to an MCP server.
     */
    public const val DEFAULT_MCP_CLIENT_NAME: String = "mcp-client-cli"

    /**
     * Default version for the MCP client when connecting to an MCP server.
     */
    public const val DEFAULT_MCP_CLIENT_VERSION: String = "1.0.0"

    /**
     * Creates a default server-sent events (SSE) transport from a provided URL.
     *
     * @param url The URL to be used for establishing an SSE connection.
     * @return An instance of SseClientTransport configured with the given URL.
     */
    pu

--- facilitates [ИСТОЧНИК 2] (line 38) ---
// File: McpToolRegistryProvider.kt
 version for the MCP client when connecting to an MCP server.
     */
    public const val DEFAULT_MCP_CLIENT_VERSION: String = "1.0.0"

    /**
     * Creates a default server-sent events (SSE) transport from a provided URL.
     *
     * @param url The URL to be used for establishing an SSE connection.
     * @return An instance of SseClientTransport configured with the given URL.
     */
    public fun defaultSseTransport(url: String, baseClient: HttpClient = HttpClient()): SseClientTransport {
        // Setup SSE transport using the HTTP client
        return SseClientTransport(
            client = baseClient.config {
                install(SSE)
            },
            urlString = url,
        )
    }

    /**
     * Creates a ToolRegistry with tools from an existing MCP client.
     *
     * This method retrieves all available tools from the MCP server using the provided client,
     * transforms them into the agent framework's Tool interface, and registers them in a ToolRegistry.
     *
     * @param mcpClient The MCP client connected to an MCP server.
     * @return A ToolRegistry containing all tools from the MCP server.
     */
    @Deprecated("Use fromClient with serverInfo param")
    public suspend fun fromClient(
        mcpClient: Client,
        mcpToolParser: McpToolDescriptorParser = DefaultMcpToolDescriptorParser,
    ): ToolRegistry {
        return fromClient(mcpClient, McpServerInfo(url = null, command = null), mcpToolParser)
    }

    /**
     * Creates a ToolRegistry with tools from an existing MCP client.
     *
     * This method retrieves all available tools from the MCP server using the provided client,
     * transforms them into the agent framework's Tool interface, and registers them in a ToolRegistry.
     *
     * @param mcpClient The MCP client connected to an MCP server.
     * @param serverInfo Information about the MCP server.
     * @return A ToolRegistry containing all tools from the MCP server.
     */
    public suspend

--- facilitates [ИСТОЧНИК 3] (line 78) ---
// File: McpToolRegistryProvider.kt
l available tools from the MCP server using the provided client,
     * transforms them into the agent framework's Tool interface, and registers them in a ToolRegistry.
     *
     * @param mcpClient The MCP client connected to an MCP server.
     * @param serverInfo Information about the MCP server.
     * @return A ToolRegistry containing all tools from the MCP server.
     */
    public suspend fun fromClient(
        mcpClient: Client,
        serverInfo: McpServerInfo,
        mcpToolParser: McpToolDescriptorParser = DefaultMcpToolDescriptorParser,
    ): ToolRegistry {
        val sdkTools = mcpClient.listTools().tools
        return buildToolRegistry(sdkTools, mcpToolParser, serverInfo, mcpClient)
    }

    @OptIn(InternalAgentsApi::class)
    private fun buildToolRegistry(
        sdkTools: List<Tool>,
        mcpToolParser: McpToolDescriptorParser,
        serverInfo: McpServerInfo,
        mcpClient: Client
    ): ToolRegistry = ToolRegistry {
        sdkTools.forEach { sdkTool ->
            try {
                val toolDescriptor = mcpToolParser.parse(sdkTool)
                val toolMetaData = mapOf(
                    McpMetadataKeys.ToolId to sdkTool.name,
                    McpMetadataKeys.McpProtocolVersion to LATEST_PROTOCOL_VERSION,
                    McpMetadataKeys.McpTransportType to when (mcpClient.transport) {
                        is SseClientTransport -> McpTransportType.Tcp
                        is StdioClientTransport -> McpTransportType.Stdio
                        else -> error("Unexpected null for client transport: ${mcpClient.transport}")
                    }.value,
                    McpMetadataKeys.McpSessionId to "",
                    McpMetadataKeys.ServerUrl to serverInfo.url.orEmpty(),
                    McpMetadataKeys.ServerPort to getPort(serverInfo.url),
                )
                tool(McpTool(mcpClient, toolDescriptor, toolMetaData))
            } catch (e: Throwable) {
                logger.error(e)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
Score: 0,750
Responsibility: Manages tools, prompts, and interactions for an AI agent's language model.
Key methods: constructor(tools: List<ToolDescriptor>, toolRegistry: ToolRegistry, prompt: Prompt, model: LLModel, responseProcessor: ResponseProcessor?, promptExecutor: PromptExecutor, environment: AIAgentEnvironment, config:), handleRead(), handleWrite()

--- AIAgentLLMContext [ИСТОЧНИК 4] (line 31) ---
// File: AIAgentLLMContext.kt
ronment The environment that manages tool execution and interaction with external dependencies.
 * @property clock The clock used for timestamps of messages
 */
public expect class AIAgentLLMContext internal constructor(
    delegate: AIAgentLLMContextImpl
) : AIAgentLLMContextAPI {

    /**
     * Constructs a new instance of `AIAgentLLMContext` with the provided parameters.
     *
     * @param tools A list of tools described by [ToolDescriptor] that the agent can interact with.
     * @param toolRegistry A registry of available tools, defaulting to an empty [ToolRegistry].
     * @param prompt The initial prompt used in the context, represented by a [Prompt] instance.
     * @param model The language model used for processing prompts and generating responses.
     * @param responseProcessor An optional [ResponseProcessor] for handling and processing model responses.
     * @param promptExecutor Responsible for executing the logic for prompt processing in the context.
     * @param environment The operational environment of the AI agent, represented by an [AIAgentEnvironment].
     * @param config Configuration settings for the AI agent, encapsulated in an [AIAgentConfig].
     * @param clock A clock instance for managing time-related operations within the context.
     */
    public constructor(
        tools: List<ToolDescriptor>,
        toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
        prompt: Prompt,
        model: LLModel,
        responseProcessor: ResponseProcessor?,
        promptExecutor: PromptExecutor,
        environment: AIAgentEnvironment,
        config: AIAgentConfig,
        clock: Clock
    )

    internal val delegate: AIAgentLLMContextImpl

    @get:JvmName("toolRegistry")
    override val toolRegistry: ToolRegistry

    @property:DetachedPromptExecutorAPI
    @get:JvmName("promptExecutor")
    override val promptExecutor: PromptExecutor

    @get:JvmName("environment")
    @InternalAgentsApi
    override val environment: AIAgentEnviron

--- AIAgentLLMContext [ИСТОЧНИК 5] (line 59) ---
// File: AIAgentLLMContext.kt
nfig: AIAgentConfig,
        clock: Clock
    )

    internal val delegate: AIAgentLLMContextImpl

    @get:JvmName("toolRegistry")
    override val toolRegistry: ToolRegistry

    @property:DetachedPromptExecutorAPI
    @get:JvmName("promptExecutor")
    override val promptExecutor: PromptExecutor

    @get:JvmName("environment")
    @InternalAgentsApi
    override val environment: AIAgentEnvironment

    @get:JvmName("config")
    @InternalAgentsApi
    override val config: AIAgentConfig

    @get:JvmName("clock")
    @InternalAgentsApi
    override val clock: Clock

    /**
     * List of current tools associated with this agent context.
     */
    @DetachedPromptExecutorAPI
    @get:JvmName("tools")
    override var tools: List<ToolDescriptor>
        @InternalAgentsApi set

    /**
     * LLM currently associated with this context.
     */
    @DetachedPromptExecutorAPI
    @get:JvmName("model")
    override var model: LLModel
        @InternalAgentsApi set

    /**
     * Response processor currently associated with this context.
     */
    @DetachedPromptExecutorAPI
    @get:JvmName("responseProcessor")
    public override var responseProcessor: ResponseProcessor?
        @InternalAgentsApi set

    /**
     * The current prompt used within the `AIAgentLLMContext`.
     *
     * This property defines the main [Prompt] instance used by the context and is updated as needed to reflect
     * modifications or new inputs for the language model operations. It is thread-safe, ensuring that updates
     * and access are managed correctly within concurrent environments.
     *
     * This variable can only be modified internally via specific methods, maintaining control over state changes.
     */
    @get:JvmName("prompt")
    override var prompt: Prompt

    /**
     * Updates the current `AIAgentLLMContext` with a new prompt and ensures thread-safe access using a read lock.
     *
     * @param prompt The new [Prompt] to be set for the context.
     */
    public

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] McpToolRegistryProvider.kt · facilitates · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 2] McpToolRegistryProvider.kt · facilitates · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 3] McpToolRegistryProvider.kt · facilitates · line 78 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 4] AIAgentLLMContext.kt · AIAgentLLMContext · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
[ИСТОЧНИК 5] AIAgentLLMContext.kt · AIAgentLLMContext · line 59 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt


---
## Query 9: "какие возможности есть по выстраиванию пайпланов и разбитие задач на подзадачи"
**Optimized:** "optimizing build plans and breaking down tasks into subtasks in kotlin"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=181ms, retrieve=47ms, filter=0ms, rerank=2316ms, top_k=0ms, pack=5ms
**Top score:** 0,85 | Avg score: 0,72

### RAG Context:
Found 3 relevant class(es) | ~9124 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ActionBuilderApi
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/goap/ActionBuilderApi.kt
Score: 0,850
Responsibility: API for building Action instances with various properties and methods.
Key methods: name(name: String), description(description: String?), precondition(precondition: Condition<State>), belief(belief: Belief<State>), build()

--- ActionBuilderApi [ИСТОЧНИК 1] (line 1) ---
// File: ActionBuilderApi.kt
package ai.koog.agents.planner.goap

/**
 * API for building [Action] instances.
 */
public interface ActionBuilderApi<State> {
    /**
     * Sets the name of the action.
     */
    public fun name(name: String): ActionBuilderApi<State>

    /**
     * Sets the description of the action.
     */
    public fun description(description: String?): ActionBuilderApi<State>

    /**
     * Sets the precondition for the action.
     */
    public fun precondition(precondition: Condition<State>): ActionBuilderApi<State>

    /**
     * Sets the belief for the action.
     */
    public fun belief(belief: Belief<State>): ActionBuilderApi<State>

    /**
     * Sets the cost function for the action.
     */
    public fun cost(cost: Cost<State>): ActionBuilderApi<State>

    /**
     * Sets the execute function for the action.
     */
    public fun execute(execute: Execute<State>): ActionBuilderApi<State>

    /**
     * Builds the [Action].
     */
    public fun build(): Action<State>
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentPlanner
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt
Score: 0,800
Responsibility: Abstract base class for AI agent planners, responsible for building and executing plans until completion.
Key methods: buildPlan(context: AIAgentPlannerContext, state: State, plan: Plan?), executeStep(context: AIAgentPlannerContext, state: State, plan: Plan), isPlanCompleted(context: AIAgentPlannerContext, state: State, plan: Plan), execute(context: AIAgentPlannerContext, input: State)

--- AIAgentPlanner [ИСТОЧНИК 2] (line 1) ---
// File: AIAgentPlanner.kt
package ai.koog.agents.planner

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AIAgentPlannerContext
import ai.koog.agents.core.agent.context.with
import ai.koog.agents.core.agent.exception.AIAgentMaxNumberOfIterationsReachedException
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.serialization.TypeToken
import ai.koog.serialization.typeToken
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * An abstract base planner component, which can be used to implement different types of AI agent planner execution flows.
 *
 * An entry point is an [execute] method, which accepts an initial arbitrary [State] and returns the final [State] after the execution.
 *
 * Planner flow works as follows:
 * 1. Build a plan: [buildPlan]
 * 2. Execute a step in the plan: [executeStep]
 * 3. Repeat steps 1 and 2 until the plan is considered completed. Then the final [State] is returned.
 *
 * @param stateType [TypeToken] of the [State].
 */
public abstract class AIAgentPlanner<State : Any, Plan : Any>(
    // FIXME: require the type explicitly when we decide, what to do with it in Java API
    stateType: TypeToken? = null,
) {
    /**
     * [TypeToken] of the [State]
     */
    public val stateType: TypeToken = stateType ?: typeToken<Any?>().also {
        logger.warn { "State type is not specified, some agent features might behave unexpectedly." }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    /**
     * Builds a plan
     */
    protected abstract suspend fun buildPlan(
        context: AIAgentPlannerContext,
        state: State,
        plan: Plan?
    ): Plan

    /**
     * Executes a step in the plan.
     */
    protected abstract suspend fun executeStep(
        context: AIAgentPlannerContext,
        state: State,
        plan: Plan
    ): State

    /**
     * Checks if the plan is completed.
     */
    protected abstract suspend fun isPlanC

--- AIAgentPlanner [ИСТОЧНИК 3] (line 42) ---
// File: AIAgentPlanner.kt
 buildPlan(
        context: AIAgentPlannerContext,
        state: State,
        plan: Plan?
    ): Plan

    /**
     * Executes a step in the plan.
     */
    protected abstract suspend fun executeStep(
        context: AIAgentPlannerContext,
        state: State,
        plan: Plan
    ): State

    /**
     * Checks if the plan is completed.
     */
    protected abstract suspend fun isPlanCompleted(
        context: AIAgentPlannerContext,
        state: State,
        plan: Plan
    ): Boolean

    /**
     * Executes the main loop for the planner, which involves building and executing plans iteratively until
     * the plan is considered successfully completed or a max number of iterations is reached.
     *
     * @param context AI Agent's context
     * @param input The initial state to be used as the starting point for the execution process.
     * @return The final state after the execution of the plans.
     * @throws AIAgentMaxNumberOfIterationsReachedException If the maximum number of iterations defined in the agent's
     * configuration is exceeded.
     */
    @OptIn(InternalAgentsApi::class)
    internal suspend fun execute(
        context: AIAgentPlannerContext,
        input: State
    ): State {
        logger.debug { formatLog(context, "Starting planner execution") }
        var state = input
        var previousPlan: Plan? = null

        while (true) {
            val stepIndex = context.stateManager.withStateLock { state ->
                state.iterations
            }

            val plan = context.with(partName = "buildPlan-${stepIndex + 1}") { executionInfo, eventId ->
                context.pipeline.onPlanCreationStarting(eventId, executionInfo, context, state, previousPlan, stepIndex + 1)
                val newPlan = buildPlan(context, state, previousPlan)
                context.pipeline.onPlanCreationCompleted(eventId, executionInfo, context, state, newPlan, stepIndex + 1)
                newPlan
            }

            logge

--- AIAgentPlanner [ИСТОЧНИК 4] (line 90) ---
// File: AIAgentPlanner.kt
{ executionInfo, eventId ->
                context.pipeline.onPlanCreationStarting(eventId, executionInfo, context, state, previousPlan, stepIndex + 1)
                val newPlan = buildPlan(context, state, previousPlan)
                context.pipeline.onPlanCreationCompleted(eventId, executionInfo, context, state, newPlan, stepIndex + 1)
                newPlan
            }

            logger.debug { formatLog(context, "Executing plan step #${stepIndex + 1}") }

            // Execute step
            context.with(partName = "executeStep-${stepIndex + 1}") { stepExecutionInfo, stepEventId ->
                context.pipeline.onStepExecutionStarting(stepEventId, stepExecutionInfo, context, state, plan, stepIndex + 1)
                state = executeStep(context, state, plan)
                context.pipeline.onStepExecutionCompleted(stepEventId, stepExecutionInfo, context, state, plan, stepIndex + 1)
            }

            logger.debug { formatLog(context, "Finished executing plan step #${stepIndex + 1}") }

            // Check if plan is completed
            val isCompleted = context.with(partName = "isPlanCompleted-${stepIndex + 1}") { executionInfo, eventId ->
                context.pipeline.onPlanCompletionEvaluationStarting(eventId, executionInfo, context, state, plan, stepIndex + 1)
                val completed = isPlanCompleted(context, state, plan)
                context.pipeline.onPlanCompletionEvaluationCompleted(eventId, executionInfo, context, state, plan, completed, stepIndex + 1)
                completed
            }

            context.stateManager.withStateLock { state ->
                if (++state.iterations > context.config.maxAgentIterations) {
                    logger.error {
                        formatLog(
                            context,
                            "Max iterations limit (${context.config.maxAgentIterations}) reached"
                        )
                    }
                    throw AIAgentMaxNumb

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphExt
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
Score: 0,500
Responsibility: Manages subgraphs and tasks in a controlled, structured manner.
Key methods: nodeLLMRequestMultiple(), setToolChoiceRequired(), toSafeResult(), toolResultKindToJSON()

--- providing [ИСТОЧНИК 5] (line 588) ---
// File: AIAgentSubgraphExt.kt
m Input the type of input data for the subgraph.
 * @param Output the type of output data from the finish tool.
 * @param OutputTransformed the transformed type of the output data after processing by the finish tool.
 * @param finishTool the tool used to signify task completion and process task finalization.
 * @param runMode the mode in which tools are executed, e.g., parallel or sequential execution.
 * @param assistantResponseRepeatMax the maximum number of assistant responses allowed before
 *        determining that the task cannot be completed. If not provided, a default is used.
 * @param defineTask a suspend function defining the task description, executed within the
 *        context of an AI agent graph and based on the given input data.
 */
@InternalAgentsApi
public fun <Input, Output, OutputTransformed> AIAgentSubgraphBuilderBase<Input, OutputTransformed>.setupSubgraphWithTask(
    finishTool: Tool<Output, OutputTransformed>,
    inputType: TypeToken,
    outputTransformedType: TypeToken,
    runMode: ToolCalls,
    assistantResponseRepeatMax: Int? = null,
    defineTask: suspend AIAgentGraphContextBase.(Input) -> String
) {
    val originalToolsKey = createStorageKey<List<ToolDescriptor>>("all-available-tools")
    val askAssistantToFinishCounterKey = createStorageKey<Int>("ask-assistant-to-finish-counter")

    val maxAssistantResponses = assistantResponseRepeatMax ?: SubgraphWithTaskUtils.ASSISTANT_RESPONSE_REPEAT_MAX

    val setupTask by node<Input, String>(inputType = inputType, outputType = typeToken<String>()) { input ->
        llm.writeSession {
            // Save tools to restore after the subgraph is finished
            storage.set(originalToolsKey, tools)

            // Append finish tool to tools if it's not present yet
            if (finishTool.descriptor !in tools) {
                this.tools += finishTool.descriptor
            }

            // Model must always call tools in the loop until it decides (via finish tool)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] ActionBuilderApi.kt · ActionBuilderApi · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/goap/ActionBuilderApi.kt
[ИСТОЧНИК 2] AIAgentPlanner.kt · AIAgentPlanner · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt
[ИСТОЧНИК 3] AIAgentPlanner.kt · AIAgentPlanner · line 42 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt
[ИСТОЧНИК 4] AIAgentPlanner.kt · AIAgentPlanner · line 90 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt
[ИСТОЧНИК 5] AIAgentSubgraphExt.kt · providing · line 588 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt


---
## Query 10: "как агенты могу общаться друг с другом"
**Optimized:** "kotlin agents communicate each other methods"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=142ms, retrieve=48ms, filter=0ms, rerank=2715ms, top_k=0ms, pack=3ms
**Top score:** 0,85 | Avg score: 0,72

### RAG Context:
Found 3 relevant class(es) | ~7036 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentStrategies
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
Score: 0,850
Responsibility: Creates and configures a chat interaction process using AI agent graph strategies.
Key methods: chatAgentStrategy(), giveFeedbackToCallTools(input: String)

--- chatAgentStrategy [ИСТОЧНИК 1] (line 31) ---
// File: AIAgentStrategies.kt
 user input, execute tools, and provide responses.
 * Allows the agent to interact with the user in a chat-like manner.
 */
public fun chatAgentStrategy(): AIAgentGraphStrategy<String, String> = strategy("chat") {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by nodeLLMSendToolResult("nodeSendToolResult")

    val giveFeedbackToCallTools by node<String, Message.Response> { input ->
        llm.writeSession {
            appendPrompt {
                user(
                    "Don't chat with plain text! Call one of the available tools, instead: ${tools.joinToString(", ") {
                        it.name
                    }}"
                )
            }

            requestLLM()
        }
    }

    edge(nodeStart forwardTo nodeCallLLM)

    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo giveFeedbackToCallTools onAssistantMessage { true })

    edge(giveFeedbackToCallTools forwardTo giveFeedbackToCallTools onAssistantMessage { true })
    edge(giveFeedbackToCallTools forwardTo nodeExecuteTool onToolCall { true })

    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(
        nodeSendToolResult forwardTo nodeFinish onToolCall { tc -> tc.tool == "__exit__" } transformed
            { "Chat finished" }
    )
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}

/**
 * Creates a ReAct AI agent strategy that alternates between reasoning and execution stages
 * to dynamically process tasks and request outputs from an LLM.
 *
 * @param reasoningInterval Specifies the interval for reasoning steps.
 * @return An instance of [AIAgentGraphStrategy] that defines the ReAct strategy.
 *
 *
 * +-------+             +---------------+             +---------------+             +--------+
 * | Start | ----------> | CallLLMReas

--- chatAgentStrategy [ИСТОЧНИК 2] (line 217) ---
// File: AIAgentStrategies.kt
ut>("structured_output_with_tools_strategy") {
    val setStructuredOutput by nodeSetStructuredOutput<Input, Output>(config = config)
    val transformInput by node<Input, String> { transform(it) }
    val callLLM by nodeLLMRequestMultiple()
    val executeTools by nodeExecuteMultipleTools(parallelTools = parallelTools)
    val sendToolResult by nodeLLMSendMultipleToolResults()
    val transformToStructuredOutput by node<Message.Assistant, Output> { response ->
        llm.writeSession {
            parseResponseToStructuredResponse(response, config, fixingParser).data
        }
    }

    // Set the structured output, get the input and then call the llm
    nodeStart then setStructuredOutput then transformInput then callLLM

    // On tools
    edge(callLLM forwardTo executeTools onMultipleToolCalls { true })
    edge(executeTools forwardTo sendToolResult)

    // On assistant messages
    edge(
        callLLM forwardTo transformToStructuredOutput
            onMultipleAssistantMessages { true }
            transformed { it.single() }
    )

    // Post tool result
    edge(sendToolResult forwardTo executeTools onMultipleToolCalls { true })
    edge(
        sendToolResult forwardTo transformToStructuredOutput
            onMultipleAssistantMessages { true }
            transformed { it.first() }
    )

    // Finish
    transformToStructuredOutput then nodeFinish
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentEnvironment
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
Score: 0,700
Responsibility: Provides a mechanism for AI agents to interface with an external environment, offering methods for tool execution, error reporting, and sending termination messages.
Key methods: executeTool(toolCall: Message.Tool.Call), reportProblem(exception: Throwable), executeTools(toolCalls: List<Message.Tool.Call>)

--- with [ИСТОЧНИК 3] (line 1) ---
// File: AIAgentEnvironment.kt
package ai.koog.agents.core.environment

import ai.koog.prompt.message.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

/**
 * AIAgentEnvironment provides a mechanism for AI agents to interface with an external environment.
 * It offers methods for tool execution, error reporting, and sending termination messages.
 */
public interface AIAgentEnvironment {

    /**
     * Executes a tool call and returns its result.
     *
     * @param toolCall A tool call messages to be executed. A message contains details about the tool,
     *        its identifier, the request content, and associated metadata.
     * @return A result corresponding to the executed tool call. The result includes details such as
     *         the tool name, identifier, response content, and associated metadata.
     */
    public suspend fun executeTool(toolCall: Message.Tool.Call): ReceivedToolResult

    /**
     * Reports a problem that occurred within the environment.
     *
     * This method is used to handle exceptions or other issues encountered during
     * the execution of operations within the AI agent environment. The provided exception
     * describes the nature of the problem.
     *
     * @param exception The exception representing the problem to report.
     */
    public suspend fun reportProblem(exception: Throwable)

    /**
     * Executes a batch of tool calls within the AI agent environment and processes their results.
     *
     * This method takes a list of tool call messages, processes them by sending appropriate requests
     * to the underlying environment, and returns a list of results corresponding to the tool calls.
     *
     * @param toolCalls A list of tool call messages to be executed. Each message contains details
     *        about the tool, its identifier, the request content, and associated metadata.
     * @return A list of results corresponding to the executed tool calls. Each result

--- with [ИСТОЧНИК 4] (line 38) ---
// File: AIAgentEnvironment.kt
iate requests
     * to the underlying environment, and returns a list of results corresponding to the tool calls.
     *
     * @param toolCalls A list of tool call messages to be executed. Each message contains details
     *        about the tool, its identifier, the request content, and associated metadata.
     * @return A list of results corresponding to the executed tool calls. Each result includes details
     *         such as the tool name, identifier, response content, and metadata.
     */
    public suspend fun executeTools(toolCalls: List<Message.Tool.Call>): List<ReceivedToolResult> {
        val results = supervisorScope {
            toolCalls
                .map { toolCall ->
                    async { executeTool(toolCall) }
                }
                .awaitAll()
        }

        return results
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStrategy.kt
Score: 0,650
Responsibility: Defines how an AI agent processes input to produce output.
Key methods: execute(context: TContext, input: TInput)

--- representing [ИСТОЧНИК 5] (line 37) ---
// File: AIAgentStrategy.kt
ntext The execution context in which the AI agent operates. It provides access
     * to the agent's configuration, pipeline, environment, and other components required for
     * execution in a graph-based structure.
     * @param input The input data to be processed by the AI agent's strategy. The type of input
     * is defined by the strategy's implementation and is used to derive the resulting output.
     * @return The output produced by the AI agent's strategy, or null if no output is generated.
     * The output type is defined by the strategy's implementation.
     */
    public suspend fun execute(context: TContext, input: TInput): TOutput?
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentStrategies.kt · chatAgentStrategy · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 2] AIAgentStrategies.kt · chatAgentStrategy · line 217 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 3] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
[ИСТОЧНИК 4] AIAgentEnvironment.kt · with · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
[ИСТОЧНИК 5] AIAgentStrategy.kt · representing · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStrategy.kt


---
## Query 11: "какие возможности по работе с сессиями"
**Optimized:** "session management techniques in kotlin"
**Metrics:** Retrieved: 15 → Filtered: 15 → Final: 5
**Timings:** query_optimize=126ms, retrieve=50ms, filter=0ms, rerank=3202ms, top_k=0ms, pack=4ms
**Top score:** 0,90 | Avg score: 0,83

### RAG Context:
Found 3 relevant class(es) | ~10160 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMActions
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
Score: 0,900
Responsibility: Manages and manipulates message history in AI Agent LLM sessions.
Key methods: clearHistory(), leaveLastNMessages(n: Int, preserveSystemMessages: Boolean), dropLastNMessages(n: Int, preserveSystemMessages: Boolean), leaveMessagesFromTimestamp(timestamp: Instant, preserveSystemMessages: Boolean), setToolChoice(toolChoice: LLMParams.ToolChoice?)

--- AIAgentLLMWriteSession [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentLLMActions.kt
package ai.koog.agents.core.dsl.extension

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import kotlin.time.Instant

/**
 * Clears the history of messages in the current AI Agent LLM Write Session.
 *
 * This method resets the message history by setting it to an empty list.
 * It is useful when you want to start a new conversation or reset the session's context.
 */
public fun AIAgentLLMWriteSession.clearHistory() {
    prompt = prompt.withMessages { emptyList() }
}

/**
 * Keeps only the last N messages in the session's prompt by removing all earlier messages.
 *
 * @param n The number of most recent messages to retain in the session's prompt.
 */
public fun AIAgentLLMWriteSession.leaveLastNMessages(n: Int, preserveSystemMessages: Boolean = true) {
    prompt = prompt.withMessages {
        val thresholdIndex = it.size - n
        it.filterIndexed { index, message ->
            index >= thresholdIndex || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Removes the last `n` messages from the current prompt in the write session.
 *
 * @param n The number of messages to remove from the end of the current message list.
 */
public fun AIAgentLLMWriteSession.dropLastNMessages(n: Int, preserveSystemMessages: Boolean = true) {
    prompt = prompt.withMessages {
        val thresholdIndex = it.size - n
        it.filterIndexed { index, message ->
            index < thresholdIndex || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Removes all messages from the current session's prompt that have a timestamp
 * earlier than the specified timestamp.
 *
 * @param timestamp The threshold timestamp. Messages with a timestamp earlier than this will be removed.
 */
public fun AIAgentLLMWriteSession.leaveMessagesFromTimestamp(
    timestamp: Instant,
    preserveSystemMessages: Boolean = true
) {
    prompt = prompt.withMessag

--- AIAgentLLMWriteSession [ИСТОЧНИК 2] (line 46) ---
// File: AIAgentLLMActions.kt
**
 * Removes all messages from the current session's prompt that have a timestamp
 * earlier than the specified timestamp.
 *
 * @param timestamp The threshold timestamp. Messages with a timestamp earlier than this will be removed.
 */
public fun AIAgentLLMWriteSession.leaveMessagesFromTimestamp(
    timestamp: Instant,
    preserveSystemMessages: Boolean = true
) {
    prompt = prompt.withMessages {
        it.filter { message ->
            message.metaInfo.timestamp >= timestamp || (preserveSystemMessages && message is Message.System)
        }
    }
}

/**
 * Sets the [ai.koog.prompt.params.LLMParams.ToolChoice] for this LLM session.
 */
public fun AIAgentLLMWriteSession.setToolChoice(toolChoice: LLMParams.ToolChoice?) {
    prompt = prompt.withUpdatedParams { this.toolChoice = toolChoice }
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.Auto] to make LLM automatically decide between calling tools and generating text
 */
public fun AIAgentLLMWriteSession.setToolChoiceAuto() {
    setToolChoice(LLMParams.ToolChoice.Auto)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.Required] to make LLM always call tools
 */
public fun AIAgentLLMWriteSession.setToolChoiceRequired() {
    setToolChoice(LLMParams.ToolChoice.Required)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.None] to make LLM never call tools
 */
public fun AIAgentLLMWriteSession.setToolChoiceNone() {
    setToolChoice(LLMParams.ToolChoice.None)
}

/**
 * Set the [ai.koog.prompt.params.LLMParams.ToolChoice] to [ai.koog.prompt.params.LLMParams.ToolChoice.None] to make LLM call one specific tool [toolName]
 */
public fun AIAgentLLMWriteSession.setToolChoiceNamed(toolName: String) {
    setToolChoice(LLMParams.ToolChoice.Named(toolName))
}

/**
 * Unset the [ai.koog.prompt.params.LLMParams.ToolChoice].
 * Mostly, if left unsp

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMWriteSessionImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionImpl.kt
Score: 0,850
Responsibility: Manages LLM write sessions for AI agents.
Key methods: findTool(toolClass: KClass<out Tool<TArgs, TResult>>), appendPrompt(body: PromptBuilder.())

--- AIAgentLLMWriteSessionImpl [ИСТОЧНИК 3] (line 1) ---
// File: AIAgentLLMWriteSessionImpl.kt
@file:OptIn(InternalAgentsApi::class)

package ai.koog.agents.core.agent.session

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.utils.ActiveProperty
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.PromptBuilder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.processor.ResponseProcessor
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.structure.StructureDefinition
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.StructuredResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Clock

@PublishedApi
internal class AIAgentLLMWriteSessionImpl internal constructor(
    override val environment: AIAgentEnvironment,
    private val executor: PromptExecutor,
    tools: List<ToolDescriptor>,
    override val toolRegistry: ToolRegistry,
    prompt: Prompt,
    model: LLModel,
    responseProcessor: ResponseProcessor?,
    override val config: AIAgentConfig,
    override val clock: Clock,
) : AIAgentLLMWriteSessionAPI {
    private val readSessionImpl
        get() = AIAgentLLMReadSessionImpl(executor, tools, prompt, model, responseProcessor, config, isActive)

    override var prompt: Pro

--- AIAgentLLMWriteSessionImpl [ИСТОЧНИК 4] (line 173) ---
// File: AIAgentLLMWriteSessionImpl.kt
rializer: KSerializer<T>,
        examples: List<T>,
        fixingParser: StructureFixingParser?
    ): Result<StructuredResponse<T>> {
        return readSessionImpl.requestLLMStructured(serializer, examples, fixingParser).also {
            it.onSuccess { response ->
                appendPrompt {
                    message(response.message)
                }
            }
        }
    }

    override suspend fun <T> parseResponseToStructuredResponse(
        response: Message.Assistant,
        config: StructuredRequestConfig<T>,
        fixingParser: StructureFixingParser?
    ): StructuredResponse<T> {
        return readSessionImpl.parseResponseToStructuredResponse(response, config)
    }

    override suspend fun requestLLMMultipleChoices(): List<LLMChoice> {
        return readSessionImpl.requestLLMMultipleChoices()
    }

    override suspend fun requestLLMStreaming(definition: StructureDefinition?): Flow<StreamFrame> {
        if (definition != null) {
            val prompt = prompt(prompt, clock) {
                user {
                    definition.definition(this)
                }
            }
            this.prompt = prompt
        }

        return readSessionImpl.requestLLMStreaming()
    }

    @PublishedApi
    internal inline fun <reified TArgs, reified TResult> Flow<TArgs>.toParallelToolCallsImpl(
        safeTool: SafeTool<TArgs, TResult>,
        concurrency: Int = 16
    ): Flow<SafeTool.Result<TResult>> = flatMapMerge(concurrency) { args ->
        flow {
            emit(safeTool.execute(args, config.serializer))
        }
    }

    @PublishedApi
    internal inline fun <reified TArgs, reified TResult> Flow<TArgs>.toParallelToolCallsRawImpl(
        safeTool: SafeTool<TArgs, TResult>,
        concurrency: Int = 16
    ): Flow<String> = flatMapMerge(concurrency) { args ->
        flow {
            emit(safeTool.execute(args, config.serializer).content)
        }
    }

    @PublishedApi
    internal inline fun <reified TArgs, reifi

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMReadSession
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMReadSession.kt
Score: 0,750
Responsibility: Manages a read-only session with an AI agent using a language model.
Key methods: requestLLMMultipleWithoutTools(), requestLLMWithoutTools(), requestLLMOnlyCallingTools(), requestLLMMultipleOnlyCallingTools(), requestLLMForceOneTool(tool: ToolDescriptor)

--- specifying [ИСТОЧНИК 5] (line 1) ---
// File: AIAgentLLMReadSession.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.session

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.processor.ResponseProcessor
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.StructuredResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

/**
 * Represents a session for interacting with a language model (LLM) in a read-only context within an AI agent setup.
 * This session is configured with a set of tools, an executor for handling prompt execution, a prompt definition,
 * a language model, and specific session configurations.
 *
 * @constructor Internal constructor to initialize a new read session for the AI agent.
 * @param tools A list of tool descriptors that define the tools available for this session.
 * @param executor The `PromptExecutor` responsible for handling execution of prompts within this session.
 * @param prompt The `Prompt` object specifying the input messages and parameters for the session.
 * @param model The language model instance to be used for processing prompts in this session.
 * @param responseProcessor The response processor instance to be used for post-processing responses.
 * @param config The configuration settings for the AI agent session.
 */
public expect class AIAgentLLMReadSession internal constructor(
    tools: List<ToolDescriptor>,
    executor: PromptExecutor,
    prompt: Prompt,
    model: LLModel,
    responseProcessor: ResponseProcessor?,
    confi

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
[ИСТОЧНИК 2] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
[ИСТОЧНИК 3] AIAgentLLMWriteSessionImpl.kt · AIAgentLLMWriteSessionImpl · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionImpl.kt
[ИСТОЧНИК 4] AIAgentLLMWriteSessionImpl.kt · AIAgentLLMWriteSessionImpl · line 173 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionImpl.kt
[ИСТОЧНИК 5] AIAgentLLMReadSession.kt · specifying · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMReadSession.kt


---
## Query 12: "как реализован state managment"
**Optimized:** "kotlin state management implementation"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=129ms, retrieve=47ms, filter=0ms, rerank=2253ms, top_k=0ms, pack=5ms
**Top score:** 0,85 | Avg score: 0,64

### RAG Context:
Found 3 relevant class(es) | ~8812 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentState
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
Score: 0,850
Responsibility: Represents the state of an AI agent.
Key methods: copy(), close()

--- provides [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentState.kt
package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.annotation.InternalAgentsApi

/**
 * Represents the state of an AI agent during its lifecycle.
 *
 * This sealed interface provides different states to reflect whether the agent
 * has not started, is currently running, has completed its task successfully with a result,
 * or has failed with an exception.
 */
public sealed interface AIAgentState<Output> {
    /**
     * Creates and returns a copy of the current state object.
     *
     * @return A new instance of `AIAgentState<Output>` that is a copy of the current object.
     */
    public fun copy(): AIAgentState<Output>

    /**
     * Represents a state that indicates an action or process has not yet started.
     *
     * This class is part of the `State` sealed interface and is used to define
     * a specific state where no progress, execution, or processing has occurred.
     */
    public class NotStarted<Output> : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = NotStarted()
    }

    /**
     * Represents the starting state of an operation or process.
     *
     * This class is a specialization of the `State` class, indicating the initial
     * state prior to progression or change. It overrides the `copy` method to
     * return a new instance of the same starting state.
     *
     * @param Output The type of output associated with the state.
     */
    public class Starting<Output> : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = Starting()
    }

    /**
     * Represents the `Running` state of an AI agent, indicating that the agent is actively executing its tasks.
     *
     * This state provides access to the root context of the agent via the `rootContext` property, allowing
     * interaction with the overall execution environment, configuration, and state management facilities.
     *
     * The `rootContext` is marked wit

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentRunSessionImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
Score: 0,700
Responsibility: Manages the lifecycle of an AI agent's execution.
Key methods: pipeline(), context(), run(input: Input)

--- handles [ИСТОЧНИК 2] (line 1) ---
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

--- handles [ИСТОЧНИК 3] (line 33) ---
// File: AIAgentRunSessionImpl.kt
ce used for logging execution details and errors.
 * @property agent the AI agent instance being executed in this session.
 * @property strategy the execution strategy that defines how the agent processes input and produces output.
 */
internal class AIAgentRunSessionImpl<Input, Output, TContext : AIAgentContext>(
    private val id: String,
    private val logger: KLogger,
    private val agent: AIAgent<Input, Output>,
    private val strategy: AIAgentStrategy<Input, Output, TContext>,
    private val sessionPipeline: AIAgentPipeline,
    private val ctxBuilder: suspend (Input, String, String) -> TContext
) : AIAgentRunSession<Input, Output, TContext> {
    private var state: AIAgentState<Output> = NotStarted()

    override fun pipeline(): AIAgentPipeline = sessionPipeline

    private var ctx: TContext? = null

    override fun context(): TContext = ctx
        ?: error("Context is not available before running the session. Call run() to start the session and initialize the context.")

    override suspend fun run(
        input: Input
    ): Output {
        state = AIAgentState.Starting()
        val context = ctxBuilder(input, id, agent.id)
        ctx = context
        val runResult = withPreparedPipeline(context, agent.id, sessionPipeline) {
            try {
                logger.debug { formatLog(id, id, "Starting agent execution") }
                sessionPipeline.onAgentStarting<Input, Output>(
                    agent.id,
                    context.executionInfo,
                    id,
                    agent,
                    context
                )

                val result = context.with(partName = strategy.name) { executionInfo, eventId ->
                    runCatchingCancellable {
                        state = AIAgentState.Running(context.parentContext ?: context)
                        context.pipeline.onStrategyStarting(eventId, executionInfo, strategy, context)
                        val result = strategy.execute(context = cont

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentBase
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt
Score: 0,600
Responsibility: Represents a single-use AI agent with state.
Key methods: id(), strategy(), pipeline()

--- representing [ИСТОЧНИК 4] (line 1) ---
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

--- representing [ИСТОЧНИК 5] (line 102) ---
// File: AIAgentBase.kt
oString()
        return AIAgentRunSessionImpl(runId, logger, this, strategy, pipeline, ::prepareContext)
    }

    /**
     * Prepares and initializes the agent context required to handle the given input and run ID.
     *
     * @param agentInput the input provided to the agent for processing.
     * @param runId a unique identifier representing the current execution or operation run.
     * @param eventId a unique identifier for agent-related events.
     * @return the initialized context specific to the agent setup for the provided input and run ID.
     */
    public abstract suspend fun prepareContext(agentInput: Input, runId: String, eventId: String): TContext
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentState.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
[ИСТОЧНИК 2] AIAgentRunSessionImpl.kt · handles · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 3] AIAgentRunSessionImpl.kt · handles · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 4] AIAgentBase.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt
[ИСТОЧНИК 5] AIAgentBase.kt · representing · line 102 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt


---
## Query 13: "как реализована обработка ошибок"
**Optimized:** "error handling implementation kotlin"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=122ms, retrieve=45ms, filter=0ms, rerank=2623ms, top_k=0ms, pack=5ms
**Top score:** 0,95 | Avg score: 0,88

### RAG Context:
Found 3 relevant class(es) | ~8604 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentError
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt
Score: 0,950
Responsibility: Encapsulates error details for AI agent errors.
Key methods: constructor(message: String, stackTrace: String, cause: String? = null)(message: String, stackTrace: String, cause: String?), constructor(throwable: Throwable)(throwable: Throwable)

--- provides [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentError.kt
package ai.koog.agents.core.feature.model

import kotlinx.serialization.Serializable

/**
 * Represents an error encountered by an AI agent, encapsulating error details such as
 * the message, stack trace, and an optional cause.
 *
 * This class provides essential information to understand and debug errors occurring
 * during the execution of AI agent strategies, tools, or nodes.
 *
 * Instances of this class can be created directly from a `Throwable`.
 *
 * @property message A human-readable description of the error. Defaults to "Unknown error"
 *           if not provided by the originating throwable.
 * @property stackTrace The stack trace of the error as a string, providing a detailed
 *           representation of where the error occurred.
 * @property cause The stack trace of the root cause if available, or null if no cause is set.
 *           This helps trace back the chain of exceptions leading to the current error.
 */
@Serializable
public data class AIAgentError(
    public val message: String,
    public val stackTrace: String,
    public val cause: String? = null
) {
    /**
     * Secondary constructor that allows creating an instance of the class using a [Throwable].
     *
     * @param throwable The [Throwable] from which the error message, stack trace, and cause will be retrieved.
     * The error message is derived from `throwable.message`, defaulting to "Unknown error" if null.
     * The stack trace is converted to a string using `throwable.stackTraceToString()`.
     * The cause is determined from `throwable.cause`, and its stack trace is converted to a string if not null.
     */
    public constructor(throwable: Throwable) : this(
        message = throwable.message ?: "Unknown error",
        stackTrace = throwable.stackTraceToString(),
        cause = throwable.cause?.stackTraceToString()
    )
}

/**
 * Converts a [Throwable] instance to an [AIAgentError].
 *
 * @return The generated [AIAgentError] containing detailed information about the

--- provides [ИСТОЧНИК 2] (line 33) ---
// File: AIAgentError.kt
a string if not null.
     */
    public constructor(throwable: Throwable) : this(
        message = throwable.message ?: "Unknown error",
        stackTrace = throwable.stackTraceToString(),
        cause = throwable.cause?.stackTraceToString()
    )
}

/**
 * Converts a [Throwable] instance to an [AIAgentError].
 *
 * @return The generated [AIAgentError] containing detailed information about the [Throwable].
 */
public fun Throwable.toAgentError(): AIAgentError = AIAgentError(this)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SafeTool
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt
Score: 0,900
Responsibility: Wraps a tool to safely execute it within an AI agent environment, handling results and errors.
Key methods: isSuccessful(), asSuccessful()

--- designed [ИСТОЧНИК 3] (line 1) ---
// File: SafeTool.kt
package ai.koog.agents.core.environment

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.serialization.JSONSerializer
import kotlin.time.Clock

/**
 * A wrapper class designed to safely execute a tool within a given AI agent environment.
 * It provides mechanisms for handling tool execution results and differentiating between
 * success and failure cases.
 *
 * @property tool The tool instance to be executed. Defines the operation and its required input/output behavior.
 * @property clock The clock used to determine tool call message timestamps
 * @property environment The environment in which the tool operates. Handles the execution of tool logic.
 */
public data class SafeTool<TArgs, TResult>(
    internal val tool: Tool<TArgs, TResult>,
    internal val environment: AIAgentEnvironment,
    internal val clock: Clock
) {
    /**
     * Represents a sealed interface for results, which can either be a success or a failure.
     *
     * @param TResult The type of the result
     */
    public sealed interface Result<TResult> {
        /**
         * Content of the result
         *
         * - In the [Success] case, this corresponds to the provided content of the successful result.
         * - In the ]Failure] case, this corresponds to the failure message.
         */
        public val content: String

        /**
         * Determines if the current result represents a successful operation.
         *
         * @return `true` if the result is an instance of [Success], otherwise `false`.
         */
        public fun isSuccessful(): Boolean = this is Success<TResult>

        /**
         * Determines whether the current instance represents a failure state.
         *
         * @return `true` if the current instance is of type [Failure], otherwise `false`.
         */
        public fun isFailure(): Boolean = this is Failure<TResult>

        /**
         * Casts the curre

--- designed [ИСТОЧНИК 4] (line 40) ---
// File: SafeTool.kt
lse`.
         */
        public fun isSuccessful(): Boolean = this is Success<TResult>

        /**
         * Determines whether the current instance represents a failure state.
         *
         * @return `true` if the current instance is of type [Failure], otherwise `false`.
         */
        public fun isFailure(): Boolean = this is Failure<TResult>

        /**
         * Casts the current instance of `Result` to a `Success` type if it is a successful result.
         *
         * @return The current instance cast to `Success<TResult>`.
         * @throws IllegalStateException if not [Success]
         */
        public fun asSuccessful(): Success<TResult> = when (this) {
            is Success<TResult> -> this
            is Failure<TResult> -> throw IllegalStateException("Result is not a success: $this")
        }

        /**
         * Casts the current object to a `Failure` type.
         *
         * This function assumes that the calling instance is of type `Failure<TResult>`.
         * Use it to retrieve the object as a `Failure` and access its specific properties and behaviors.
         *
         * @return The current instance cast to `Failure<TResult>`.
         * @throws IllegalStateException if not [Failure]
         */
        public fun asFailure(): Failure<TResult> = when (this) {
            is Success<TResult> -> throw IllegalStateException("Result is not a failure: $this")
            is Failure<TResult> -> this
        }

        /**
         * Represents a successful result of an operation, wrapping a specific tool result and its corresponding content.
         *
         * @param TResult The type of the tool result.
         * @property result The tool result
         * @property content The associated content describing or representing the result in string format.
         */
        public data class Success<TResult>(
            val result: TResult,
            override val content: String
        ) : Result<TResult>

        /**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentException
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/exception/AIAgentException.kt
Score: 0,850
Responsibility: Represents a custom exception for AI Agent-related processes, providing detailed error messages.
Key methods: AIAgentException(problem: String, throwable: Throwable?), AIAgentStuckInTheNodeException(node: AIAgentNodeBase<*, *>, output: Any?), AIAgentMaxNumberOfIterationsReachedException(maxNumberOfIterations: Int), AIAgentTerminationByClientException(message: String)

--- for [ИСТОЧНИК 5] (line 1) ---
// File: AIAgentException.kt
package ai.koog.agents.core.agent.exception

import ai.koog.agents.core.agent.entity.AIAgentNodeBase

// TODO: how it differs from AgentRuntimeException?

/**
 * Represents a custom exception class for use in AI Agent-related processes.
 *
 * This exception is thrown when the AI Agent encounters a specific problem
 * that requires handling or reporting. It extends the base `Exception` class
 * and provides a detailed message for easier identification of the issue.
 *
 * @constructor Creates an instance of `AgentException`.
 * @param problem Description of the problem encountered by the AI Agent.
 * @param throwable Optional cause of the exception, which can provide additional
 * context about the error.
 */
public open class AIAgentException(problem: String, throwable: Throwable? = null) :
    Exception("AI Agent has run into a problem: $problem", throwable)

/**
 * Exception thrown when an agent becomes stuck in a specific node during the execution
 * of the agent graph. This typically occurs when the output produced by the node does not
 * match any conditions on the available edges, preventing further progress in the graph execution.
 *
 * @param node The node in which the agent becomes stuck.
 * @param output The output produced by the node that doesn't match any edge conditions.
 */
public class AIAgentStuckInTheNodeException(node: AIAgentNodeBase<*, *>, output: Any?) :
    AIAgentException(
        "When executing agent graph, stuck in node ${node.name} " +
            "because output $output doesn't match any condition on available edges."
    )

/**
 * Exception thrown when an agent exceeds the maximum allowed number of iterations during execution.
 *
 * This exception indicates that the agent could not complete its task within the specified number
 * of steps, as defined by the `maxAgentIterations` parameter in the agent's configuration. To
 * resolve this, consider increasing the value of `maxAgentIterations` to accommodate more
 * iterations for the agent

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentError.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt
[ИСТОЧНИК 2] AIAgentError.kt · provides · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt
[ИСТОЧНИК 3] SafeTool.kt · designed · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt
[ИСТОЧНИК 4] SafeTool.kt · designed · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt
[ИСТОЧНИК 5] AIAgentException.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/exception/AIAgentException.kt


---
## Query 14: "какие механизмы работы с опасными tool calling"
**Optimized:** "kotlin dangerous tools handling mechanisms"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=135ms, retrieve=80ms, filter=0ms, rerank=2124ms, top_k=0ms, pack=5ms
**Top score:** 0,90 | Avg score: 0,77

### RAG Context:
Found 3 relevant class(es) | ~7636 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolException
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolException.kt
Score: 0,900
Responsibility: Represents exceptions specific to tools with a custom message.
Key methods: ValidationFailure(message: String), validate(expectation: Boolean, message: () -> String), validateNotNull(value: T?, message: () -> String), fail(message: String)

--- representing [ИСТОЧНИК 1] (line 1) ---
// File: ToolException.kt
package ai.koog.agents.core.tools

import kotlinx.serialization.Serializable

/**
 * A base sealed class representing exceptions specific to tools.
 * This class provides a structure for exceptions with a custom message.
 */
@Serializable
public sealed class ToolException(override val message: String) : Exception() {
    /**
     * Represents a failure that occurs during validation processes.
     *
     * This exception is a specific type of ToolException used to indicate
     * that validation of some input or process has failed. It typically
     * contains a message that provides more details about the validation failure.
     *
     * @constructor Creates a ValidationFailure instance with the given message.
     * @param message The detail message describing the validation error.
     */
    public class ValidationFailure(message: String) : ToolException(message)
}

/**
 * Validates a given condition and throws a [ToolException.ValidationFailure] exception if the condition is not met.
 *
 * @param expectation The condition that is expected to be true.
 * @param message A lambda function to generate the exception message if the condition is not met.
 */
public fun validate(expectation: Boolean, message: () -> String) {
    if (!expectation) throw ToolException.ValidationFailure(message())
}

/**
 * Validates that the provided value is not null. If the value is null,
 * a [ToolException.ValidationFailure] exception is thrown with the provided error message.
 *
 * @param value The value to be validated as not null.
 * @param message A lambda that provides the error message in case the value is null.
 * @return The same non-null value that was provided as input.
 * @throws ToolException.ValidationFailure if the value is null.
 */
public fun <T : Any> validateNotNull(value: T?, message: () -> String): T {
    if (value == null) throw ToolException.ValidationFailure(message())
    return value
}

/**
 * Throws a [ToolException.ValidationFailure] exception with the sp

--- representing [ИСТОЧНИК 2] (line 39) ---
// File: ToolException.kt
e in case the value is null.
 * @return The same non-null value that was provided as input.
 * @throws ToolException.ValidationFailure if the value is null.
 */
public fun <T : Any> validateNotNull(value: T?, message: () -> String): T {
    if (value == null) throw ToolException.ValidationFailure(message())
    return value
}

/**
 * Throws a [ToolException.ValidationFailure] exception with the specified error message.
 *
 * @param message The error message to include in the exception.
 * @return Nothing, as this function always throws an exception.
 */
public fun fail(message: String): Nothing = throw ToolException.ValidationFailure(message)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] McpToolDefinitionParser
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolDefinitionParser.kt
Score: 0,750
Responsibility: Parses MCP SDK tool definitions into standardized ToolDescriptor format.
Key methods: parse(sdkTool: SDKTool), parseParameters(properties: JsonObject), parseParameterType(element: JsonObject, depth: Int)

--- McpToolDescriptorParser [ИСТОЧНИК 3] (line 1) ---
// File: McpToolDefinitionParser.kt
package ai.koog.agents.mcp

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import io.modelcontextprotocol.kotlin.sdk.types.EmptyJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.modelcontextprotocol.kotlin.sdk.types.Tool as SDKTool

/**
 * Parsers tool definition from MCP SDK to our tool descriptor format.
 */
public interface McpToolDescriptorParser {
    /**
     * Parses an SDK tool representation into a standardized ToolDescriptor format.
     *
     * @param sdkTool The SDKTool instance containing tool information to be parsed.
     * @return The parsed ToolDescriptor, representing the tool in a standardized format.
     */
    public fun parse(sdkTool: SDKTool): ToolDescriptor
}

/**
 * Default implementation of [McpToolDescriptorParser].
 */
public object DefaultMcpToolDescriptorParser : McpToolDescriptorParser {
    // Maximum depth of recursive parsing
    private const val MAX_DEPTH = 30

    /**
     * Parses an MCP SDK Tool definition into tool descriptor format.
     *
     * This method extracts tool information (name, description, parameters) from an MCP SDK Tool
     * and converts it into a ToolDescriptor that can be used by the agent framework.
     *
     * @param sdkTool The MCP SDK Tool to parse.
     * @return A ToolDescriptor representing the MCP tool.
     */
    override fun parse(sdkTool: SDKTool): ToolDescriptor {
        // Parse all parameters from the input schema
        val parameters = parseParameters(sdkTool.inputSchema.properties ?: EmptyJsonObject)

        // Get the list of required parameters
        val requiredParameters = sdkTool.inputSchema.required ?: emptyList()

        // Cr

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentEnvironment
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
Score: 0,700
Responsibility: Provides a mechanism for AI agents to interface with an external environment, offering methods for tool execution, error reporting, and sending termination messages.
Key methods: executeTool(toolCall: Message.Tool.Call), reportProblem(exception: Throwable), executeTools(toolCalls: List<Message.Tool.Call>)

--- with [ИСТОЧНИК 4] (line 1) ---
// File: AIAgentEnvironment.kt
package ai.koog.agents.core.environment

import ai.koog.prompt.message.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

/**
 * AIAgentEnvironment provides a mechanism for AI agents to interface with an external environment.
 * It offers methods for tool execution, error reporting, and sending termination messages.
 */
public interface AIAgentEnvironment {

    /**
     * Executes a tool call and returns its result.
     *
     * @param toolCall A tool call messages to be executed. A message contains details about the tool,
     *        its identifier, the request content, and associated metadata.
     * @return A result corresponding to the executed tool call. The result includes details such as
     *         the tool name, identifier, response content, and associated metadata.
     */
    public suspend fun executeTool(toolCall: Message.Tool.Call): ReceivedToolResult

    /**
     * Reports a problem that occurred within the environment.
     *
     * This method is used to handle exceptions or other issues encountered during
     * the execution of operations within the AI agent environment. The provided exception
     * describes the nature of the problem.
     *
     * @param exception The exception representing the problem to report.
     */
    public suspend fun reportProblem(exception: Throwable)

    /**
     * Executes a batch of tool calls within the AI agent environment and processes their results.
     *
     * This method takes a list of tool call messages, processes them by sending appropriate requests
     * to the underlying environment, and returns a list of results corresponding to the tool calls.
     *
     * @param toolCalls A list of tool call messages to be executed. Each message contains details
     *        about the tool, its identifier, the request content, and associated metadata.
     * @return A list of results corresponding to the executed tool calls. Each result

--- with [ИСТОЧНИК 5] (line 38) ---
// File: AIAgentEnvironment.kt
iate requests
     * to the underlying environment, and returns a list of results corresponding to the tool calls.
     *
     * @param toolCalls A list of tool call messages to be executed. Each message contains details
     *        about the tool, its identifier, the request content, and associated metadata.
     * @return A list of results corresponding to the executed tool calls. Each result includes details
     *         such as the tool name, identifier, response content, and metadata.
     */
    public suspend fun executeTools(toolCalls: List<Message.Tool.Call>): List<ReceivedToolResult> {
        val results = supervisorScope {
            toolCalls
                .map { toolCall ->
                    async { executeTool(toolCall) }
                }
                .awaitAll()
        }

        return results
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] ToolException.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolException.kt
[ИСТОЧНИК 2] ToolException.kt · representing · line 39 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolException.kt
[ИСТОЧНИК 3] McpToolDefinitionParser.kt · McpToolDescriptorParser · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolDefinitionParser.kt
[ИСТОЧНИК 4] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
[ИСТОЧНИК 5] AIAgentEnvironment.kt · with · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt


---
## Query 15: "где хранится текущий диалог агента с пользователем и какие возможности по его управлению"
**Optimized:** "where-is-current-agent-user-dialog-stored-and-its-management-capabilities"
**Metrics:** Retrieved: 14 → Filtered: 14 → Final: 5
**Timings:** query_optimize=189ms, retrieve=45ms, filter=0ms, rerank=2895ms, top_k=0ms, pack=2ms
**Top score:** 0,85 | Avg score: 0,71

### RAG Context:
Found 2 relevant class(es) | ~8704 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentMemoryProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
Score: 0,850
Responsibility: Core interface for managing an agent's persistent memory system.
Key methods: save(fact: Fact, subject: MemorySubject, scope: MemoryScope), load(concept: Concept, subject: MemorySubject, scope: MemoryScope)

--- for [ИСТОЧНИК 1] (line 1) ---
// File: AgentMemoryProvider.kt
package ai.koog.agents.memory.providers

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

/**
 * Core interface for managing an agent's persistent memory system.
 * This interface defines the fundamental operations for storing and retrieving
 * knowledge in a structured, context-aware manner.
 *
 * Key features:
 * - Structured knowledge storage using concepts and facts
 * - Context-aware memory organization (subjects and scopes)
 * - Flexible storage backend support (local/remote)
 * - Semantic search capabilities
 *
 * Usage example:
 * ```
 * val provider: AgentMemoryProvider = LocalFileMemoryProvider(
 *     config = LocalMemoryConfig("memory"),
 *     storage = EncryptedStorage(fs, encryption),
 *     fs = JVMFileSystemProvider,
 *     root = basePath
 * )
 *
 * // Store project information
 * provider.save(
 *     fact = SingleFact(
 *         concept = Concept("build-system", "Project build configuration", FactType.SINGLE),
 *         timestamp = currentTime,
 *         value = "Gradle 8.0"
 *     ),
 *     subject = MemorySubject.Project,
 *     scope = MemoryScope.Product("my-app")
 * )
 *
 * // Retrieve environment information
 * val envFacts = provider.loadByDescription(
 *     description = "system environment",
 *     subject = MemorySubject.Machine,
 *     scope = MemoryScope.Agent("env-analyzer")
 * )
 * ```
 */
public interface AgentMemoryProvider {
    /**
     * Persists a fact in the agent's memory system.
     * This operation ensures:
     * - Atomic storage of the fact
     * - Proper scoping and subject categorization
     * - Consistent storage format
     *
     * @param fact Knowledge unit to store (can be SingleFact or MultipleFacts)
     * @param subject Context category (e.g., MACHINE, PROJECT)

--- for [ИСТОЧНИК 2] (line 50) ---
// File: AgentMemoryProvider.kt
interface AgentMemoryProvider {
    /**
     * Persists a fact in the agent's memory system.
     * This operation ensures:
     * - Atomic storage of the fact
     * - Proper scoping and subject categorization
     * - Consistent storage format
     *
     * @param fact Knowledge unit to store (can be SingleFact or MultipleFacts)
     * @param subject Context category (e.g., MACHINE, PROJECT)
     * @param scope Visibility boundary (e.g., Agent, Feature)
     * @throws IOException if storage operation fails
     */
    public suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope)

    /**
     * Retrieves facts associated with a specific concept.
     * This operation provides:
     * - Direct concept-based knowledge retrieval
     * - Context-aware fact filtering
     * - Ordered fact list (typically by timestamp)
     *
     * @param concept Knowledge category to retrieve
     * @param subject Context to search within
     * @param scope Visibility boundary to consider
     * @return List of matching facts, empty if none found
     */
    public suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact>

    /**
     * Retrieves all facts within a specific context.
     * This operation is useful for:
     * - Building comprehensive context understanding
     * - Memory analysis and debugging
     * - Data migration between storage backends
     *
     * @param subject Context to retrieve from
     * @param scope Visibility boundary to consider
     * @return All available facts in the context
     */
    public suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact>

    /**
     * Performs semantic search across stored facts.
     * This operation enables:
     * - Natural language queries
     * - Fuzzy concept matching
     * - Context-aware search results
     *
     * Implementation considerations:
     * - May use different matching algorithms
     * - Could integrate with LLM for better

--- for [ИСТОЧНИК 3] (line 137) ---
// File: AgentMemoryProvider.kt
ptional encryption support
 *
 * Usage example:
 * ```
 * val config = LocalMemoryConfig(
 *     storageDirectory = "agent-memory",
 *     defaultScope = MemoryScope.Agent("assistant")
 * )
 * ```
 *
 * @property storageDirectory Base directory for memory files
 * @property defaultScope Default visibility scope, typically agent-specific
 */
@Serializable
@SerialName("local")
public data class LocalMemoryConfig @JvmOverloads constructor(
    val storageDirectory: String,
    override val defaultScope: MemoryScope = MemoryScope.CrossProduct,
) : MemoryProviderConfig

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ChatMemory
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
Score: 0,700
Responsibility: Stores and loads conversation history between an agent and a user.
Key methods: createInitialConfig(agentConfig: AIAgentConfig), install(config: ChatMemoryConfig, pipeline: AIAgentGraphPipeline), install(config: ChatMemoryConfig, pipeline: AIAgentFunctionalPipeline), install(config: ChatMemoryConfig, pipeline: AIAgentPlannerPipeline), applyPreProcessors(messages: List<Message>, preProcessors: List<ChatMemoryPreProcessor>)

--- ChatMemory [ИСТОЧНИК 4] (line 1) ---
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

--- ChatMemory [ИСТОЧНИК 5] (line 46) ---
// File: ChatMemory.kt
oryConfig, ChatMemory>,
        AIAgentPlannerFeature<ChatMemoryConfig, ChatMemory> {

        override val key: AIAgentStorageKey<ChatMemory> =
            AIAgentStorageKey("agents-features-chat-memory")

        override fun createInitialConfig(
            agentConfig: AIAgentConfig,
        ): ChatMemoryConfig = ChatMemoryConfig()

        override fun install(
            config: ChatMemoryConfig,
            pipeline: AIAgentGraphPipeline,
        ): ChatMemory {
            val chatMemory = ChatMemory()
            installInternal(config, pipeline)
            return chatMemory
        }

        override fun install(
            config: ChatMemoryConfig,
            pipeline: AIAgentFunctionalPipeline,
        ): ChatMemory {
            val chatMemory = ChatMemory()
            installInternal(config, pipeline)
            return chatMemory
        }

        override fun install(
            config: ChatMemoryConfig,
            pipeline: AIAgentPlannerPipeline
        ): ChatMemory {
            val chatMemory = ChatMemory()
            installInternal(config, pipeline)
            return chatMemory
        }

        private fun applyPreProcessors(
            messages: List<Message>,
            preProcessors: List<ChatMemoryPreProcessor>,
        ): List<Message> {
            return preProcessors.fold(messages) { acc, processor -> processor.preprocess(acc) }
        }

        private fun installInternal(config: ChatMemoryConfig, pipeline: AIAgentPipeline) {
            pipeline.interceptStrategyStarting(this) {
                val history = config.chatHistoryProvider.load(it.context.runId)
                val processed = applyPreProcessors(history, config.preprocessors)

                it.context.llm.writeSession {
                    prompt = prompt.withMessages { processed }
                }
            }

            pipeline.interceptStrategyCompleted(this) {
                val history = it.context.llm.prompt.messages
                val proc

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 2] AgentMemoryProvider.kt · for · line 50 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 3] AgentMemoryProvider.kt · for · line 137 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 4] ChatMemory.kt · ChatMemory · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
[ИСТОЧНИК 5] ChatMemory.kt · ChatMemory · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt


---
## Query 16: "как устроена observability и работа с событиями от агентов "
**Optimized:** "observability architecture event handling agents kotlin"
**Metrics:** Retrieved: 10 → Filtered: 10 → Final: 5
**Timings:** query_optimize=140ms, retrieve=49ms, filter=0ms, rerank=1918ms, top_k=0ms, pack=4ms
**Top score:** 0,90 | Avg score: 0,85

### RAG Context:
Found 3 relevant class(es) | ~9320 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/agent/AgentEventContext.kt
Score: 0,900
Responsibility: Provides context for handling events specific to AI agents.
Key methods: AgentStartingContext(eventId: String, executionInfo: AgentExecutionInfo, agent: AIAgent<*, *>, runId: String, context: AIAgentContext), AgentCompletedContext(eventId: String, executionInfo: AgentExecutionInfo, agentId: String, runId: String, result: Any?, context: AIAgentContext), AgentErrorContext(eventId: String, executionInfo: AgentExecutionInfo, agentId: String, runId: String, throwable: Throwable?, context: AIAgentContext)

--- extends [ИСТОЧНИК 1] (line 1) ---
// File: AgentEventContext.kt
package ai.koog.agents.core.feature.handler.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.feature.handler.AgentLifecycleEventContext
import ai.koog.agents.core.feature.handler.AgentLifecycleEventType

/**
 * Provides the context for handling events specific to AI agents.
 * This interface extends the foundational event handling context, `EventHandlerContext`,
 * and is specialized for scenarios involving agents and their associated workflows or features.
 *
 * The `AgentEventHandlerContext` enables implementation of event-driven systems within
 * the AI Agent framework by offering hooks for custom event handling logic tailored to agent operations.
 */
public interface AgentEventContext : AgentLifecycleEventContext

/**
 * Represents the context available during the start of an AI agent.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property agent The AI agent associated with this context.
 * @property runId The identifier for the session in which the agent is being executed.
 * @property context The context associated with the agent's execution.
 */
public data class AgentStartingContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    public val agent: AIAgent<*, *>,
    public val runId: String,
    public val context: AIAgentContext,
) : AgentEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.AgentStarting
}

/**
 * Represents the context for handling the completion of an agent's execution.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property agentId The unique identifier of the agent that completed its execution.

--- extends [ИСТОЧНИК 2] (line 34) ---
// File: AgentEventContext.kt
ontext,
) : AgentEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.AgentStarting
}

/**
 * Represents the context for handling the completion of an agent's execution.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property agentId The unique identifier of the agent that completed its execution.
 * @property runId The identifier of the session in which the agent was executed.
 * @property result The optional result of the agent's execution, if available.
 * @property context
 */
public data class AgentCompletedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    public val agentId: String,
    public val runId: String,
    public val result: Any?,
    public val context: AIAgentContext,
) : AgentEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.AgentCompleted
}

/**
 * Represents the context for handling errors that occur during the execution of an agent run.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property agentId The unique identifier of the agent associated with the error.
 * @property runId The identifier for the session during which the error occurred.
 * @property throwable The exception or error thrown during the execution.
 */
public data class AgentExecutionFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val agentId: String,
    val runId: String,
    val throwable: Throwable,
    public val context: AIAgentContext,
) : AgentEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.AgentExecutionFailed
}

/**
 * Represents the context passed to the handler that is executed before an agent is closed.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @proper

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AgentLifecycleHandlersCollector
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
Score: 0,850
Responsibility: Collects and manages lifecycle event handlers associated with AI agents and features.
Key methods: addHandlerForFeature(featureKey: AIAgentStorageKey<*>, eventType: AgentLifecycleEventType, handler: AgentLifecycleEventHandler<TContext, TReturn>), getHandlersForEvent(eventType: AgentLifecycleEventType)

--- serves [ИСТОЧНИК 3] (line 1) ---
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

--- serves [ИСТОЧНИК 4] (line 36) ---
// File: AgentLifecycleHandlersCollector.kt
e[eventType]?.mapNotNull { handler ->
                @Suppress("UNCHECKED_CAST")
                handler as? AgentLifecycleEventHandler<TContext, TReturn>
            } ?: emptyList()
        }
    }

    private val featureToHandlersMap = mutableMapOf<AIAgentStorageKey<*>, FeatureEventHandlers>()

    internal fun <TContext : AgentLifecycleEventContext, TReturn : Any> addHandlerForFeature(
        featureKey: AIAgentStorageKey<*>,
        eventType: AgentLifecycleEventType,
        handler: AgentLifecycleEventHandler<TContext, TReturn>
    ) {
        featureToHandlersMap.getOrPut(featureKey) { FeatureEventHandlers(featureKey) }
            .addHandler(eventType, handler)
    }

    internal fun <TContext : AgentLifecycleEventContext, TReturn : Any> getHandlersForEvent(
        eventType: AgentLifecycleEventType
    ): Map<AIAgentStorageKey<*>, List<AgentLifecycleEventHandler<TContext, TReturn>>> {
        val handlers = featureToHandlersMap
            .mapValues { (_, featureHandlers) -> featureHandlers.getHandlers<TContext, TReturn>(eventType) }
            .filterValues { it.isNotEmpty() }

        return handlers
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] EventHandlerConfigImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-event-handler/src/commonMain/kotlin/ai/koog/agents/features/eventHandler/feature/EventHandlerConfigImpl.kt
Score: 0,800
Responsibility: Manages event handlers for various agent and LLM events.
Key methods: _setOnAgentStarting(handler: suspend (eventHandler: AgentStartingContext) -> Unit), _setOnAgentCompleted(handler: suspend (eventHandler: AgentCompletedContext) -> Unit), _setOnLLMCallStarting(handler: suspend (eventHandler: LLMCallStartingContext) -> Unit), _onAgentExecutionFailed(eventHandler: AgentExecutionFailedContext), _setOnNodeExecutionStarting(handler: suspend (eventHandler: NodeExecutionStartingContext) -> Unit)

--- EventHandlerConfigImpl [ИСТОЧНИК 5] (line 1) ---
// File: EventHandlerConfigImpl.kt
package ai.koog.agents.features.eventHandler.feature

import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.feature.handler.AfterLLMCallContext
import ai.koog.agents.core.feature.handler.AgentBeforeCloseContext
import ai.koog.agents.core.feature.handler.AgentFinishedContext
import ai.koog.agents.core.feature.handler.AgentRunErrorContext
import ai.koog.agents.core.feature.handler.AgentStartContext
import ai.koog.agents.core.feature.handler.BeforeLLMCallContext
import ai.koog.agents.core.feature.handler.NodeAfterExecuteContext
import ai.koog.agents.core.feature.handler.NodeBeforeExecuteContext
import ai.koog.agents.core.feature.handler.NodeExecutionErrorContext
import ai.koog.agents.core.feature.handler.StrategyFinishedContext
import ai.koog.agents.core.feature.handler.StrategyStartContext
import ai.koog.agents.core.feature.handler.ToolCallContext
import ai.koog.agents.core.feature.handler.ToolCallFailureContext
import ai.koog.agents.core.feature.handler.ToolCallResultContext
import ai.koog.agents.core.feature.handler.ToolValidationErrorContext
import ai.koog.agents.core.feature.handler.agent.AgentClosingContext
import ai.koog.agents.core.feature.handler.agent.AgentCompletedContext
import ai.koog.agents.core.feature.handler.agent.AgentExecutionFailedContext
import ai.koog.agents.core.feature.handler.agent.AgentStartingContext
import ai.koog.agents.core.feature.handler.llm.LLMCallCompletedContext
import ai.koog.agents.core.feature.handler.llm.LLMCallStartingContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionCompletedContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionFailedContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionStartingContext
import ai.koog.agents.core.feature.handler.strategy.StrategyCompletedContext
import ai.koog.agents.core.feature.handler.strategy.StrategyStartingContext
import ai.koog.agents.core.feature.handler.streaming.LLMStreamingCompletedContext
import ai.koog.agen

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AgentEventContext.kt · extends · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/agent/AgentEventContext.kt
[ИСТОЧНИК 2] AgentEventContext.kt · extends · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/agent/AgentEventContext.kt
[ИСТОЧНИК 3] AgentLifecycleHandlersCollector.kt · serves · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
[ИСТОЧНИК 4] AgentLifecycleHandlersCollector.kt · serves · line 36 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
[ИСТОЧНИК 5] EventHandlerConfigImpl.kt · EventHandlerConfigImpl · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-event-handler/src/commonMain/kotlin/ai/koog/agents/features/eventHandler/feature/EventHandlerConfigImpl.kt


---
## Query 17: "что такое GraphAIAgent, каковы его преимущества и схемы использования?"
**Optimized:** "what is graphaiaagent, its advantages, and usage scenarios"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=182ms, retrieve=48ms, filter=0ms, rerank=2701ms, top_k=0ms, pack=1ms
**Top score:** 0,85 | Avg score: 0,75

### RAG Context:
Found 2 relevant class(es) | ~9124 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentStrategies
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
Score: 0,850
Responsibility: Creates and configures a chat interaction process using AI agent graph strategies.
Key methods: chatAgentStrategy(), giveFeedbackToCallTools(input: String)

--- chatAgentStrategy [ИСТОЧНИК 1] (line 31) ---
// File: AIAgentStrategies.kt
 user input, execute tools, and provide responses.
 * Allows the agent to interact with the user in a chat-like manner.
 */
public fun chatAgentStrategy(): AIAgentGraphStrategy<String, String> = strategy("chat") {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by nodeLLMSendToolResult("nodeSendToolResult")

    val giveFeedbackToCallTools by node<String, Message.Response> { input ->
        llm.writeSession {
            appendPrompt {
                user(
                    "Don't chat with plain text! Call one of the available tools, instead: ${tools.joinToString(", ") {
                        it.name
                    }}"
                )
            }

            requestLLM()
        }
    }

    edge(nodeStart forwardTo nodeCallLLM)

    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo giveFeedbackToCallTools onAssistantMessage { true })

    edge(giveFeedbackToCallTools forwardTo giveFeedbackToCallTools onAssistantMessage { true })
    edge(giveFeedbackToCallTools forwardTo nodeExecuteTool onToolCall { true })

    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(
        nodeSendToolResult forwardTo nodeFinish onToolCall { tc -> tc.tool == "__exit__" } transformed
            { "Chat finished" }
    )
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}

/**
 * Creates a ReAct AI agent strategy that alternates between reasoning and execution stages
 * to dynamically process tasks and request outputs from an LLM.
 *
 * @param reasoningInterval Specifies the interval for reasoning steps.
 * @return An instance of [AIAgentGraphStrategy] that defines the ReAct strategy.
 *
 *
 * +-------+             +---------------+             +---------------+             +--------+
 * | Start | ----------> | CallLLMReas

--- chatAgentStrategy [ИСТОЧНИК 2] (line 72) ---
// File: AIAgentStrategies.kt
etween reasoning and execution stages
 * to dynamically process tasks and request outputs from an LLM.
 *
 * @param reasoningInterval Specifies the interval for reasoning steps.
 * @return An instance of [AIAgentGraphStrategy] that defines the ReAct strategy.
 *
 *
 * +-------+             +---------------+             +---------------+             +--------+
 * | Start | ----------> | CallLLMReason | ----------> | CallLLMAction | ----------> | Finish |
 * +-------+             +---------------+             +---------------+             +--------+
 *                                   ^                       | Finished?     Yes
 *                                   |                       | No
 *                                   |                       v
 *                                   +-----------------------+
 *                                   |      ExecuteTool      |
 *                                   +-----------------------+
 *
 * Example execution flow of a banking agent with ReAct strategy:
 *
 * 1. Start: User asks "How much did I spend last month?"
 *
 * 2. Reasoning Phase:
 *    CallLLMReason: "I need to follow these steps:
 *    1. Get all transactions from last month
 *    2. Filter out deposits (positive amounts)
 *    3. Calculate total spending"
 *
 * 3. Action & Execution Phase 1:
 *    CallLLMAction: {tool: "get_transactions", args: {startDate: "2025-05-19", endDate: "2025-06-18"}}
 *    ExecuteTool Result: [
 *      {date: "2025-05-25", amount: -100.00, description: "Grocery Store"},
 *      {date: "2025-05-31", amount: +1000.00, description: "Salary Deposit"},
 *      {date: "2025-06-10", amount: -500.00, description: "Rent Payment"},
 *      {date: "2025-06-13", amount: -200.00, description: "Utilities"}
 *    ]
 *
 * 4. Reasoning Phase:
 *    CallLLMReason: "I have the transactions. Now I need to:
 *    1. Remove the salary deposit of +1000.00
 *    2. Sum up the remaining transactions"
 *
 * 5. Action & Execution Phase 2:
 *    CallLLM

--- chatAgentStrategy [ИСТОЧНИК 3] (line 217) ---
// File: AIAgentStrategies.kt
ut>("structured_output_with_tools_strategy") {
    val setStructuredOutput by nodeSetStructuredOutput<Input, Output>(config = config)
    val transformInput by node<Input, String> { transform(it) }
    val callLLM by nodeLLMRequestMultiple()
    val executeTools by nodeExecuteMultipleTools(parallelTools = parallelTools)
    val sendToolResult by nodeLLMSendMultipleToolResults()
    val transformToStructuredOutput by node<Message.Assistant, Output> { response ->
        llm.writeSession {
            parseResponseToStructuredResponse(response, config, fixingParser).data
        }
    }

    // Set the structured output, get the input and then call the llm
    nodeStart then setStructuredOutput then transformInput then callLLM

    // On tools
    edge(callLLM forwardTo executeTools onMultipleToolCalls { true })
    edge(executeTools forwardTo sendToolResult)

    // On assistant messages
    edge(
        callLLM forwardTo transformToStructuredOutput
            onMultipleAssistantMessages { true }
            transformed { it.single() }
    )

    // Post tool result
    edge(sendToolResult forwardTo executeTools onMultipleToolCalls { true })
    edge(
        sendToolResult forwardTo transformToStructuredOutput
            onMultipleAssistantMessages { true }
            transformed { it.first() }
    )

    // Finish
    transformToStructuredOutput then nodeFinish
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt
Score: 0,700
Responsibility: Manages AI agents operating within a graph structure.
Key methods: copy(environment: AIAgentEnvironment, agentId: String, agentInput: Any?, agentInputType: TypeToken, config: AIAgentConfig)

--- extends [ИСТОЧНИК 4] (line 106) ---
// File: AIAgentGraphContext.kt
* This class encapsulates configurations, the execution pipeline,
 * agent environment, and tools for handling agent lifecycles and interactions.
 *
 * @constructor Creates an instance of the context with the given parameters.
 *
 * @param environment The AI agent environment responsible for tool execution and problem reporting.
 * @param agentInput The input message to be used for the agent's interaction with the environment.
 * @param config The configuration settings of the AI agent.
 * @param llm The contextual data and execution utilities for the AI agent's interaction with LLMs.
 * @param stateManager Manages the internal state of the AI agent.
 * @param storage Concurrent-safe storage for managing key-value data across the agent's lifecycle.
 * @param runId The unique identifier for the agent session.
 * @param strategyName The identifier for the selected strategy in the agent's lifecycle.
 * @param pipeline The AI agent pipeline responsible for coordinating AI agent execution and processing.
 */
@OptIn(InternalAgentsApi::class)
public class AIAgentGraphContext(
    environment: AIAgentEnvironment,
    override val agentId: String,
    override val agentInputType: TypeToken,
    override val agentInput: Any?,
    override val config: AIAgentConfig,
    llm: AIAgentLLMContext,
    stateManager: AIAgentStateManager,
    storage: AIAgentStorage,
    override val runId: String,
    override val strategyName: String,
    override val pipeline: AIAgentGraphPipeline,
    executionInfo: AgentExecutionInfo,
    override val parentContext: AIAgentGraphContextBase?,
) : AIAgentGraphContextBase {
    private val mutableAIAgentContext = MutableAIAgentContext(llm, stateManager, storage, environment, executionInfo)

    override val llm: AIAgentLLMContext
        get() = mutableAIAgentContext.llm

    override val storage: AIAgentStorage
        get() = mutableAIAgentContext.storage

    override val stateManager: AIAgentStateManager
        get() = mutableAIAgentContext.st

--- extends [ИСТОЧНИК 5] (line 265) ---
// File: AIAgentGraphContext.kt
i::class)
public val agentContextDataAdditionalKey: AIAgentStorageKey<AgentContextData> =
    AIAgentStorageKey("agent-context-data-key")

/**
 * Stores the given agent context data within the current AI agent context.
 *
 * @param data The context-specific data to be stored for later retrieval or use within the agent context.
 */
@InternalAgentsApi
public fun AIAgentContext.store(data: AgentContextData) {
    this.rootContext().store(agentContextDataAdditionalKey, data)
}

/**
 * Retrieves the agent-specific context data associated with the current instance.
 *
 * This function accesses and returns the contextual information stored as part of the agent's context,
 * or null if no such data is present.
 *
 * Note: This is part of the internal agents API and should be used cautiously, understanding that
 * it is subject to changes or removal in future updates.
 *
 * @return The agent context data, or null if no context data is associated.
 */
@InternalAgentsApi
public fun AIAgentContext.getAgentContextData(): AgentContextData? {
    return this.rootContext().get(agentContextDataAdditionalKey)
}

/**
 * Removes the agent-specific context data associated with the current context.
 *
 * This function attempts to remove the context data identified by the `agentContextDataAdditionalKey`.
 *
 * @return `true` if the agent context data was successfully removed, or `false` if no data was found to remove.
 */
@OptIn(InternalAgentsApi::class)
public fun AIAgentContext.removeAgentContextData(): Boolean {
    return this.rootContext().remove(agentContextDataAdditionalKey)
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentStrategies.kt · chatAgentStrategy · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 2] AIAgentStrategies.kt · chatAgentStrategy · line 72 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 3] AIAgentStrategies.kt · chatAgentStrategy · line 217 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 4] AIAgentGraphContext.kt · extends · line 106 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt
[ИСТОЧНИК 5] AIAgentGraphContext.kt · extends · line 265 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt


---
## Query 18: "все полезное про AIAgentContext и AIAgentGraphContext"
**Optimized:** "aiagentcontext aiagentgraphcontext kotlin"
**Metrics:** Retrieved: 15 → Filtered: 15 → Final: 5
**Timings:** query_optimize=148ms, retrieve=47ms, filter=0ms, rerank=3348ms, top_k=0ms, pack=6ms
**Top score:** 0,95 | Avg score: 0,89

### RAG Context:
Found 5 relevant class(es) | ~10148 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] LLMAsAJudge
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/LLMAsAJudge.kt
Score: 0,950
Responsibility: Evaluates tasks using an LLM and provides context-aware feedback.
Key methods: evaluateTask(task: String, history: List<Message>), generateFeedback(isCorrect: Boolean, feedback: String)

--- is [ИСТОЧНИК 1] (line 1) ---
// File: LLMAsAJudge.kt
package ai.koog.agents.ext.agent

import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

/**
 * Represents the result of a plan evaluation performed by an LLM (Large Language Model).
 *
 * This class is primarily used within internal agent-related implementations where an LLM
 * evaluates the correctness of a plan and optionally provides feedback for improvements.
 *
 * @property isCorrect Indicates whether the evaluated plan is correct.
 * @property feedback Optional feedback provided by the LLM about the evaluated plan. This property
 *        is populated only when the plan is deemed incorrect (`isCorrect == false`) and adjustments
 *        are suggested.
 */
@InternalAgentsApi
@Serializable
@LLMDescription("Result of the evaluation")
public data class CriticResultFromLLM(
    @property:LLMDescription("Was the task solved correctly?")
    val isCorrect: Boolean,
    @property:LLMDescription(
        "Optional feedback about the provided solution. " +
            "Only needed if `isCorrect == false` and if solution needs adjustments."
    )
    val feedback: String
)

/**
 * Represents the result of a critique or feedback process.
 *
 * @property successful Indicates whether the critique operation was successful.
 * @property feedback A textual message providing details about the*/
public class CriticResult<T>(
    successful: Boolean,
    feedback: String,
    input: T
) {
    /**
     * Indicates whether the verification of the critic has successfull

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt
Score: 0,920
Responsibility: Manages AI agents operating within a graph structure.
Key methods: copy(environment: AIAgentEnvironment, agentId: String, agentInput: Any?, agentInputType: TypeToken, config: AIAgentConfig)

--- extends [ИСТОЧНИК 2] (line 1) ---
// File: AIAgentGraphContext.kt
package ai.koog.agents.core.agent.context

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStateManager
import ai.koog.agents.core.agent.entity.AIAgentStorage
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.agent.execution.AgentExecutionInfo
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.utils.RWLock
import ai.koog.prompt.message.Message
import ai.koog.serialization.TypeToken

/**
 * The `AIAgentGraphContextBase` interface extends the `AIAgentContextBase` interface
 * to provide a foundational context specifically tailored for AI agents operating
 * within a graph structure.
 *
 * This interface inherits the core capabilities from `AIAgentContextBase`, including
 * environment management, configuration access, session tracking, state management,
 * and custom workflows. By building upon these features, it serves as a base for
 * defining additional constructs and behaviors that facilitate the agent's execution
 * in graph-based workflows or execution pipelines.
 *
 * Implementations of this interface are expected to leverage the provided capabilities
 * to handle graph-specific logic, such as node traversal, input/output management,
 * and handling complex dependencies between graph nodes.
 */
public interface AIAgentGraphContextBase : AIAgentContext {

    override val pipeline: AIAgentGraphPipeline

    /**
     * [TypeToken] representing the type of the [agentInput]
     */
    public val agentInputType: TypeToken

    /**
     * Creates a copy of the current [AIAgentGraphContext], allowing for selective overriding of its properties.
     *
     * @param environment The [AIAgentEnvironment] to be used in the new

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt
Score: 0,900
Responsibility: Manages and executes AI agent workflows built as interconnected nodes.
Key methods: AIAgentGraphStrategy(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), AIAgentGraphStrategyBase(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), nodeStart(), nodeFinish()

--- AIAgentGraphStrategy [ИСТОЧНИК 3] (line 1) ---
// File: AIAgentGraphStrategy.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.entity

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.context.AgentContextData
import ai.koog.agents.core.agent.context.getAgentContextData
import ai.koog.agents.core.agent.context.removeAgentContextData
import ai.koog.agents.core.agent.execution.DEFAULT_AGENT_PATH_SEPARATOR
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.serialization.JSONElement
import ai.koog.serialization.kotlinx.toKoogJSONElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Represents a strategy for managing and executing AI agent workflows built as subgraphs of interconnected nodes.
 *
 * @property name The unique identifier for the strategy.
 * @property nodeStart The starting node of the strategy, initiating the subgraph execution.
 * By default, the start node gets the agent input and returns
 * @property nodeFinish The finishing node of the strategy, marking the subgraph's endpoint.
 * @property toolSelectionStrategy The strategy responsible for determining the toolset available during subgraph execution.
 */
public expect class AIAgentGraphStrategy<TInput, TOutput>(
    name: String,
    nodeStart: StartNode<TInput>,
    nodeFinish: FinishNode<TOutput>,
    toolSelectionStrategy: ToolSelectionStrategy,
    serializer: Json = Json { prettyPrint = true }
) : AIAgentGraphStrategyBase<TInput, TOutput>

/**
 * Base class for [AIAgentStrategy].
 *
 * @property name The unique identifier for the strategy.
 * @property nodeStart The starting node of the strategy, initiating the subgraph execution.
 * By default, the start node gets the agent input and returns
 * @property nodeFinish The finishing node of the strategy, marking the subgraph's endpoint.
 * @property toolSelect

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentNode
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentNode.kt
Score: 0,850
Responsibility: Represents an abstract node in an AI agent strategy graph, responsible for executing a specific operation and managing directed edges to other nodes.
Key methods: addEdge()

--- AIAgentNodeBase [ИСТОЧНИК 4] (line 1) ---
// File: AIAgentNode.kt
package ai.koog.agents.core.agent.entity

import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.context.with
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentEdgeBuilderIntermediate
import ai.koog.agents.core.dsl.builder.EdgeTransformationDslMarker
import ai.koog.agents.core.utils.Some
import ai.koog.serialization.KotlinTypeToken
import ai.koog.serialization.TypeToken
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlin.reflect.KType
import kotlin.uuid.ExperimentalUuidApi

/**
 * Represents an abstract node in an AI agent strategy graph, responsible for executing a specific
 * operation and managing directed edges to other nodes.
 *
 * @param TInput The type of input data this node processes.
 * @param TOutput The type of output data this node produces.
 */
@OptIn(ExperimentalUuidApi::class)
public abstract class AIAgentNodeBase<in TInput, TOutput> internal constructor() {
    /**
     * The name of the AI agent node.
     * This property serves as a unique identifier for the node within the strategy graph
     * and is used to distinguish and reference nodes in the graph structure.
     */
    public abstract val name: String

    /**
     * The [TypeToken] of the [TInput]
     */
    public abstract val inputType: TypeToken

    /**
     * The [TypeToken] of the [TOutput]
     */
    public abstract val outputType: TypeToken

    /**
     * Represents the unique identifier of the AI agent node.
     */
    public val id: String get() = name

    /**
     * Represents the directed edges connecting the current node in the AI agent strategy graph
     * to other nodes. Each edge defines the flow and transformation of output data
     * from this node to another.
     *
     * The list is initially empty and can only be modified internally by using the
     * [addEdge] function, which appends new edges to the exis

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphPipelineImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipelineImpl.kt
Score: 0,850
Responsibility: Handles node execution events for AI agent graph pipeline.
Key methods: onNodeExecutionStarting(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken), onNodeExecutionCompleted(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken, output: Any?, outputType: TypeToken), onNodeExecutionFailed(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken, throwable: Throwable)

--- AIAgentGraphPipelineImpl [ИСТОЧНИК 5] (line 1) ---
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
## Источники
[ИСТОЧНИК 1] LLMAsAJudge.kt · is · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/LLMAsAJudge.kt
[ИСТОЧНИК 2] AIAgentGraphContext.kt · extends · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt
[ИСТОЧНИК 3] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt
[ИСТОЧНИК 4] AIAgentNode.kt · AIAgentNodeBase · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentNode.kt
[ИСТОЧНИК 5] AIAgentGraphPipelineImpl.kt · AIAgentGraphPipelineImpl · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipelineImpl.kt


---
## Query 19: "опиши работу с AIAgentLLMContext"
**Optimized:** "work-with-AIAgentLLMContext"
**Metrics:** Retrieved: 13 → Filtered: 13 → Final: 5
**Timings:** query_optimize=148ms, retrieve=43ms, filter=0ms, rerank=2755ms, top_k=0ms, pack=4ms
**Top score:** 0,95 | Avg score: 0,90

### RAG Context:
Found 3 relevant class(es) | ~8616 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
Score: 0,950
Responsibility: Manages tools, prompts, and interactions for an AI agent's language model.
Key methods: constructor(tools: List<ToolDescriptor>, toolRegistry: ToolRegistry, prompt: Prompt, model: LLModel, responseProcessor: ResponseProcessor?, promptExecutor: PromptExecutor, environment: AIAgentEnvironment, config:), handleRead(), handleWrite()

--- AIAgentLLMContext [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentLLMContext.kt
@file:OptIn(DetachedPromptExecutorAPI::class, InternalAgentsApi::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.context

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.session.AIAgentLLMReadSession
import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.processor.ResponseProcessor
import kotlin.jvm.JvmName
import kotlin.time.Clock

/**
 * Represents the context for an AI agent LLM, managing tools, prompt handling, and interaction with the
 * environment and execution layers. It provides mechanisms for concurrent read and write operations
 * through sessions, ensuring thread safety.
 *
 * @property tools A list of tool descriptors available for the context.
 * @property toolRegistry A registry that contains metadata about available tools.
 * @property prompt The current LLM prompt being used or updated in write sessions.
 * @property model The current LLM model being used or updated in write sessions.
 * @property responseProcessor The current response processor being used or updated in write sessions.
 * @property promptExecutor The [PromptExecutor] responsible for performing operations on the current prompt.
 * @property environment The environment that manages tool execution and interaction with external dependencies.
 * @property clock The clock used for timestamps of messages
 */
public expect class AIAgentLLMContext internal constructor(
    delegate: AIAgentLLMContextImpl
) : AIAgentLLMContextAPI {

    /**
     * Constructs a new instance of `AIAgentLLMContext` with the provided parameters.
     *
     * @param

--- AIAgentLLMContext [ИСТОЧНИК 2] (line 31) ---
// File: AIAgentLLMContext.kt
ronment The environment that manages tool execution and interaction with external dependencies.
 * @property clock The clock used for timestamps of messages
 */
public expect class AIAgentLLMContext internal constructor(
    delegate: AIAgentLLMContextImpl
) : AIAgentLLMContextAPI {

    /**
     * Constructs a new instance of `AIAgentLLMContext` with the provided parameters.
     *
     * @param tools A list of tools described by [ToolDescriptor] that the agent can interact with.
     * @param toolRegistry A registry of available tools, defaulting to an empty [ToolRegistry].
     * @param prompt The initial prompt used in the context, represented by a [Prompt] instance.
     * @param model The language model used for processing prompts and generating responses.
     * @param responseProcessor An optional [ResponseProcessor] for handling and processing model responses.
     * @param promptExecutor Responsible for executing the logic for prompt processing in the context.
     * @param environment The operational environment of the AI agent, represented by an [AIAgentEnvironment].
     * @param config Configuration settings for the AI agent, encapsulated in an [AIAgentConfig].
     * @param clock A clock instance for managing time-related operations within the context.
     */
    public constructor(
        tools: List<ToolDescriptor>,
        toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
        prompt: Prompt,
        model: LLModel,
        responseProcessor: ResponseProcessor?,
        promptExecutor: PromptExecutor,
        environment: AIAgentEnvironment,
        config: AIAgentConfig,
        clock: Clock
    )

    internal val delegate: AIAgentLLMContextImpl

    @get:JvmName("toolRegistry")
    override val toolRegistry: ToolRegistry

    @property:DetachedPromptExecutorAPI
    @get:JvmName("promptExecutor")
    override val promptExecutor: PromptExecutor

    @get:JvmName("environment")
    @InternalAgentsApi
    override val environment: AIAgentEnviron

--- AIAgentLLMContext [ИСТОЧНИК 3] (line 156) ---
// File: AIAgentLLMContext.kt
sion.() -> T): T

    /**
     * Returns the current prompt used in the LLM context.
     *
     * @return The current [Prompt] instance.
     */
    public override fun copy(
        tools: List<ToolDescriptor>,
        prompt: Prompt,
        model: LLModel,
        responseProcessor: ResponseProcessor?,
        promptExecutor: PromptExecutor,
        environment: AIAgentEnvironment,
        config: AIAgentConfig,
        clock: Clock
    ): AIAgentLLMContext
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentLLMContextAPI
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContextAPI.kt
Score: 0,880
Responsibility: Provides context and tools for managing AI agents within an LLM framework.
Key methods: toolRegistry(), promptExecutor(), environment()

--- DetachedPromptExecutorAPI [ИСТОЧНИК 4] (line 1) ---
// File: AIAgentLLMContextAPI.kt
@file:OptIn(DetachedPromptExecutorAPI::class, InternalAgentsApi::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.context

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.session.AIAgentLLMReadSession
import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.processor.ResponseProcessor
import kotlin.time.Clock

/**
 * Annotation for marking APIs as detached prompt executors within the `AIAgentLLMContext`.
 *
 * Using APIs annotated with this requires opting in, as calls to `PromptExecutor` will be disconnected
 * from the agent logic. This means these calls will not affect the agent's state or adhere to the
 * `ToolsConversionStrategy`.
 *
 * This API should be used with caution, as it provides functionality that operates outside the
 * standard agent lifecycle and processing logic.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Calls to PromptExecutor used from `AIAgentLLMContext` will not be connected to the agent logic, " +
        "and will not impact the agent's state. " +
        "Other than that, `ToolsConversionStrategy` will not be applied. " +
        "Please be cautious when using this API."
)
public annotation class DetachedPromptExecutorAPI

/**
 * API for the [AIAgentLLMContext]
 */
public interface AIAgentLLMContextAPI {
    /**
     * A [ToolRegistry] that contains metadata about available tools.
     * */
    public val toolRegistry: ToolRegistry

    /**
     * The [PromptExecutor] responsible for performing operations on t

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt
Score: 0,850
Responsibility: Manages and executes AI agent workflows built as interconnected nodes.
Key methods: AIAgentGraphStrategy(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), AIAgentGraphStrategyBase(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), nodeStart(), nodeFinish()

--- AIAgentGraphStrategy [ИСТОЧНИК 5] (line 1) ---
// File: AIAgentGraphStrategy.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.entity

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.context.AgentContextData
import ai.koog.agents.core.agent.context.getAgentContextData
import ai.koog.agents.core.agent.context.removeAgentContextData
import ai.koog.agents.core.agent.execution.DEFAULT_AGENT_PATH_SEPARATOR
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.serialization.JSONElement
import ai.koog.serialization.kotlinx.toKoogJSONElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Represents a strategy for managing and executing AI agent workflows built as subgraphs of interconnected nodes.
 *
 * @property name The unique identifier for the strategy.
 * @property nodeStart The starting node of the strategy, initiating the subgraph execution.
 * By default, the start node gets the agent input and returns
 * @property nodeFinish The finishing node of the strategy, marking the subgraph's endpoint.
 * @property toolSelectionStrategy The strategy responsible for determining the toolset available during subgraph execution.
 */
public expect class AIAgentGraphStrategy<TInput, TOutput>(
    name: String,
    nodeStart: StartNode<TInput>,
    nodeFinish: FinishNode<TOutput>,
    toolSelectionStrategy: ToolSelectionStrategy,
    serializer: Json = Json { prettyPrint = true }
) : AIAgentGraphStrategyBase<TInput, TOutput>

/**
 * Base class for [AIAgentStrategy].
 *
 * @property name The unique identifier for the strategy.
 * @property nodeStart The starting node of the strategy, initiating the subgraph execution.
 * By default, the start node gets the agent input and returns
 * @property nodeFinish The finishing node of the strategy, marking the subgraph's endpoint.
 * @property toolSelect

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentLLMContext.kt · AIAgentLLMContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
[ИСТОЧНИК 2] AIAgentLLMContext.kt · AIAgentLLMContext · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
[ИСТОЧНИК 3] AIAgentLLMContext.kt · AIAgentLLMContext · line 156 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
[ИСТОЧНИК 4] AIAgentLLMContextAPI.kt · DetachedPromptExecutorAPI · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContextAPI.kt
[ИСТОЧНИК 5] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt


---
## Query 20: "опиши назначение и как работать с AIAgentPlannerContext"
**Optimized:** "explain usage and implementation of AIAgentPlannerContext"
**Metrics:** Retrieved: 11 → Filtered: 11 → Final: 5
**Timings:** query_optimize=171ms, retrieve=44ms, filter=0ms, rerank=2163ms, top_k=0ms, pack=3ms
**Top score:** 0,30 | Avg score: 0,20

### RAG Context:
Found 2 relevant class(es) | ~9228 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] DummyAIAgentContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt
Score: 0,300
Responsibility: Mock implementation of AIAgentContext for testing purposes.
Key methods: isLLMDefined(), isEnvironmentDefined()

--- used [ИСТОЧНИК 1] (line 281) ---
// File: DummyAIAgentContext.kt
 is defined or required.
     */
    public var strategyName: String?

    /**
     * Represents execution-specific context information for the mock AI agent builder.
     * This variable allows tracking and observability of the agent's execution flow.
     *
     * By leveraging the properties defined in [AgentExecutionInfo], this information
     * aids in linking, tracing, and managing execution paths throughout the agent's lifecycle.
     */
    public var executionInfo: AgentExecutionInfo?

    /**
     * Creates and returns a copy of the current instance of `AIAgentContextMockBuilderBase`.
     *
     * @return A new instance of `AIAgentContextMockBuilderBase` with the same properties as the original.
     */
    public fun copy(
        environment: AIAgentEnvironment? = this.environment,
        agentInput: Any? = this.agentInput,
        agentInputType: TypeToken? = this.agentInputType,
        config: AIAgentConfig? = this.config,
        llm: AIAgentLLMContext? = this.llm,
        stateManager: AIAgentStateManager? = this.stateManager,
        storage: AIAgentStorage? = this.storage,
        runId: String? = this.runId,
        strategyName: String? = this.strategyName,
        executionInfo: AgentExecutionInfo? = this.executionInfo,
    ): AIAgentContextMockBuilderBase

    /**
     * Builds and returns an instance of [AIAgentContext] based on the current properties
     * of the builder. This method creates a finalized AI agent context, integrating all the
     * specified configurations, environment settings, and components into a coherent context
     * object ready for use.
     *
     * @return A fully constructed [AIAgentContext] instance representing the configured agent context.
     */
    override fun build(): AIAgentContext
}

/**
 * AIAgentContextMockBuilder is a builder class for constructing a mock implementation of an AI agent context.
 * It provides mechanisms to configure various components of the AI agent context before constructing it.

--- used [ИСТОЧНИК 2] (line 352) ---
// File: DummyAIAgentContext.kt
 Represents the [TypeToken] of the [agentInput].
     */
    override var agentInputType: TypeToken? = typeToken<String>()

    /**
     * Represents the AI agent configuration used in the mock builder.
     *
     * This property holds the agent's configuration, which may include the parameters for prompts,
     * the language model to be used, iteration limits, and strategies for handling missing tools.
     * It is used in constructing or copying an AI agent context during testing or mock setup.
     *
     * The configuration, represented by [AIAgentConfig], can be modified or replaced
     * depending on the requirements of the mock or testing scenario. A `null` value indicates
     * the absence of a specific configuration.
     */
    override var config: AIAgentConfig? = null

    /**
     * Represents the context for accessing and managing an AI agent's LLM (Large Language Model) configuration
     * and behavior. The `llm` property allows you to define or override the LLM context for the agent,
     * including tools, prompt handling, and interaction with external dependencies.
     *
     * Can be used for dependency injection, mock testing, or modifying the LLM behavior dynamically during
     * runtime. If set to `null`, it indicates that no specific LLM context is defined, and defaults or
     * fallback mechanisms may be used by the containing class.
     */
    override var llm: AIAgentLLMContext? = null

    /**
     * An overrideable property for managing the agent's state using an instance of [AIAgentStateManager].
     *
     * The `stateManager` provides thread-safe mechanisms to update, lock, and access the internal
     * state of the AI agent. It ensures the consistency of state modifications and employs a
     * mutual exclusion mechanism to synchronize coroutines accessing the state.
     *
     * This property can be used for customizing state management within the context of the
     * `AIAgentContextMockBuilder` and its associated operat

--- used [ИСТОЧНИК 3] (line 551) ---
// File: DummyAIAgentContext.kt
s("UNUSED_PARAMETER")
                operator fun get(propertyName: String): Any {
                    error("Unimplemented property access: $name.$propertyName")
                }

                /**
                 * Invokes a method by its name and passes the provided arguments.
                 *
                 * This method is typically used to simulate an unimplemented method call, throwing an error
                 * to indicate that the called method is not yet implemented.
                 *
                 * @param methodName The name of the method to invoke.
                 * @param args The arguments to pass to the method, provided as a vararg of any type.
                 * @return This function does not return a value as it throws an error instead.
                 */
                @Suppress("UNUSED_PARAMETER")
                fun invoke(methodName: String, vararg args: Any?): Any {
                    error("Unimplemented method call: $name.$methodName(${args.joinToString()})")
                }
            } as T
        }
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
Score: 0,180
Responsibility: Constructs AI agent subgraphs by defining and connecting nodes.
Key methods: nodeStart(), nodeFinish()

--- for [ИСТОЧНИК 4] (line 194) ---
// File: AIAgentSubgraphBuilder.kt
onStrategy,
    private val llmModel: LLModel?,
    private val llmParams: LLMParams?,
    private val responseProcessor: ResponseProcessor? = null,
) : AIAgentSubgraphBuilderBase<Input, Output>(),
    BaseBuilder<AIAgentSubgraphDelegate<Input, Output>> {
    override val nodeStart: StartNode<Input> = StartNode(subgraphName = name, type = inputType)
    override val nodeFinish: FinishNode<Output> = FinishNode(subgraphName = name, type = outputType)

    /**
     * Constructs an instance of AIAgentSubgraphBuilder with the provided parameters, using KTypes
     * for input and output type representation.
     *
     * This constructor is deprecated. All [KType] parameters should be replaced by the use of [TypeToken] instead.
     *
     * @param name An optional name for the subgraph being built.
     * @param inputType The type of the input data for the subgraph, represented as a [KType].
     * @param outputType The type of the output data for the subgraph, represented as a [KType].
     * @param toolSelectionStrategy The strategy used to select the tools for this subgraph.
     * @param llmModel An optional Large Language Model ([LLModel]) to be used within the subgraph.
     * @param llmParams An optional set of parameters ([LLMParams]) for configuring the LLM behavior.
     * @param responseProcessor An optional [ResponseProcessor] for post-processing responses in the subgraph.
     */
    @Deprecated("KTypes usage in graphs and nodes is deprecated. Please, use TypeTokens instead.")
    public constructor(
        name: String? = null,
        inputType: KType,
        outputType: KType,
        toolSelectionStrategy: ToolSelectionStrategy,
        llmModel: LLModel?,
        llmParams: LLMParams?,
        responseProcessor: ResponseProcessor? = null,
    ) : this(
        name,
        typeToken(inputType),
        typeToken(outputType),
        toolSelectionStrategy,
        llmModel,
        llmParams,
        responseProcessor
    )

    override fun build():

--- for [ИСТОЧНИК 5] (line 360) ---
// File: AIAgentSubgraphBuilder.kt
resenting a unit of execution that takes an input and produces an output.
 *
 * @param name An optional name for the node. If not provided, the property name of the delegate will be used.
 * @param execute A suspendable function that defines the node's execution logic.
 */
@InternalAgentsApi
public fun <Input, Output> node(
    name: String? = null,
    inputType: TypeToken,
    outputType: TypeToken,
    execute: suspend AIAgentGraphContextBase.(input: Input) -> Output
): AIAgentNodeDelegate<Input, Output> {
    return AIAgentNodeDelegate(
        name = name,
        inputType = inputType,
        outputType = outputType,
        execute = execute
    )
}

/**
 * Creates a subgraph with a specified tool selection strategy.
 * @param name Optional subgraph name
 * @param toolSelectionStrategy Strategy for tool selection
 * @param llmModel Initial LLM model used in this subgraph
 * @param llmParams Initial LLM prompt parameters used in this subgraph
 * @param responseProcessor Initial optional processor defining the post-processing of messages returned from the LLM.
 * @param define Subgraph definition function
 */
public inline fun <reified Input, reified Output> subgraph(
    name: String? = null,
    toolSelectionStrategy: ToolSelectionStrategy = ToolSelectionStrategy.ALL,
    llmModel: LLModel? = null,
    llmParams: LLMParams? = null,
    responseProcessor: ResponseProcessor? = null,
    define: AIAgentSubgraphBuilderBase<Input, Output>.() -> Unit
): AIAgentSubgraphDelegate<Input, Output> {
    return AIAgentSubgraphBuilder<Input, Output>(
        name,
        inputType = typeToken<Input>(),
        outputType = typeToken<Output>(),
        toolSelectionStrategy = toolSelectionStrategy,
        llmModel = llmModel,
        llmParams = llmParams,
        responseProcessor = responseProcessor,
    ).also { it.define() }.build()
}

/**
 * Creates a subgraph with a specified tool selection strategy.
 * @param name Optional subgraph name
 * @param toolSelectionStra

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] DummyAIAgentContext.kt · used · line 281 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt
[ИСТОЧНИК 2] DummyAIAgentContext.kt · used · line 352 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt
[ИСТОЧНИК 3] DummyAIAgentContext.kt · used · line 551 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt
[ИСТОЧНИК 4] AIAgentSubgraphBuilder.kt · for · line 194 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 5] AIAgentSubgraphBuilder.kt · for · line 360 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt


---
## Query 21: "что такое AIAgentGraphStrategy  и примеры использования"
**Optimized:** "aiagentgraphstrategy examples usage"
**Metrics:** Retrieved: 10 → Filtered: 10 → Final: 5
**Timings:** query_optimize=141ms, retrieve=46ms, filter=0ms, rerank=2003ms, top_k=0ms, pack=5ms
**Top score:** 0,95 | Avg score: 0,85

### RAG Context:
Found 3 relevant class(es) | ~9116 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphStrategyBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
Score: 0,950
Responsibility: Constructs an instance of AIAgentGraphStrategy with a defined start and finish node, along with a designated tool selection strategy.
Key methods: build(), strategy(name: String, toolSelectionStrategy: ToolSelectionStrategy)

--- responsible [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentGraphStrategyBuilder.kt
package ai.koog.agents.core.dsl.builder

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.FinishNode
import ai.koog.agents.core.agent.entity.StartNode
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.serialization.TypeToken
import ai.koog.serialization.typeToken

/**
 * A builder class responsible for constructing an instance of [AIAgentGraphStrategy].
 * The [AIAgentGraphStrategyBuilder] serves as a specific configuration for creating AI agent strategies
 * with a defined start and finish node, along with a designated tool selection strategy.
 *
 * @param name The name of the strategy being built, serving as a unique identifier.
 * @param toolSelectionStrategy The strategy used to determine the subset of tools available during subgraph execution.
 */
public class AIAgentGraphStrategyBuilder<TInput, TOutput>(
    private val name: String,
    inputType: TypeToken,
    outputType: TypeToken,
    private val toolSelectionStrategy: ToolSelectionStrategy,
) : AIAgentSubgraphBuilderBase<TInput, TOutput>(), BaseBuilder<AIAgentGraphStrategy<TInput, TOutput>> {
    public override val nodeStart: StartNode<TInput> = StartNode(type = inputType)
    public override val nodeFinish: FinishNode<TOutput> = FinishNode(type = outputType)

    override fun build(): AIAgentGraphStrategy<TInput, TOutput> {
        val strategy = AIAgentGraphStrategy(
            name = name,
            nodeStart = nodeStart,
            nodeFinish = nodeFinish,
            toolSelectionStrategy = toolSelectionStrategy
        )
        strategy.metadata = buildSubgraphMetadata(nodeStart, name, strategy)
        return strategy
    }
}

/**
 * Builds a local AI agent that processes user input through a sequence of stages.
 *
 * The agent executes a series of stages in sequence, with each stage receiving the output
 * of the previous stage as its input.
 *
 * @property name The unique identifier for this agent.
 * @param ini

--- responsible [ИСТОЧНИК 2] (line 34) ---
// File: AIAgentGraphStrategyBuilder.kt
      strategy.metadata = buildSubgraphMetadata(nodeStart, name, strategy)
        return strategy
    }
}

/**
 * Builds a local AI agent that processes user input through a sequence of stages.
 *
 * The agent executes a series of stages in sequence, with each stage receiving the output
 * of the previous stage as its input.
 *
 * @property name The unique identifier for this agent.
 * @param init Lambda that defines stages and nodes of this agent
 */
public inline fun <reified Input, reified Output> strategy(
    name: String,
    toolSelectionStrategy: ToolSelectionStrategy = ToolSelectionStrategy.ALL,
    init: AIAgentGraphStrategyBuilder<Input, Output>.() -> Unit,
): AIAgentGraphStrategy<Input, Output> {
    return AIAgentGraphStrategyBuilder<Input, Output>(
        name = name,
        inputType = typeToken<Input>(),
        outputType = typeToken<Output>(),
        toolSelectionStrategy = toolSelectionStrategy
    ).apply(init).build()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphStrategy
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt
Score: 0,900
Responsibility: Manages and executes AI agent workflows built as interconnected nodes.
Key methods: AIAgentGraphStrategy(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), AIAgentGraphStrategyBase(name: String, nodeStart: StartNode<TInput>, nodeFinish: FinishNode<TOutput>, toolSelectionStrategy: ToolSelectionStrategy), nodeStart(), nodeFinish()

--- AIAgentGraphStrategy [ИСТОЧНИК 3] (line 33) ---
// File: AIAgentGraphStrategy.kt
egyBase<TInput, TOutput>

/**
 * Base class for [AIAgentStrategy].
 *
 * @property name The unique identifier for the strategy.
 * @property nodeStart The starting node of the strategy, initiating the subgraph execution.
 * By default, the start node gets the agent input and returns
 * @property nodeFinish The finishing node of the strategy, marking the subgraph's endpoint.
 * @property toolSelectionStrategy The strategy responsible for determining the toolset available during subgraph execution.
 */
public open class AIAgentGraphStrategyBase<TInput, TOutput>(
    override val name: String,
    public val nodeStart: StartNode<TInput>,
    public val nodeFinish: FinishNode<TOutput>,
    toolSelectionStrategy: ToolSelectionStrategy,
    private val serializer: Json = Json { prettyPrint = true }
) : AIAgentStrategy<TInput, TOutput, AIAgentGraphContextBase>, AIAgentSubgraphBase<TInput, TOutput>(
    name,
    nodeStart,
    nodeFinish,
    toolSelectionStrategy
) {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    /**
     * Represents the metadata of the subgraph associated with the AI agent strategy.
     *
     * This variable holds essential information about the structure and properties of the
     * subgraph, such as the mapping of node names to their associated implementations and
     * the uniqueness of node names within the subgraph.
     *
     * This property can only be set internally, and an attempt to access it before initialization
     * will result in an `IllegalStateException`.
     */
    public lateinit var metadata: SubgraphMetadata

    @OptIn(InternalAgentsApi::class)
    override suspend fun execute(context: AIAgentGraphContextBase, input: TInput): TOutput? {
        restoreStateIfNeeded(context)

        var result: TOutput? = super.execute(context = context, input = input)

        while (result == null && context.getAgentContextData() != null) {
            restoreStateIfNeeded(context)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] TestingFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/TestingFeature.kt
Score: 0,800
Responsibility: Manages AI agent testing and simulation.
Key methods: createStorageKey(), handleRuntest(agentId: Long, presetArg: String?, chat: Chat), resolveNodeReference(subgraph: AIAgentSubgraph)

--- NodeReference [ИСТОЧНИК 4] (line 154) ---
// File: TestingFeature.kt
agent workflow.
         * @return The resolved subgraph of type `AIAgentSubgraph<Input, Output>`.
         * @throws IllegalArgumentException If the resolved subgraph does not match the expected type constraints.
         */
        override fun resolve(subgraph: AIAgentSubgraphBase<*, *>): AIAgentSubgraphBase<Input, Output> {
            val result = super.resolve(subgraph)

            if (result !is AIAgentSubgraph<Input, Output>) {
                throw IllegalArgumentException("Node with name '$name' is not a subgraph")
            }

            return result
        }
    }

    /**
     * Represents a specific strategy within an AI agent subgraph.
     *
     * The `Strategy` class is a specialization of the `SubgraphNode` class designed to focus on
     * resolving and managing `AIAgentStrategy` instances. It provides a mechanism to ensure the
     * resolution process evaluates to the expected strategy type, preserving type safety
     * and logical consistency within the graph-based AI workflow.
     *
     * @param name The unique identifier for the strategy.
     */
    public class Strategy<Input, Output>(name: String) : SubgraphNode<Input, Output>(name) {
        /**
         * Resolves the given subgraph into an `AIAgentStrategy` instance to ensure that the resolved object
         * matches the expected strategy name and type.
         *
         * @param subgraph The subgraph to be resolved. It must have the same name as the current strategy instance
         *                 and must also be of type `AIAgentStrategy`.
         * @return The resolved subgraph as an instance of `AIAgentStrategy`.
         * @throws IllegalArgumentException If the subgraph's name does not match the name of the current strategy.
         * @throws IllegalStateException If the subgraph is not of type `AIAgentStrategy`.
         */
        @Suppress("UNCHECKED_CAST")
        override fun resolve(subgraph: AIAgentSubgraphBase<*, *>): AIAgentGraphStrategyBase<Input, Out

--- NodeReference [ИСТОЧНИК 5] (line 186) ---
// File: TestingFeature.kt
graph as an instance of `AIAgentStrategy`.
         * @throws IllegalArgumentException If the subgraph's name does not match the name of the current strategy.
         * @throws IllegalStateException If the subgraph is not of type `AIAgentStrategy`.
         */
        @Suppress("UNCHECKED_CAST")
        override fun resolve(subgraph: AIAgentSubgraphBase<*, *>): AIAgentGraphStrategyBase<Input, Output> {
            if (subgraph.name != name) {
                throw IllegalArgumentException("Strategy with name '$name' was expected")
            }

            if (subgraph !is AIAgentGraphStrategy<*, *>) {
                throw IllegalStateException("Resolving a strategy is not possible from a subgraph")
            }

            return subgraph as AIAgentGraphStrategy<Input, Output>
        }
    }
}

/**
 * Represents a set of assertions for validating the structure and behavior of a graph-based system.
 *
 * @property name The name or identifier of the graph being asserted.
 * @property start The starting node reference of the graph.
 * @property finish The finishing node reference of the graph.
 * @property nodes A map containing all nodes of the graph, indexed by their names.
 * @property nodeOutputAssertions A list of assertions verifying the output of specific nodes based on given inputs and contexts.
 * @property edgeAssertions A list of assertions validating the edges between nodes in the graph for specific contexts.
 * @property unconditionalEdgeAssertions A list of assertions ensuring unconditional connections between nodes.
 * @property reachabilityAssertions A list of assertions verifying that one node in the graph is reachable from another node.
 * @property subgraphAssertions A mutable list of assertions related to specific subgraphs within the graph structure.
 */
@TestOnly
public data class GraphAssertions(
    val name: String,
    val start: NodeReference.Start<*>,
    val finish: NodeReference.Finish<*>,
    val nodes: Map<String, NodeReference<*,

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentGraphStrategyBuilder.kt · responsible · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
[ИСТОЧНИК 2] AIAgentGraphStrategyBuilder.kt · responsible · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
[ИСТОЧНИК 3] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt
[ИСТОЧНИК 4] TestingFeature.kt · NodeReference · line 154 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/TestingFeature.kt
[ИСТОЧНИК 5] TestingFeature.kt · NodeReference · line 186 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/TestingFeature.kt


---
## Query 22: "подробно про AIAgentNode  и его преимущества"
**Optimized:** "AIAgentNode advantages and details"
**Metrics:** Retrieved: 10 → Filtered: 10 → Final: 5
**Timings:** query_optimize=142ms, retrieve=46ms, filter=0ms, rerank=2046ms, top_k=0ms, pack=4ms
**Top score:** 0,85 | Avg score: 0,67

### RAG Context:
Found 4 relevant class(es) | ~8652 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentNodesWithChoiceExt
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/llm/choice/AIAgentNodesWithChoiceExt.kt
Score: 0,850
Responsibility: Creates nodes for sending tool results to LLM and selecting an LLM choice.
Key methods: nodeLLMSendResultsMultipleChoices(name: String?), nodeSelectLLMChoice(choiceSelectionStrategy: ChoiceSelectionStrategy, name: String?)

--- nodeLLMSendResultsMultipleChoices [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentNodesWithChoiceExt.kt
package ai.koog.agents.ext.llm.choice

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.result
import ai.koog.prompt.message.LLMChoice

/**
 * A node that sends multiple tool execution results to the LLM and gets multiple LLM choices.
 *
 * @param name Optional name for the node.
 */
@AIAgentBuilderDslMarker
public fun nodeLLMSendResultsMultipleChoices(
    name: String? = null
): AIAgentNodeDelegate<List<ReceivedToolResult>, List<LLMChoice>> =
    node(name) { results ->
        llm.writeSession {
            appendPrompt {
                tool {
                    results.forEach { result(it) }
                }
            }

            requestLLMMultipleChoices()
        }
    }

/**
 * A node that chooses an LLM choice based on the given strategy.
 *
 * @param choiceSelectionStrategy The strategy used to choose an LLM choice.
 * @param name Optional name for the node.
 */
@AIAgentBuilderDslMarker
public fun nodeSelectLLMChoice(
    choiceSelectionStrategy: ChoiceSelectionStrategy,
    name: String? = null
): AIAgentNodeDelegate<List<LLMChoice>, LLMChoice> =
    node(name) { choices ->
        llm.writeSession {
            choiceSelectionStrategy.choose(prompt, choices).also { choice ->
                choice.forEach { appendPrompt { message(it) } }
            }
        }
    }

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphExt
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
Score: 0,700
Responsibility: Manages subgraphs and tasks in a controlled, structured manner.
Key methods: nodeLLMRequestMultiple(), setToolChoiceRequired(), toSafeResult(), toolResultKindToJSON()

--- providing [ИСТОЧНИК 2] (line 197) ---
// File: AIAgentSubgraphExt.kt
fining the post-processing of messages returned from the LLM.
 * @param defineTask A suspending lambda function that defines the task for the subgraph, taking the input as a parameter.
 * @return A delegate that represents the created subgraph, allowing input and output operations.
 */
@OptIn(InternalAgentToolsApi::class, InternalAgentsApi::class)
@AIAgentBuilderDslMarker
@InternalAgentsApi
public fun <Input : Any, Output : Any> subgraphWithTask(
    name: String? = null,
    inputType: TypeToken,
    outputType: TypeToken,
    toolSelectionStrategy: ToolSelectionStrategy = ToolSelectionStrategy.ALL,
    llmModel: LLModel? = null,
    llmParams: LLMParams? = null,
    runMode: ToolCalls = ToolCalls.SEQUENTIAL,
    assistantResponseRepeatMax: Int? = null,
    responseProcessor: ResponseProcessor? = null,
    defineTask: suspend AIAgentGraphContextBase.(input: Input) -> String
): AIAgentSubgraphDelegate<Input, Output> = subgraph(
    name = name,
    inputType = inputType,
    outputType = outputType,
    toolSelectionStrategy = toolSelectionStrategy,
    llmModel = llmModel,
    llmParams = llmParams,
    responseProcessor = responseProcessor,
) {
    val finishTool = FinishTool<Output>(outputType)

    setupSubgraphWithTask<Input, Output, Output>(
        finishTool = finishTool,
        inputType = inputType,
        outputTransformedType = outputType,
        runMode = runMode,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        defineTask = defineTask
    )
}

/**
 * Creates a subgraph with a task definition and specified tools. The subgraph uses the provided tools to process
 * input and execute the defined task, eventually producing a result through the provided finish tool.
 *
 * @param tools The list of tools that are available for use within the subgraph.
 * @param name An optional name for the subgraph. Defaults to null if not provided.
 * @param llmModel An optional language model to be used in the subgraph. If not specified, a default

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
Score: 0,650
Responsibility: Constructs AI agent subgraphs by defining and connecting nodes.
Key methods: nodeStart(), nodeFinish()

--- for [ИСТОЧНИК 3] (line 265) ---
// File: AIAgentSubgraphBuilder.kt
subgraph and executes the initial logic.
 * @property nodeFinish The finishing node of the subgraph. This node marks the endpoint
 * and produces the final output of the subgraph.
 * @property toolSelectionStrategy The strategy for selecting the set of tools available
 * to the subgraph during its execution.
 * @property llmModel Initial LLM model used in this subgraph
 * @property llmParams Initial LLM prompt parameters used in this subgraph
 * @property responseProcessor Initial optional processor defining the post-processing of messages returned from the LLM.
 */
public open class AIAgentSubgraphDelegate<Input, Output> internal constructor(
    private val name: String?,
    public val nodeStart: StartNode<Input>,
    public val nodeFinish: FinishNode<Output>,
    private val toolSelectionStrategy: ToolSelectionStrategy,
    private val llmModel: LLModel?,
    private val llmParams: LLMParams?,
    private val responseProcessor: ResponseProcessor? = null,
) {
    private var subgraph: AIAgentSubgraph<Input, Output>? = null

    /**
     * Provides access to an instance of [AIAgentSubgraph] based on the specified property reference.
     *
     * This operator function acts as a delegate to dynamically retrieve and return an appropriate
     * instance of [AIAgentSubgraph] associated with the input and output types specified by the containing context.
     *
     * @param thisRef The reference to the object that contains the delegated property. Can be null if the property is a top-level or package-level property.
     * @param property The property metadata used to identify the property for which the subgraph instance is being accessed.
     * @return An [AIAgentSubgraph] instance that handles the specified input and output data types.
     */
    public operator fun getValue(thisRef: Any?, property: KProperty<*>): AIAgentSubgraph<Input, Output> {
        if (subgraph == null) {
            // if the name is explicitly defined, use it, otherwise use the property n

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphStrategyBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
Score: 0,600
Responsibility: Constructs an instance of AIAgentGraphStrategy with a defined start and finish node, along with a designated tool selection strategy.
Key methods: build(), strategy(name: String, toolSelectionStrategy: ToolSelectionStrategy)

--- responsible [ИСТОЧНИК 4] (line 1) ---
// File: AIAgentGraphStrategyBuilder.kt
package ai.koog.agents.core.dsl.builder

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.FinishNode
import ai.koog.agents.core.agent.entity.StartNode
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.serialization.TypeToken
import ai.koog.serialization.typeToken

/**
 * A builder class responsible for constructing an instance of [AIAgentGraphStrategy].
 * The [AIAgentGraphStrategyBuilder] serves as a specific configuration for creating AI agent strategies
 * with a defined start and finish node, along with a designated tool selection strategy.
 *
 * @param name The name of the strategy being built, serving as a unique identifier.
 * @param toolSelectionStrategy The strategy used to determine the subset of tools available during subgraph execution.
 */
public class AIAgentGraphStrategyBuilder<TInput, TOutput>(
    private val name: String,
    inputType: TypeToken,
    outputType: TypeToken,
    private val toolSelectionStrategy: ToolSelectionStrategy,
) : AIAgentSubgraphBuilderBase<TInput, TOutput>(), BaseBuilder<AIAgentGraphStrategy<TInput, TOutput>> {
    public override val nodeStart: StartNode<TInput> = StartNode(type = inputType)
    public override val nodeFinish: FinishNode<TOutput> = FinishNode(type = outputType)

    override fun build(): AIAgentGraphStrategy<TInput, TOutput> {
        val strategy = AIAgentGraphStrategy(
            name = name,
            nodeStart = nodeStart,
            nodeFinish = nodeFinish,
            toolSelectionStrategy = toolSelectionStrategy
        )
        strategy.metadata = buildSubgraphMetadata(nodeStart, name, strategy)
        return strategy
    }
}

/**
 * Builds a local AI agent that processes user input through a sequence of stages.
 *
 * The agent executes a series of stages in sequence, with each stage receiving the output
 * of the previous stage as its input.
 *
 * @property name The unique identifier for this agent.
 * @param ini

--- responsible [ИСТОЧНИК 5] (line 34) ---
// File: AIAgentGraphStrategyBuilder.kt
      strategy.metadata = buildSubgraphMetadata(nodeStart, name, strategy)
        return strategy
    }
}

/**
 * Builds a local AI agent that processes user input through a sequence of stages.
 *
 * The agent executes a series of stages in sequence, with each stage receiving the output
 * of the previous stage as its input.
 *
 * @property name The unique identifier for this agent.
 * @param init Lambda that defines stages and nodes of this agent
 */
public inline fun <reified Input, reified Output> strategy(
    name: String,
    toolSelectionStrategy: ToolSelectionStrategy = ToolSelectionStrategy.ALL,
    init: AIAgentGraphStrategyBuilder<Input, Output>.() -> Unit,
): AIAgentGraphStrategy<Input, Output> {
    return AIAgentGraphStrategyBuilder<Input, Output>(
        name = name,
        inputType = typeToken<Input>(),
        outputType = typeToken<Output>(),
        toolSelectionStrategy = toolSelectionStrategy
    ).apply(init).build()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentNodesWithChoiceExt.kt · nodeLLMSendResultsMultipleChoices · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/llm/choice/AIAgentNodesWithChoiceExt.kt
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · providing · line 197 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 3] AIAgentSubgraphBuilder.kt · for · line 265 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 4] AIAgentGraphStrategyBuilder.kt · responsible · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
[ИСТОЧНИК 5] AIAgentGraphStrategyBuilder.kt · responsible · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt


---
## Query 23: "как использовать AIAgentState"
**Optimized:** "how-to-use-aiagentstate"
**Metrics:** Retrieved: 12 → Filtered: 12 → Final: 5
**Timings:** query_optimize=141ms, retrieve=50ms, filter=0ms, rerank=2526ms, top_k=0ms, pack=3ms
**Top score:** 0,95 | Avg score: 0,87

### RAG Context:
Found 3 relevant class(es) | ~10088 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentState
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
Score: 0,950
Responsibility: Represents the state of an AI agent.
Key methods: copy(), close()

--- provides [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentState.kt
package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.annotation.InternalAgentsApi

/**
 * Represents the state of an AI agent during its lifecycle.
 *
 * This sealed interface provides different states to reflect whether the agent
 * has not started, is currently running, has completed its task successfully with a result,
 * or has failed with an exception.
 */
public sealed interface AIAgentState<Output> {
    /**
     * Creates and returns a copy of the current state object.
     *
     * @return A new instance of `AIAgentState<Output>` that is a copy of the current object.
     */
    public fun copy(): AIAgentState<Output>

    /**
     * Represents a state that indicates an action or process has not yet started.
     *
     * This class is part of the `State` sealed interface and is used to define
     * a specific state where no progress, execution, or processing has occurred.
     */
    public class NotStarted<Output> : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = NotStarted()
    }

    /**
     * Represents the starting state of an operation or process.
     *
     * This class is a specialization of the `State` class, indicating the initial
     * state prior to progression or change. It overrides the `copy` method to
     * return a new instance of the same starting state.
     *
     * @param Output The type of output associated with the state.
     */
    public class Starting<Output> : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = Starting()
    }

    /**
     * Represents the `Running` state of an AI agent, indicating that the agent is actively executing its tasks.
     *
     * This state provides access to the root context of the agent via the `rootContext` property, allowing
     * interaction with the overall execution environment, configuration, and state management facilities.
     *
     * The `rootContext` is marked wit

--- AIAgentState [ИСТОЧНИК 2] (line 1) ---
// File: AIAgentState.kt
package ai.koog.agents.core.agent.entity

import ai.koog.agents.core.utils.ActiveProperty
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents the state of an AI agent.
 */
@OptIn(ExperimentalStdlibApi::class)
public class AIAgentState(
    iterations: Int = 0,
) : AutoCloseable {
    /**
     * The number of iterations that have been completed since the agent was created.
     */
    public var iterations: Int by ActiveProperty(iterations) { isActive }

    private var isActive = true

    override fun close() {
        isActive = false
    }

    /**
     * Creates a copy of the current state.
     */
    public fun copy(): AIAgentState {
        return AIAgentState(
            iterations = iterations
        )
    }
}

/**
 * Manages the state of an AI agent by providing thread-safe access and mechanisms
 * to update the internal state using a locking mechanism.
 *
 * This class ensures consistency across state modifications by using a mutual exclusion
 * lock, allowing only one coroutine to access or modify the state at a time.
 *
 * @constructor Creates a new instance of AIAgentStateManager with the initial state,
 * defaulting to a new `AIAgentState` if not provided.
 */
public class AIAgentStateManager(
    private var state: AIAgentState = AIAgentState()
) {
    private val mutex = Mutex()

    /**
     * Executes the provided suspending [block] of code with exclusive access to the current state.
     * @return The result of [block].
     */
    public suspend fun <T> withStateLock(block: suspend (AIAgentState) -> T): T = mutex.withLock {
        val result = block(state)
        val newState = AIAgentState(
            iterations = state.iterations
        )

        // close this snapshot and create a new one
        state.close()
        state = newState

        result
    }

    internal suspend fun copy(): AIAgentStateManager {
        return withStateLock {
            AIAgentStateManager(state.copy())

--- provides [ИСТОЧНИК 3] (line 41) ---
// File: AIAgentState.kt
ng()
    }

    /**
     * Represents the `Running` state of an AI agent, indicating that the agent is actively executing its tasks.
     *
     * This state provides access to the root context of the agent via the `rootContext` property, allowing
     * interaction with the overall execution environment, configuration, and state management facilities.
     *
     * The `rootContext` is marked with the `@InternalAgentsApi` annotation, meaning its usage is intended for
     * internal agent-related implementations and may not maintain backwards compatibility.
     *
     * @property rootContext Provides access to the root context of the agent, facilitating operations
     *                       such as state management, feature retrieval, and context-based workflows.
     *                       This allows the agent to perform actions and manage its execution lifecycle within the given context.
     */
    public class Running<Output>(
        @property:InternalAgentsApi public val rootContext: AIAgentContext
    ) : AIAgentState<Output> {
        @OptIn(InternalAgentsApi::class)
        override fun copy(): AIAgentState<Output> = Running(rootContext)
    }

    /**
     * Represents the final state of a computation or process with its resulting output.
     *
     * @param Output The type of the result produced by the finished computation or process.
     * @property result The computed result of the finished process.
     */
    public class Finished<Output>(
        public val result: Output
    ) : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = Finished(result)
    }

    /**
     * Represents a state indicating an operation has failed.
     *
     * @property exception The throwable that caused the failure.
     */
    public class Failed<Output>(
        public val exception: Throwable
    ) : AIAgentState<Output> {
        override fun copy(): AIAgentState<Output> = Failed(exception)
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentContext.kt
Score: 0,850
Responsibility: Represents the context of an AI agent in the lifecycle.
Key methods: environment(), agentId(), pipeline(), runId(), agentInput()

--- represents [ИСТОЧНИК 4] (line 80) ---
// File: AIAgentContext.kt
access to the agent's state to ensure thread safety
     * and consistent state transitions during concurrent operations. It acts as a central
     * mechanism for managing state updates and validations across different
     * nodes and subgraphs of the AI agent's execution flow.
     *
     * The [stateManager] is utilized extensively in coordinating state changes, such as
     * tracking the number of iterations made by the agent and enforcing execution limits
     * or conditions. This aids in maintaining predictable and controlled behavior
     * of the agent during execution.
     */
    public val stateManager: AIAgentStateManager

    /**
     * Concurrent-safe key-value storage for an agent, used to manage and persist data within the context of
     *  the AI agent stage execution. The `storage` property provides a thread-safe mechanism for sharing
     * and storing data specific to the agent's operation.
     */
    public val storage: AIAgentStorage

    /**
     * Represents the name of the strategy being used in the current AI agent context.
     */
    public val strategyName: String

    /**
     * Represents the parent context of the AI Agent.
     */
    @InternalAgentsApi
    public val parentContext: AIAgentContext?

    /**
     * Represents the observability data associated with the AI Agent context.
     */
    public var executionInfo: AgentExecutionInfo

    /**
     * Stores a feature in the agent's storage using the specified key.
     *
     * @param key A uniquely identifying key of type `AIAgentStorageKey` used to store the feature.
     * @param value The feature to be stored, which can be of any type.
     */
    public fun store(key: AIAgentStorageKey<*>, value: Any)

    /**
     * Retrieves data from the agent's storage using the specified key.
     *
     * @param key A uniquely identifying key of type `AIAgentStorageKey` used to fetch the corresponding data.
     * @return The data associated with the provided key, or null if no m

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] DummyAIAgentContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt
Score: 0,750
Responsibility: Mock implementation of AIAgentContext for testing purposes.
Key methods: isLLMDefined(), isEnvironmentDefined()

--- used [ИСТОЧНИК 5] (line 383) ---
// File: DummyAIAgentContext.kt
s thread-safe mechanisms to update, lock, and access the internal
     * state of the AI agent. It ensures the consistency of state modifications and employs a
     * mutual exclusion mechanism to synchronize coroutines accessing the state.
     *
     * This property can be used for customizing state management within the context of the
     * `AIAgentContextMockBuilder` and its associated operations such as copying or building
     * mock agent contexts.
     *
     * By default, it is initialized to `null` and can be set or overridden to integrate a
     * specific `AIAgentStateManager` instance for managing agent state in custom scenarios.
     */
    override var stateManager: AIAgentStateManager? = null

    /**
     * Represents a concurrent-safe key-value storage instance for an AI agent.
     *
     * This property holds a reference to an optional [AIAgentStorage] implementation, which enables the
     * handling of typed keys and respective values in a thread-safe manner within the agent context.
     * The storage can be used to store, retrieve, or manage custom data uniquely identified by specific keys.
     *
     * It can be configured or overridden during the agent context setup or through later modifications
     * to the context builder. If not provided, the default value remains `null`.
     */
    override var storage: AIAgentStorage? = null

    /**
     * Defines the unique identifier for the session context within the agent's lifecycle.
     * This property can be used to correlate and differentiate multiple sessions for the same agent
     * or across different agents.
     *
     * The `runId` can be null, indicating that the session has not been associated with an identifier.
     */
    @OptIn(ExperimentalUuidApi::class)
    override var runId: String? = "test-run-id-${Uuid.random()}"

    /**
     * Represents the identifier for the strategy to be used in the agent context.
     *
     * This property is used to distinguish and configure d

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentState.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
[ИСТОЧНИК 2] AIAgentState.kt · AIAgentState · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentState.kt
[ИСТОЧНИК 3] AIAgentState.kt · provides · line 41 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
[ИСТОЧНИК 4] AIAgentContext.kt · represents · line 80 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentContext.kt
[ИСТОЧНИК 5] DummyAIAgentContext.kt · used · line 383 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt


---
## Query 24: "зачем нужен AIAgentStorage"
**Optimized:** "why-is-aiagentstorage-needed"
**Metrics:** Retrieved: 10 → Filtered: 10 → Final: 5
**Timings:** query_optimize=142ms, retrieve=40ms, filter=0ms, rerank=2042ms, top_k=0ms, pack=5ms
**Top score:** 0,92 | Avg score: 0,77

### RAG Context:
Found 3 relevant class(es) | ~7392 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentStorage
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorage.kt
Score: 0,920
Responsibility: Concurrent-safe key-value storage for an AI agent.
Key methods: copy(), set(key: AIAgentStorageKey<T>, value: T), get(key: AIAgentStorageKey<T>), getValue(key: AIAgentStorageKey<T>), remove(key: AIAgentStorageKey<T>)

--- AIAgentStorageKey [ИСТОЧНИК 1] (line 1) ---
// File: AIAgentStorage.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ai.koog.agents.core.agent.entity

/**
 * Represents a storage key used for identifying and accessing data associated with an AI agent.
 *
 * The generic type parameter [T] specifies the type of data associated with this key, ensuring
 * type safety when storing and retrieving data in the context of an AI agent.
 *
 * @param name The string identifier that uniquely represents the storage key.
 */
public class AIAgentStorageKey<T : Any>(public val name: String) {
    override fun toString(): String = "${super.toString()}(name=$name)"
}

/**
 * Creates a storage key for a specific type, allowing identification and retrieval of values associated with it.
 *
 * @param name The name of the storage key, used to uniquely identify it.
 * @return A new instance of [AIAgentStorageKey] for the specified type.
 */
public fun <T : Any> createStorageKey(name: String): AIAgentStorageKey<T> = AIAgentStorageKey(name)

/**
 * Concurrent-safe key-value storage for an agent.
 * You can create typed keys for your data using the [createStorageKey] function and
 * set and retrieve data using it by calling [set] and [get].
 */
public expect class AIAgentStorage internal constructor(
    delegate: AIAgentStorageImpl,
) : AIAgentStorageAPI {
    public constructor()

    internal val delegate: AIAgentStorageImpl

    /**
     * Creates a deep copy of this storage.
     *
     * @return A new instance of [AIAgentStorage] with the same content as this one.
     */
    internal suspend fun copy(): AIAgentStorage

    override suspend fun <T : Any> set(key: AIAgentStorageKey<T>, value: T)
    override suspend fun <T : Any> get(key: AIAgentStorageKey<T>): T?
    override suspend fun <T : Any> getValue(key: AIAgentStorageKey<T>): T
    override suspend fun <T : Any> remove(key: AIAgentStorageKey<T>): T?
    override suspend fun toMap(): Map<AIAgentStorageKey<*>, Any>
    override suspend fun putAll(map: Map<AIAgentStorageKey<*

--- AIAgentStorageKey [ИСТОЧНИК 2] (line 44) ---
// File: AIAgentStorage.kt
nd fun <T : Any> set(key: AIAgentStorageKey<T>, value: T)
    override suspend fun <T : Any> get(key: AIAgentStorageKey<T>): T?
    override suspend fun <T : Any> getValue(key: AIAgentStorageKey<T>): T
    override suspend fun <T : Any> remove(key: AIAgentStorageKey<T>): T?
    override suspend fun toMap(): Map<AIAgentStorageKey<*>, Any>
    override suspend fun putAll(map: Map<AIAgentStorageKey<*>, Any>)
    override suspend fun clear()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentStorageAPI
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorageAPI.kt
Score: 0,880
Responsibility: Manages storage for AI agents.
Key methods: set(key: AIAgentStorageKey<T>, value: T), get(key: AIAgentStorageKey<T>), getValue(key: AIAgentStorageKey<T>), remove(key: AIAgentStorageKey<T>), toMap()

--- AIAgentStorageAPI [ИСТОЧНИК 3] (line 1) ---
// File: AIAgentStorageAPI.kt
package ai.koog.agents.core.agent.entity

/**
 * API for [AIAgentStorage]
 */
public interface AIAgentStorageAPI {
    /**
     * Sets the value associated with the given key in the storage.
     *
     * @param key The key of type [AIAgentStorageKey] used to identify the value in the storage.
     * @param value The value to be associated with the key.
     */
    public suspend fun <T : Any> set(key: AIAgentStorageKey<T>, value: T)

    /**
     * Retrieves the value associated with the given key from the storage.
     *
     * @param key The key of type [AIAgentStorageKey] used to identify the value in the storage.
     * @return The value associated with the key, cast to type [T], or null if the key does not exist.
     */
    public suspend fun <T : Any> get(key: AIAgentStorageKey<T>): T?

    /**
     * Retrieves the non-null value associated with the given key from the storage.
     * If the key does not exist in the storage, a [NoSuchElementException] is thrown.
     *
     * @param key The key of type [AIAgentStorageKey] used to identify the value in the storage.
     * @return The value associated with the key, of type [T].
     * @throws NoSuchElementException if the key does not exist in the storage.
     */
    public suspend fun <T : Any> getValue(key: AIAgentStorageKey<T>): T

    /**
     * Removes the value associated with the given key from the storage.
     *
     * @param key The key of type [AIAgentStorageKey] used to identify the value in the storage.
     * @return The value associated with the key, cast to type [T], or null if the key does not exist.
     */
    public suspend fun <T : Any> remove(key: AIAgentStorageKey<T>): T?

    /**
     * Converts the storage to a map representation.
     *
     * @return A map containing all key-value pairs currently stored in the system, where keys are of type [AIAgentStorageKey]
     * and values are of type [Any].
     */
    public suspend fun toMap(): Map<AIAgentStorageKey<*>, Any>

    /**
     *

--- AIAgentStorageAPI [ИСТОЧНИК 4] (line 37) ---
// File: AIAgentStorageAPI.kt

     */
    public suspend fun <T : Any> remove(key: AIAgentStorageKey<T>): T?

    /**
     * Converts the storage to a map representation.
     *
     * @return A map containing all key-value pairs currently stored in the system, where keys are of type [AIAgentStorageKey]
     * and values are of type [Any].
     */
    public suspend fun toMap(): Map<AIAgentStorageKey<*>, Any>

    /**
     * Adds all key-value pairs from the given map to the storage.
     *
     * @param map A map containing keys of type [AIAgentStorageKey] and their associated values of type [Any].
     * The keys and values in the provided map will be added to the storage.
     */
    public suspend fun putAll(map: Map<AIAgentStorageKey<*>, Any>)

    /**
     * Clears all data from the storage.
     */
    public suspend fun clear()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ToolRegistry
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolRegistry.kt
Score: 0,300
Responsibility: Manages a collection of tools for agents.
Key methods: getToolOrNull(), getTool(), getTool()

--- ToolRegistry [ИСТОЧНИК 5] (line 1) ---
// File: ToolRegistry.kt
package ai.koog.agents.core.tools

import ai.koog.agents.annotations.JavaAPI
import kotlin.jvm.JvmStatic

/**
 * A registry that manages a collection of tools for use by agents.
 *
 * ToolRegistry serves as a central repository for all tools available to an agent.
 * It provides functionality to register tools and retrieve them by name or type.
 *
 * Key features:
 * - Maintains a unique collection of named tools
 * - Provides methods to retrieve tools by name or type
 * - Supports merging multiple registries
 *
 * Usage examples:
 * 1. Creating a registry:
 *    ```
 *    val registry = ToolRegistry {
 *        tool(MyCustomTool())
 *        tool(AnotherTool())
 *    }
 *    ```
 * 2. Merging registries:
 *    ```
 *    val combinedRegistry = registry1 + registry2
 *    ```
 *
 * @property tools The list of tools contained in this registry
 */
public class ToolRegistry private constructor(tools: List<Tool<*, *>> = emptyList()) {

    private val _tools: MutableList<Tool<*, *>> = tools.toMutableList()

    /**
     * Provides an immutable list of tools currently available in the registry.
     *
     * The tools are sourced from the internal backing collection and returned as
     * a read-only list to prevent external modification of the registry state.
     */
    public val tools: List<Tool<*, *>>
        get() = _tools.toList()

    /**
     * Retrieves a tool by its name from the registry, or null if not found.
     *
     * This method searches for a tool with the specified name and returns null
     * if no matching tool is found.
     *
     * @param toolName The name of the tool to retrieve
     * @return The tool with the specified name, or null if not found
     */
    public fun getToolOrNull(toolName: String): Tool<*, *>? {
        return _tools.firstOrNull { it.name == toolName }
    }

    /**
     * Retrieves a tool by its name from the registry.
     *
     * This method searches for a tool with the specified name.
     *
     * @param toolName The

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentStorage.kt · AIAgentStorageKey · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorage.kt
[ИСТОЧНИК 2] AIAgentStorage.kt · AIAgentStorageKey · line 44 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorage.kt
[ИСТОЧНИК 3] AIAgentStorageAPI.kt · AIAgentStorageAPI · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorageAPI.kt
[ИСТОЧНИК 4] AIAgentStorageAPI.kt · AIAgentStorageAPI · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorageAPI.kt
[ИСТОЧНИК 5] ToolRegistry.kt · ToolRegistry · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolRegistry.kt


---
## Query 25: "как работает и зона ответственности AIAgentStrategy"
**Optimized:** "what-is-the-working-of-and-responsibility-of-aiagentstrategy"
**Metrics:** Retrieved: 14 → Filtered: 14 → Final: 5
**Timings:** query_optimize=190ms, retrieve=46ms, filter=0ms, rerank=2931ms, top_k=0ms, pack=3ms
**Top score:** 0,90 | Avg score: 0,83

### RAG Context:
Found 2 relevant class(es) | ~10196 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentRunSessionImpl
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
Score: 0,900
Responsibility: Manages the lifecycle of an AI agent's execution.
Key methods: pipeline(), context(), run(input: Input)

--- handles [ИСТОЧНИК 1] (line 1) ---
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

--- handles [ИСТОЧНИК 2] (line 33) ---
// File: AIAgentRunSessionImpl.kt
ce used for logging execution details and errors.
 * @property agent the AI agent instance being executed in this session.
 * @property strategy the execution strategy that defines how the agent processes input and produces output.
 */
internal class AIAgentRunSessionImpl<Input, Output, TContext : AIAgentContext>(
    private val id: String,
    private val logger: KLogger,
    private val agent: AIAgent<Input, Output>,
    private val strategy: AIAgentStrategy<Input, Output, TContext>,
    private val sessionPipeline: AIAgentPipeline,
    private val ctxBuilder: suspend (Input, String, String) -> TContext
) : AIAgentRunSession<Input, Output, TContext> {
    private var state: AIAgentState<Output> = NotStarted()

    override fun pipeline(): AIAgentPipeline = sessionPipeline

    private var ctx: TContext? = null

    override fun context(): TContext = ctx
        ?: error("Context is not available before running the session. Call run() to start the session and initialize the context.")

    override suspend fun run(
        input: Input
    ): Output {
        state = AIAgentState.Starting()
        val context = ctxBuilder(input, id, agent.id)
        ctx = context
        val runResult = withPreparedPipeline(context, agent.id, sessionPipeline) {
            try {
                logger.debug { formatLog(id, id, "Starting agent execution") }
                sessionPipeline.onAgentStarting<Input, Output>(
                    agent.id,
                    context.executionInfo,
                    id,
                    agent,
                    context
                )

                val result = context.with(partName = strategy.name) { executionInfo, eventId ->
                    runCatchingCancellable {
                        state = AIAgentState.Running(context.parentContext ?: context)
                        context.pipeline.onStrategyStarting(eventId, executionInfo, strategy, context)
                        val result = strategy.execute(context = cont

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentFunctionalContextBaseAPI
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt
Score: 0,850
Responsibility: Provides an interface for managing and interacting with AI agent functionalities.
Key methods: requestLLM(message: String, allowToolCalls: Boolean = true), store(key: AIAgentStorageKey<*>, value: Any), get(key: AIAgentStorageKey<*>), remove(key: AIAgentStorageKey<*>), getHistory()

--- AIAgentFunctionalContextBaseAPI [ИСТОЧНИК 3] (line 273) ---
// File: AIAgentFunctionalContextBaseAPI.kt
   preserveMemory: Boolean = true
    )

    /**
     * Executes a subtask with validation and verification of the results.
     * The method defines a subtask for the AI agent using the provided input
     * and additional parameters and ensures that the output is evaluated
     * based on its correctness and feedback.
     *
     * @param taskDescription The subtask to be executed by AIAgent.
     * @param Input The type of the input provided to the subtask.
     * @param input The input data for the subtask, which will be used to
     * create and execute the task.
     * @param tools An optional list of tools that can be used during
     * the execution of the subtask.
     * @param llmModel An optional parameter specifying the LLM model to be used for the subtask.
     * @param llmParams Optional configuration parameters for the LLM, such as temperature
     * and token limits.
     * @param runMode The mode in which tools should be executed, either sequentially or in parallel.
     * @param assistantResponseRepeatMax An optional parameter specifying the maximum number of
     * retries for getting valid responses from the assistant.
     * @return A [CriticResult] object containing the verification status, feedback,
     * and the original input for the subtask.
     */
    @OptIn(InternalAgentToolsApi::class, InternalAgentsApi::class)
    public suspend fun <Input> subtaskWithVerification(
        taskDescription: String,
        input: Input,
        tools: List<Tool<*, *>>? = null,
        llmModel: LLModel? = null,
        llmParams: LLMParams? = null,
        runMode: ToolCalls = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax: Int? = null,
        responseProcessor: ResponseProcessor? = null,
    ): CriticResult<Input>

    /**
     * Executes a subtask within the larger context of an AI agent's functional operation. This method allows you to define a specific
     * task to be performed, using the given input, tools, and optional configuration

--- AIAgentFunctionalContextBaseAPI [ИСТОЧНИК 4] (line 304) ---
// File: AIAgentFunctionalContextBaseAPI.kt
Mode: ToolCalls = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax: Int? = null,
        responseProcessor: ResponseProcessor? = null,
    ): CriticResult<Input>

    /**
     * Executes a subtask within the larger context of an AI agent's functional operation. This method allows you to define a specific
     * task to be performed, using the given input, tools, and optional configuration parameters.
     *
     * @param taskDescription The subtask to be executed by AIAgent.
     * @param Input The type of input provided to the subtask.
     * @param Output The type of the output expected from the subtask.
     * @param input The input data required for the subtask execution.
     * @param tools A list of tools available for use within the subtask.
     * @param llmModel The optional large language model to be used during the subtask, if different from the default one.
     * @param llmParams The configuration parameters for the large language model, such as temperature, etc.
     * @param runMode The mode in which tools should be executed, either sequentially or in parallel.
     * @param assistantResponseRepeatMax The maximum number of times the assistant response can repeat. Useful to control redundant outputs.
     * @return The result of the subtask execution, as an instance of type Output.
     */
    @OptIn(InternalAgentToolsApi::class)
    public suspend fun <Input, Output : Any> subtask(
        taskDescription: String,
        input: Input,
        outputClass: KClass<Output>,
        tools: List<Tool<*, *>>? = null,
        llmModel: LLModel? = null,
        llmParams: LLMParams? = null,
        runMode: ToolCalls = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax: Int? = null,
        responseProcessor: ResponseProcessor? = null,
    ): Output

    /**
     * Executes a subtask within the AI agent's functional context. This method enables the use of tools to
     * achieve a specific task based on the input provided. It defines the task u

--- AIAgentFunctionalContextBaseAPI [ИСТОЧНИК 5] (line 331) ---
// File: AIAgentFunctionalContextBaseAPI.kt
    llmParams: LLMParams? = null,
        runMode: ToolCalls = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax: Int? = null,
        responseProcessor: ResponseProcessor? = null,
    ): Output

    /**
     * Executes a subtask within the AI agent's functional context. This method enables the use of tools to
     * achieve a specific task based on the input provided. It defines the task using an inline function,
     * employs tools iteratively, and attempts to complete the subtask with a designated finishing tool.
     *
     * @param taskDescription The subtask to be executed by AIAgent.
     * @param input The input data required to define and execute the subtask.
     * @param tools An optional list of tools that can be used to achieve the task, excluding the finishing tool.
     * @param finishTool A mandatory tool that determines the final result of the subtask by producing and transforming output.
     * @param llmModel An optional specific language learning model (LLM) to use for executing the subtask.
     * @param llmParams Optional parameters for configuring the behavior of the LLM during subtask execution.
     * @param runMode The mode in which tools should be executed, either sequentially or in parallel.
     * @param assistantResponseRepeatMax The maximum number of feedback attempts allowed from the language model if the subtask is not completed.
     * @return The transformed final result of executing the finishing tool to complete the subtask.
     */
    @OptIn(InternalAgentToolsApi::class, DetachedPromptExecutorAPI::class, InternalAgentsApi::class)
    public suspend fun <Input, OutputTransformed> subtask(
        taskDescription: String,
        input: Input,
        tools: List<Tool<*, *>>? = null,
        finishTool: Tool<*, OutputTransformed>,
        llmModel: LLModel? = null,
        llmParams: LLMParams? = null,
        runMode: ToolCalls = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax: Int? = null,
        responseProce

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentRunSessionImpl.kt · handles · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 2] AIAgentRunSessionImpl.kt · handles · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 3] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 273 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt
[ИСТОЧНИК 4] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 304 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt
[ИСТОЧНИК 5] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 331 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt


---
## Query 26: "как работает и зона ответственности AIAgentSubgraph с примерами"
**Optimized:** "how does aiagentsubgraph responsibility zone work with examples"
**Metrics:** Retrieved: 14 → Filtered: 14 → Final: 5
**Timings:** query_optimize=174ms, retrieve=49ms, filter=0ms, rerank=2971ms, top_k=0ms, pack=4ms
**Top score:** 0,85 | Avg score: 0,77

### RAG Context:
Found 3 relevant class(es) | ~9592 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphPipeline
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
Score: 0,850
Responsibility: Manages the execution of AI agent graph nodes using registered handlers.
Key methods: install(feature: AIAgentGraphFeature, configure: TConfig.() -> Unit), triggerNodeHandlersBeforeExecution(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken)

--- manages [ИСТОЧНИК 1] (line 268) ---
// File: AIAgentGraphPipeline.kt
am feature The AI agent graph feature that specifies the feature to intercept.
     * @param handle A suspendable function that handles the subgraph execution completion event,
     * taking the event context as a parameter.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
     *     logger.info("Subgraph ${eventContext.subgraph.name} executed with input: ${eventContext.input} and produced output: ${eventContext.output}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionCompleted(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionCompletedContext) -> Unit
    )

    /**
     * Intercepts and handles subgraph execution failures for a given feature.
     *
     * @param feature The feature associated with this handler.
     * @param handle A suspend function that processes the subgraph execution failure event.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionFailed(feature) { eventContext ->
     *     logger.error("Subgraph ${eventContext.subgraph.name} execution failed with error: ${eventContext.throwable}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionFailed(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionFailedContext) -> Unit
    )

    //endregion Interceptors
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphExt
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
Score: 0,800
Responsibility: Manages subgraphs and tasks in a controlled, structured manner.
Key methods: nodeLLMRequestMultiple(), setToolChoiceRequired(), toSafeResult(), toolResultKindToJSON()

--- providing [ИСТОЧНИК 2] (line 165) ---
// File: AIAgentSubgraphExt.kt
IAgentGraphContextBase.(input: Input) -> String
): AIAgentSubgraphDelegate<Input, Output> = subgraph(
    name = name,
    toolSelectionStrategy = toolSelectionStrategy,
    llmModel = llmModel,
    llmParams = llmParams,
    responseProcessor = responseProcessor,
) {
    val finishTool = FinishTool<Output>(typeToken<Output>())

    setupSubgraphWithTask<Input, Output, Output>(
        finishTool = finishTool,
        runMode = runMode,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        defineTask = defineTask
    )
}

/**
 * Creates a subgraph, which performs one specific task, defined by [defineTask],
 * using the tools defined by [toolSelectionStrategy].
 *
 * Use this function if you need the agent to perform a single task which outputs a structured result.
 *
 * @param Input The input type for the task to be defined in the subgraph.
 * @param Output The output type for the subgraph's finalized result.
 * @param toolSelectionStrategy The strategy used to select tools for the subgraph operations.
 * @param name An optional name for the subgraph. Defaults to null if not provided.
 * @param llmModel Optional language model to be used within the subgraph. Defaults to null.
 * @param llmParams Optional parameters for configuring the language model behavior. Defaults to null.
 * @param runMode The mode in which tools are executed. Defaults to sequential execution.
 * @param assistantResponseRepeatMax The maximum number of assistant responses allowed before determining that the task cannot be completed.
 * @param responseProcessor An optional processor defining the post-processing of messages returned from the LLM.
 * @param defineTask A suspending lambda function that defines the task for the subgraph, taking the input as a parameter.
 * @return A delegate that represents the created subgraph, allowing input and output operations.
 */
@OptIn(InternalAgentToolsApi::class, InternalAgentsApi::class)
@AIAgentBuilderDslMarker
@InternalAgentsApi
public

--- providing [ИСТОЧНИК 3] (line 197) ---
// File: AIAgentSubgraphExt.kt
fining the post-processing of messages returned from the LLM.
 * @param defineTask A suspending lambda function that defines the task for the subgraph, taking the input as a parameter.
 * @return A delegate that represents the created subgraph, allowing input and output operations.
 */
@OptIn(InternalAgentToolsApi::class, InternalAgentsApi::class)
@AIAgentBuilderDslMarker
@InternalAgentsApi
public fun <Input : Any, Output : Any> subgraphWithTask(
    name: String? = null,
    inputType: TypeToken,
    outputType: TypeToken,
    toolSelectionStrategy: ToolSelectionStrategy = ToolSelectionStrategy.ALL,
    llmModel: LLModel? = null,
    llmParams: LLMParams? = null,
    runMode: ToolCalls = ToolCalls.SEQUENTIAL,
    assistantResponseRepeatMax: Int? = null,
    responseProcessor: ResponseProcessor? = null,
    defineTask: suspend AIAgentGraphContextBase.(input: Input) -> String
): AIAgentSubgraphDelegate<Input, Output> = subgraph(
    name = name,
    inputType = inputType,
    outputType = outputType,
    toolSelectionStrategy = toolSelectionStrategy,
    llmModel = llmModel,
    llmParams = llmParams,
    responseProcessor = responseProcessor,
) {
    val finishTool = FinishTool<Output>(outputType)

    setupSubgraphWithTask<Input, Output, Output>(
        finishTool = finishTool,
        inputType = inputType,
        outputTransformedType = outputType,
        runMode = runMode,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        defineTask = defineTask
    )
}

/**
 * Creates a subgraph with a task definition and specified tools. The subgraph uses the provided tools to process
 * input and execute the defined task, eventually producing a result through the provided finish tool.
 *
 * @param tools The list of tools that are available for use within the subgraph.
 * @param name An optional name for the subgraph. Defaults to null if not provided.
 * @param llmModel An optional language model to be used in the subgraph. If not specified, a default

--- providing [ИСТОЧНИК 4] (line 547) ---
// File: AIAgentSubgraphExt.kt
ponseRepeatMax = assistantResponseRepeatMax,
    responseProcessor = responseProcessor,
    defineTask = defineTask
)

//endregion Subgraph With Verification

/**
 * Configures a subgraph within the AI agent framework, associating it with required tasks and operations.
 *
 * FOR INTERNAL USAGE ONLY!
 *
 * @param finishTool A descriptor for the tool that determines the condition to finalize the subgraph's operation.
 * @param defineTask A suspending lambda that defines the main task of the subgraph, producing a task description based on the input.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(InternalAgentToolsApi::class)
@Deprecated(
    message = "Use setupSubgraphWithTask API that receive a runMode parameter instead.",
    replaceWith = ReplaceWith(
        expression = "setupSubgraphWithTask(finishTool, assistantResponseRepeatMax, runMode, defineTask)"
    )
)
@InternalAgentsApi
public inline fun <reified Input, reified Output, reified OutputTransformed> AIAgentSubgraphBuilderBase<Input, OutputTransformed>.setupSubgraphWithTask(
    finishTool: Tool<Output, OutputTransformed>,
    assistantResponseRepeatMax: Int? = null,
    noinline defineTask: suspend AIAgentGraphContextBase.(Input) -> String
) {
    return setupSubgraphWithTask(
        finishTool = finishTool,
        runMode = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        defineTask = defineTask,
    )
}

/**
 * Configures and sets up a subgraph with task handling, including tool execution operations,
 * assistant response management, and task finalization logic.
 *
 * @param Input the type of input data for the subgraph.
 * @param Output the type of output data from the finish tool.
 * @param OutputTransformed the transformed type of the output data after processing by the finish tool.
 * @param finishTool the tool used to signify task completion and process task finalization.
 * @param runMode the mode in which tools are executed, e.g., parallel or sequential execu

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraph
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentSubgraph.kt
Score: 0,700
Responsibility: Manages and executes a subgraph of AI agent processing.
Key methods: handleRuntest(), selectRelevantTools(), store(), with()

--- for [ИСТОЧНИК 5] (line 33) ---
// File: AIAgentSubgraph.kt

 * Base class for [AIAgentSubgraph].
 *
 * @param TInput The type of input data accepted by the subgraph.
 * @param TOutput The type of output data returned by the subgraph.
 * @param name The name of the subgraph.
 * @param start The starting node of the subgraph, which initiates the processing.
 * @param finish The finishing node of the subgraph, which concludes the processing.
 * @param toolSelectionStrategy Strategy determining which tools should be available during this subgraph's execution.
 * @param llmModel Optional [LLModel] override for the subgraph execution.
 * @param llmParams Optional [LLMParams] override for the prompt for the subgraph execution.
 * @param responseProcessor Optional [ResponseProcessor] override for the subgraph execution.
 */
public open class AIAgentSubgraphBase<TInput, TOutput>(
    override val name: String,
    public val start: StartNode<TInput>,
    public val finish: FinishNode<TOutput>,
    private val toolSelectionStrategy: ToolSelectionStrategy,
    private val llmModel: LLModel? = null,
    private val llmParams: LLMParams? = null,
    private val responseProcessor: ResponseProcessor? = null,
) : AIAgentNodeBase<TInput, TOutput>(), ExecutionPointNode {
    override val inputType: TypeToken = start.inputType
    override val outputType: TypeToken = finish.outputType

    /**
     * Companion object for the AIAgentSubgraphBase class.
     *
     * This companion object provides predefined constants used to denote
     * special nodes (start and finish) within the subgraph of an AI agent strategy.
     * It also includes utilities for internal logging.
     */
    public companion object {
        private val logger = KotlinLogging.logger { }

        /**
         * A constant string used as a prefix to identify the starting node in an AI agent's execution graph.
         * This prefix is used to ensure unique identification and separation of the start node
         * within the graph structure or during execution-related ope

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · providing · line 165 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 3] AIAgentSubgraphExt.kt · providing · line 197 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 4] AIAgentSubgraphExt.kt · providing · line 547 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 5] AIAgentSubgraph.kt · for · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentSubgraph.kt


---
## Query 27: "расскажи все про ExecutionPointNode"
**Optimized:** "executionpointnode implementation"
**Metrics:** Retrieved: 5 → Filtered: 5 → Final: 5
**Timings:** query_optimize=120ms, retrieve=41ms, filter=0ms, rerank=970ms, top_k=0ms, pack=5ms
**Top score:** 1,00 | Avg score: 0,20

### RAG Context:
Found 5 relevant class(es) | ~7252 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ExecutionPointNode
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/ExecutionPointNode.kt
Score: 1,000
Responsibility: Manages and enforces execution points in an AI agent's graph.
Key methods: getExecutionPoint(), resetExecutionPoint(), enforceExecutionPoint(node: AIAgentNodeBase<*, *>, input: Any?)

--- provides [ИСТОЧНИК 1] (line 1) ---
// File: ExecutionPointNode.kt
package ai.koog.agents.core.agent.entity

/**
 * Represents a node in the execution graph of an AI agent that can explicitly enforce execution
 * at a specified node with optional input data.
 * This interface provides the ability to define a forced node and input,
 * overriding default execution behavior.
 */
public interface ExecutionPointNode {
    /**
     * Retrieves the current execution point, which consists of a specific node in the execution
     * graph and an optional input. If no forced node is defined, the method returns null.
     *
     * @return The execution point containing the forced node and input, or null if no forced node is set.
     */
    public fun getExecutionPoint(): ExecutionPoint?

    /**
     * Resets the currently enforced execution point for the AI agent, including clearing
     * any forced node and input data. Once the execution point is reset, the system will
     * revert to its default execution behavior without targeting a specific node explicitly.
     */
    public fun resetExecutionPoint()

    /**
     * Sets a forced node for the entity.
     */
    public fun enforceExecutionPoint(node: AIAgentNodeBase<*, *>, input: Any? = null)
}

/**
 * Represents a point of execution within the AI agent's strategy graph.
 * An execution point consists of a specific node and an optional input value.
 *
 * @property node The node within the AI agent's strategy graph to be executed.
 * The node is an instance of [AIAgentNodeBase], which defines the operation to be performed
 * and its associated metadata.
 *
 * @property input The optional input data provided to the execution point.
 * The node will use this data during its execution.
 */
public data class ExecutionPoint(val node: AIAgentNodeBase<*, *>, val input: Any? = null)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] RegisteredFeature
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
Score: 0,000
Responsibility: Represents a configured and installed agent feature implementation along with its configuration.
Key methods: featureImpl(), featureConfig()

--- RegisteredFeature [ИСТОЧНИК 2] (line 1) ---
// File: RegisteredFeature.kt
package ai.koog.agents.core.feature.pipeline

import ai.koog.agents.core.feature.config.FeatureConfig

/**
 * Represents configured and installed agent feature implementation along with its configuration.
 * @param featureImpl The feature implementation
 * @param featureConfig The feature configuration
 */
@Suppress("RedundantVisibilityModifier") // have to put public here, explicitApi requires it
public class RegisteredFeature(
    public val featureImpl: Any,
    public val featureConfig: FeatureConfig
)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] MySQLJdbcPersistenceStorageProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-persistence-jdbc/src/main/kotlin/ai/koog/agents/features/persistence/jdbc/MySQLJdbcPersistenceStorageProvider.kt
Score: 0,000
Responsibility: Provides MySQL-specific JDBC implementation for persistence storage.
Key methods: <init>(dataSource: DataSource, tableName: String?, ttlSeconds: Long?, migrator: SQLPersistenceSchemaMigrator?, json: Json?), upsertSql()

--- MySQLJdbcPersistenceStorageProvider [ИСТОЧНИК 3] (line 1) ---
// File: MySQLJdbcPersistenceStorageProvider.kt
package ai.koog.agents.features.persistence.jdbc

import ai.koog.agents.features.sql.providers.SQLPersistenceSchemaMigrator
import kotlinx.serialization.json.Json
import javax.sql.DataSource

/**
 * MySQL-specific JDBC implementation of [JdbcPersistenceStorageProvider].
 *
 * Uses `INSERT ... ON DUPLICATE KEY UPDATE` for upsert operations.
 * Compatible with MySQL 5.7+ and MariaDB 10.2+.
 *
 * @param dataSource The JDBC DataSource for MySQL connections
 * @param tableName Name of the table to store checkpoints (default: "agent_checkpoints")
 * @param ttlSeconds Optional TTL for checkpoint entries in seconds (null = no expiration)
 * @param migrator Schema migrator for creating/updating the table
 * @param json JSON serializer instance for checkpoint serialization
 */
public class MySQLJdbcPersistenceStorageProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "agent_checkpoints",
    ttlSeconds: Long? = null,
    migrator: SQLPersistenceSchemaMigrator = MySQLJdbcPersistenceSchemaMigrator(dataSource, tableName),
    json: Json = defaultJson
) : JdbcPersistenceStorageProvider(dataSource, migrator, ttlSeconds, tableName) {

    override val upsertSql: String = """
        INSERT INTO $tableName (persistence_id, checkpoint_id, created_at, checkpoint_json, ttl_timestamp, version)
        VALUES (?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            created_at = VALUES(created_at),
            checkpoint_json = VALUES(checkpoint_json),
            ttl_timestamp = VALUES(ttl_timestamp),
            version = VALUES(version)
    """.trimIndent()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] PostgresJdbcPersistenceStorageProvider
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-persistence-jdbc/src/main/kotlin/ai/koog/agents/features/persistence/jdbc/PostgresJdbcPersistenceStorageProvider.kt
Score: 0,000
Responsibility: Provides PostgreSQL-specific JDBC implementation for persistence storage.
Key methods: constructor(dataSource: DataSource, tableName: String?, ttlSeconds: Long?, migrator: SQLPersistenceSchemaMigrator?, json: Json?), upsertSql()

--- PostgresJdbcPersistenceStorageProvider [ИСТОЧНИК 4] (line 1) ---
// File: PostgresJdbcPersistenceStorageProvider.kt
package ai.koog.agents.features.persistence.jdbc

import ai.koog.agents.features.sql.providers.SQLPersistenceSchemaMigrator
import kotlinx.serialization.json.Json
import javax.sql.DataSource

/**
 * PostgreSQL-specific JDBC implementation of [JdbcPersistenceStorageProvider].
 *
 * Uses `INSERT ... ON CONFLICT (...) DO UPDATE` for upsert operations.
 *
 * @param dataSource The JDBC DataSource for PostgreSQL connections
 * @param tableName Name of the table to store checkpoints (default: "agent_checkpoints")
 * @param ttlSeconds Optional TTL for checkpoint entries in seconds (null = no expiration)
 * @param migrator Schema migrator for creating/updating the table
 * @param json JSON serializer instance for checkpoint serialization
 */
public class PostgresJdbcPersistenceStorageProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "agent_checkpoints",
    ttlSeconds: Long? = null,
    migrator: SQLPersistenceSchemaMigrator = PostgresJdbcPersistenceSchemaMigrator(dataSource, tableName),
    json: Json = defaultJson
) : JdbcPersistenceStorageProvider(dataSource, migrator, ttlSeconds, tableName) {

    override val upsertSql: String = """
        INSERT INTO $tableName (persistence_id, checkpoint_id, created_at, checkpoint_json, ttl_timestamp, version)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (persistence_id, checkpoint_id) DO UPDATE SET
            created_at = EXCLUDED.created_at,
            checkpoint_json = EXCLUDED.checkpoint_json,
            ttl_timestamp = EXCLUDED.ttl_timestamp,
            version = EXCLUDED.version
    """.trimIndent()
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] NoMemory
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/NoMemory.kt
Score: 0,000
Responsibility: Provides an implementation of AgentMemoryProvider that does nothing and logs a message when memory feature is accessed.
Key methods: save(fact: Fact, subject: MemorySubject, scope: MemoryScope), load(concept: Concept, subject: MemorySubject, scope: MemoryScope), loadAll(subject: MemorySubject, scope: MemoryScope), loadByDescription(description: String, subject: MemorySubject, scope: MemoryScope)

--- NoMemory [ИСТОЧНИК 5] (line 1) ---
// File: NoMemory.kt
package ai.koog.agents.memory.providers

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Implementation of [AgentMemoryProvider] that does nothing and logs that memory feature is not enabled
 */
public object NoMemory : AgentMemoryProvider {
    private val logger = KotlinLogging.logger { }

    override suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope) {
        logger.info {
            "Memory feature is not enabled in the agent. Skipping saving fact for concept '${fact.concept.keyword}'"
        }
    }

    override suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        logger.info {
            "Memory feature is not enabled in the agent. No facts will be loaded for concept '${concept.keyword}'"
        }
        return emptyList()
    }

    override suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact> {
        logger.info { "Memory feature is not enabled in the agent. No facts will be loaded" }
        return emptyList()
    }

    override suspend fun loadByDescription(
        description: String,
        subject: MemorySubject,
        scope: MemoryScope
    ): List<Fact> {
        logger.info {
            "Memory feature is not enabled in the agent. No facts will be loaded for question: '$description'"
        }
        return emptyList()
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] ExecutionPointNode.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/ExecutionPointNode.kt
[ИСТОЧНИК 2] RegisteredFeature.kt · RegisteredFeature · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
[ИСТОЧНИК 3] MySQLJdbcPersistenceStorageProvider.kt · MySQLJdbcPersistenceStorageProvider · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-persistence-jdbc/src/main/kotlin/ai/koog/agents/features/persistence/jdbc/MySQLJdbcPersistenceStorageProvider.kt
[ИСТОЧНИК 4] PostgresJdbcPersistenceStorageProvider.kt · PostgresJdbcPersistenceStorageProvider · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-persistence-jdbc/src/main/kotlin/ai/koog/agents/features/persistence/jdbc/PostgresJdbcPersistenceStorageProvider.kt
[ИСТОЧНИК 5] NoMemory.kt · NoMemory · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/NoMemory.kt


---
## Query 28: "расскажи все про SubgraphMetadata"
**Optimized:** "subgraphmetadata details"
**Metrics:** Retrieved: 8 → Filtered: 8 → Final: 5
**Timings:** query_optimize=118ms, retrieve=43ms, filter=0ms, rerank=1564ms, top_k=0ms, pack=6ms
**Top score:** 0,95 | Avg score: 0,45

### RAG Context:
Found 4 relevant class(es) | ~8580 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SubgraphMetadata
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/SubgraphMetadata.kt
Score: 0,950
Responsibility: Stores and provides metadata about a subgraph in an AI agent strategy graph.
Key methods: equals(other: Any?), hashCode(), toString()

--- holds [ИСТОЧНИК 1] (line 1) ---
// File: SubgraphMetadata.kt
package ai.koog.agents.core.agent.entity

/**
 * Represents metadata associated with a subgraph in an AI agent strategy graph.
 *
 * This class holds information about the nodes present in the subgraph and provides
 * insights into the structural uniqueness of node names within the graph. The subgraph
 * is identified by a map of node names to their corresponding `AIAgentNodeBase` implementations.
 *
 * @property nodesMap A map where the keys are node names (String) and the values are the corresponding
 * AI agent nodes (`AIAgentNodeBase`). This map represents the structural composition
 * of the subgraph.
 *
 * @property uniqueNames A boolean flag indicating if node names within the subgraph are unique. If `true`,
 * all node names in the `nodesMap` are guaranteed to be distinct, which can be useful
 * for ensuring consistency and avoiding conflicts in subgraph operations.
 */
public data class SubgraphMetadata(
    val nodesMap: Map<String, AIAgentNodeBase<*, *>>,
    val uniqueNames: Boolean = false,
)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphPipeline
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
Score: 0,400
Responsibility: Manages the execution of AI agent graph nodes using registered handlers.
Key methods: install(feature: AIAgentGraphFeature, configure: TConfig.() -> Unit), triggerNodeHandlersBeforeExecution(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken)

--- manages [ИСТОЧНИК 2] (line 147) ---
// File: AIAgentGraphPipeline.kt
@param executionInfo The execution information for the agent environment transformation event
     * @param subgraph The subgraph that was executed.
     * @param context The agent context in which the subgraph was executed.
     * @param input The input data provided to the subgraph.
     * @param inputType The type of the input data provided to the subgraph.
     * @param output The output data produced by the subgraph execution.
     * @param outputType The type of the output data produced by the subgraph execution.
     */
    @InternalAgentsApi
    public open override suspend fun onSubgraphExecutionCompleted(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        subgraph: AIAgentSubgraphBase<*, *>,
        context: AIAgentGraphContextBase,
        input: Any?,
        inputType: TypeToken,
        output: Any?,
        outputType: TypeToken,
    )

    /**
     * Notifies all registered subgraph handlers when a subgraph execution fails.
     *
     * @param eventId The unique identifier for the event group.
     * @param executionInfo The execution information for the agent environment transformation event
     * @param subgraph The subgraph for which the execution failed.
     * @param context The agent context in which the subgraph execution occurred.
     * @param input The input data that was provided to the subgraph when it failed.
     * @param inputType The type of the input data provided to the subgraph.
     * @param throwable The exception or error that caused the subgraph execution to fail.
     */
    @InternalAgentsApi
    public open override suspend fun onSubgraphExecutionFailed(
        eventId: String,
        executionInfo: AgentExecutionInfo,
        subgraph: AIAgentSubgraphBase<*, *>,
        context: AIAgentGraphContextBase,
        input: Any?,
        inputType: TypeToken,
        throwable: Throwable
    )

    //endregion Trigger Subgraph Handlers

    //region Interceptors

    /**
     * Intercepts node executi

--- manages [ИСТОЧНИК 3] (line 268) ---
// File: AIAgentGraphPipeline.kt
am feature The AI agent graph feature that specifies the feature to intercept.
     * @param handle A suspendable function that handles the subgraph execution completion event,
     * taking the event context as a parameter.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
     *     logger.info("Subgraph ${eventContext.subgraph.name} executed with input: ${eventContext.input} and produced output: ${eventContext.output}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionCompleted(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionCompletedContext) -> Unit
    )

    /**
     * Intercepts and handles subgraph execution failures for a given feature.
     *
     * @param feature The feature associated with this handler.
     * @param handle A suspend function that processes the subgraph execution failure event.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionFailed(feature) { eventContext ->
     *     logger.error("Subgraph ${eventContext.subgraph.name} execution failed with error: ${eventContext.throwable}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionFailed(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionFailedContext) -> Unit
    )

    //endregion Interceptors
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] subgraphExecutionEvents
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/subgraphExecutionEvents.kt
Score: 0,300
Responsibility: Handles events related to subgraph execution start and completion.
Key methods: SubgraphExecutionStartingEvent(eventId: String, executionInfo: AgentExecutionInfo, runId: String, subgraphName: String, input: JSONElement?, timestamp: Long), SubgraphExecutionCompletedEvent(eventId: String, executionInfo: AgentExecutionInfo, runId: String)

--- SubgraphExecutionStartingEvent [ИСТОЧНИК 4] (line 38) ---
// File: subgraphExecutionEvents.kt
onInfo, runId, subgraphName, input, timestamp)")
    )
    public constructor(
        runId: String,
        subgraphName: String,
        input: JSONElement?,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : this(
        eventId = SubgraphExecutionStartingEvent::class.simpleName.toString(),
        executionInfo = AgentExecutionInfo(
            parent = null,
            partName = SubgraphExecutionStartingEvent::class.simpleName.toString(),
        ),
        runId = runId,
        subgraphName = subgraphName,
        input = input,
        timestamp = timestamp
    )
}

/**
 * Represents an event triggered when the execution of a specific subgraph completes.
 *
 * @property eventId A unique identifier for the event or a group of events;
 * @property executionInfo Provides contextual information about the execution associated with this event.
 * @property runId Unique identifier for the subgraph run;
 * @property subgraphName The name of the subgraph being executed;
 * @property input The input data provided to the subgraph;
 * @property output The output data generated by the subgraph;
 * @property timestamp The timestamp of the event, in milliseconds since the Unix epoch.
 */
@Serializable
public data class SubgraphExecutionCompletedEvent(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val runId: String,
    val subgraphName: String,
    val input: JSONElement?,
    val output: JSONElement?,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) : DefinedFeatureEvent() {

    /**
     * @deprecated Use constructor with [executionInfo] parameter
     */
    @Deprecated(
        message = "Use constructor with executionInfo parameter",
        replaceWith = ReplaceWith("SubgraphExecutionCompletedEvent(executionInfo, runId, subgraphName, input, output, timestamp)")
    )
    public constructor(
        runId: String,
        subgraphName: String,
        input: JSONEleme

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SubgraphExecutionEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt
Score: 0,250
Responsibility: Defines event contexts for handling subgraph execution lifecycle events.
Key methods: SubgraphExecutionStartingContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken), SubgraphExecutionCompletedContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, output: Any?, inputType: TypeToken, outputType: TypeToken), SubgraphExecutionFailedContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken)

--- SubgraphExecutionEventContext [ИСТОЧНИК 5] (line 38) ---
// File: SubgraphExecutionEventContext.kt
ecution information containing parentId and current execution path;
 * @property subgraph The subgraph instance that was executed.
 * @property context The context in which the subgraph was executed.
 * @property input The input data for the subgraph execution.
 * @property output The output data from the subgraph execution.
 * @property inputType The type of the input data for the subgraph execution.
 * @property outputType The type of the output data for the subgraph execution.
 */
public data class SubgraphExecutionCompletedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val subgraph: AIAgentSubgraphBase<*, *>,
    val context: AIAgentGraphContextBase,
    val input: Any?,
    val output: Any?,
    val inputType: TypeToken,
    val outputType: TypeToken,
) : SubgraphExecutionEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.SubgraphExecutionCompleted
}

/**
 * The context for handling a subgraph execution failed event.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property subgraph The subgraph instance that failed to execute.
 * @property context The context in which the subgraph failed to execute.
 * @property input The input data for the subgraph execution.
 * @property inputType The type of the input data for the subgraph execution.
 * @property throwable The exception that caused the subgraph execution to fail.
 */
public data class SubgraphExecutionFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val subgraph: AIAgentSubgraphBase<*, *>,
    val context: AIAgentGraphContextBase,
    val input: Any?,
    val inputType: TypeToken,
    val throwable: Throwable
) : SubgraphExecutionEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.SubgraphExecutionFailed
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] SubgraphMetadata.kt · holds · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/SubgraphMetadata.kt
[ИСТОЧНИК 2] AIAgentGraphPipeline.kt · manages · line 147 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 3] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 4] subgraphExecutionEvents.kt · SubgraphExecutionStartingEvent · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/subgraphExecutionEvents.kt
[ИСТОЧНИК 5] SubgraphExecutionEventContext.kt · SubgraphExecutionEventContext · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt


---
## Query 29: "примеры использования AIAgentSubgraphBuilder"
**Optimized:** "examples of usage aiagentsubgraphbuilder"
**Metrics:** Retrieved: 12 → Filtered: 12 → Final: 5
**Timings:** query_optimize=155ms, retrieve=48ms, filter=0ms, rerank=2523ms, top_k=0ms, pack=6ms
**Top score:** 0,95 | Avg score: 0,89

### RAG Context:
Found 4 relevant class(es) | ~9556 chars

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphExt
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
Score: 0,950
Responsibility: Manages subgraphs and tasks in a controlled, structured manner.
Key methods: nodeLLMRequestMultiple(), setToolChoiceRequired(), toSafeResult(), toolResultKindToJSON()

--- providing [ИСТОЧНИК 1] (line 286) ---
// File: AIAgentSubgraphExt.kt
ucted subgraph for the specified task.
 */
@OptIn(InternalAgentsApi::class)
@AIAgentBuilderDslMarker
@InternalAgentsApi
public fun <Input : Any, OutputTransformed : Any> subgraphWithTask(
    inputType: TypeToken,
    toolSelectionStrategy: ToolSelectionStrategy,
    finishTool: Tool<*, OutputTransformed>,
    name: String? = null,
    llmModel: LLModel? = null,
    llmParams: LLMParams? = null,
    runMode: ToolCalls = ToolCalls.SEQUENTIAL,
    assistantResponseRepeatMax: Int? = null,
    responseProcessor: ResponseProcessor? = null,
    defineTask: suspend AIAgentGraphContextBase.(input: Input) -> String
): AIAgentSubgraphDelegate<Input, OutputTransformed> = subgraph<Input, OutputTransformed>(
    inputType = inputType,
    outputType = inputType,
    name = name,
    toolSelectionStrategy = toolSelectionStrategy,
    llmModel = llmModel,
    llmParams = llmParams,
    responseProcessor = responseProcessor,
) {
    setupSubgraphWithTask(
        finishTool = finishTool,
        inputType = inputType,
        outputTransformedType = finishTool.resultType,
        runMode = runMode,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        defineTask = defineTask,
    )
}

/**
 * Defines a subgraph with a specific task to be performed by an AI agent.
 *
 * @param Input The input type provided to the subgraph.
 * @param Output The output type returned by the subgraph.
 * @param OutputTransformed The transformed output type after finishing the task.
 * @param toolSelectionStrategy The strategy to be used for selecting tools within the subgraph.
 * @param finishTool The tool responsible for finalizing the task and producing the transformed output.
 * @param name An optional name for the subgraph. Defaults to null if not provided.
 * @param llmModel The optional language model to be used in the subgraph for processing requests.
 * @param llmParams The optional parameters to customize the behavior of the language model.
 * @param runMode The mode in which

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentSubgraphBuilder
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
Score: 0,920
Responsibility: Constructs AI agent subgraphs by defining and connecting nodes.
Key methods: nodeStart(), nodeFinish()

--- for [ИСТОЧНИК 2] (line 37) ---
// File: AIAgentSubgraphBuilder.kt
ships between subgraph nodes.
 *
 * @param Input The input type expected by the starting node of the subgraph.
 * @param Output The output type produced by the finishing node of the subgraph.
 */
@AIAgentBuilderDslMarker
public abstract class AIAgentSubgraphBuilderBase<Input, Output> {
    /**
     * Represents the starting node of the subgraph in the AI agent's strategy graph.
     *
     * This property holds a reference to a `StartAIAgentNodeBase` instance, which acts as the
     * entry point for the subgraph. It is used to define the initial step in the processing
     * pipeline for input data and is integral to the construction of the subgraph.
     *
     * @param Input The type of input data that this starting node processes.
     */
    public abstract val nodeStart: StartNode<Input>

    /**
     * Represents the "finish" node in the AI agent's subgraph structure. This node indicates
     * the endpoint of the subgraph and acts as a terminal stage where the workflow stops.
     *
     * The `nodeFinish` property is an abstract member that subclasses must define. It is of type
     * `FinishAIAgentNodeBase`, which is a specialized node that directly passes its input to its
     * output without modification as part of an identity operation.
     *
     * This node does not allow outgoing edges and cannot be linked further in the graph.
     * It serves as the final node responsible for receiving and producing data of the defined
     * output type.
     *
     * @param Output The type of data processed and produced by this node.
     */
    public abstract val nodeFinish: FinishNode<Output>

    /**
     * Creates an edge between nodes.
     * @param edgeIntermediate Intermediate edge builder
     */
    public fun <IncomingOutput, OutgoingInput, CompatibleOutput : OutgoingInput> edge(
        edgeIntermediate: AIAgentEdgeBuilderIntermediate<IncomingOutput, CompatibleOutput, OutgoingInput>
    ): Unit = edge(AIAgentEdgeBuilder(edgeIntermediate).build())

--- for [ИСТОЧНИК 3] (line 194) ---
// File: AIAgentSubgraphBuilder.kt
onStrategy,
    private val llmModel: LLModel?,
    private val llmParams: LLMParams?,
    private val responseProcessor: ResponseProcessor? = null,
) : AIAgentSubgraphBuilderBase<Input, Output>(),
    BaseBuilder<AIAgentSubgraphDelegate<Input, Output>> {
    override val nodeStart: StartNode<Input> = StartNode(subgraphName = name, type = inputType)
    override val nodeFinish: FinishNode<Output> = FinishNode(subgraphName = name, type = outputType)

    /**
     * Constructs an instance of AIAgentSubgraphBuilder with the provided parameters, using KTypes
     * for input and output type representation.
     *
     * This constructor is deprecated. All [KType] parameters should be replaced by the use of [TypeToken] instead.
     *
     * @param name An optional name for the subgraph being built.
     * @param inputType The type of the input data for the subgraph, represented as a [KType].
     * @param outputType The type of the output data for the subgraph, represented as a [KType].
     * @param toolSelectionStrategy The strategy used to select the tools for this subgraph.
     * @param llmModel An optional Large Language Model ([LLModel]) to be used within the subgraph.
     * @param llmParams An optional set of parameters ([LLMParams]) for configuring the LLM behavior.
     * @param responseProcessor An optional [ResponseProcessor] for post-processing responses in the subgraph.
     */
    @Deprecated("KTypes usage in graphs and nodes is deprecated. Please, use TypeTokens instead.")
    public constructor(
        name: String? = null,
        inputType: KType,
        outputType: KType,
        toolSelectionStrategy: ToolSelectionStrategy,
        llmModel: LLModel?,
        llmParams: LLMParams?,
        responseProcessor: ResponseProcessor? = null,
    ) : this(
        name,
        typeToken(inputType),
        typeToken(outputType),
        toolSelectionStrategy,
        llmModel,
        llmParams,
        responseProcessor
    )

    override fun build():

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] AIAgentGraphPipeline
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
Score: 0,850
Responsibility: Manages the execution of AI agent graph nodes using registered handlers.
Key methods: install(feature: AIAgentGraphFeature, configure: TConfig.() -> Unit), triggerNodeHandlersBeforeExecution(eventId: String, executionInfo: AgentExecutionInfo, node: AIAgentNodeBase, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken)

--- manages [ИСТОЧНИК 4] (line 268) ---
// File: AIAgentGraphPipeline.kt
am feature The AI agent graph feature that specifies the feature to intercept.
     * @param handle A suspendable function that handles the subgraph execution completion event,
     * taking the event context as a parameter.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
     *     logger.info("Subgraph ${eventContext.subgraph.name} executed with input: ${eventContext.input} and produced output: ${eventContext.output}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionCompleted(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionCompletedContext) -> Unit
    )

    /**
     * Intercepts and handles subgraph execution failures for a given feature.
     *
     * @param feature The feature associated with this handler.
     * @param handle A suspend function that processes the subgraph execution failure event.
     *
     * Example:
     * ```
     * pipeline.interceptSubgraphExecutionFailed(feature) { eventContext ->
     *     logger.error("Subgraph ${eventContext.subgraph.name} execution failed with error: ${eventContext.throwable}")
     * }
     * ```
     */
    public open override fun interceptSubgraphExecutionFailed(
        feature: AIAgentGraphFeature<*, *>,
        handle: suspend (eventContext: SubgraphExecutionFailedContext) -> Unit
    )

    //endregion Interceptors
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] SubgraphExecutionEventContext
File: /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt
Score: 0,850
Responsibility: Defines event contexts for handling subgraph execution lifecycle events.
Key methods: SubgraphExecutionStartingContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken), SubgraphExecutionCompletedContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, output: Any?, inputType: TypeToken, outputType: TypeToken), SubgraphExecutionFailedContext(eventId: String, executionInfo: AgentExecutionInfo, subgraph: AIAgentSubgraphBase<*, *>, context: AIAgentGraphContextBase, input: Any?, inputType: TypeToken)

--- SubgraphExecutionEventContext [ИСТОЧНИК 5] (line 38) ---
// File: SubgraphExecutionEventContext.kt
ecution information containing parentId and current execution path;
 * @property subgraph The subgraph instance that was executed.
 * @property context The context in which the subgraph was executed.
 * @property input The input data for the subgraph execution.
 * @property output The output data from the subgraph execution.
 * @property inputType The type of the input data for the subgraph execution.
 * @property outputType The type of the output data for the subgraph execution.
 */
public data class SubgraphExecutionCompletedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val subgraph: AIAgentSubgraphBase<*, *>,
    val context: AIAgentGraphContextBase,
    val input: Any?,
    val output: Any?,
    val inputType: TypeToken,
    val outputType: TypeToken,
) : SubgraphExecutionEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.SubgraphExecutionCompleted
}

/**
 * The context for handling a subgraph execution failed event.
 *
 * @property executionInfo The execution information containing parentId and current execution path;
 * @property subgraph The subgraph instance that failed to execute.
 * @property context The context in which the subgraph failed to execute.
 * @property input The input data for the subgraph execution.
 * @property inputType The type of the input data for the subgraph execution.
 * @property throwable The exception that caused the subgraph execution to fail.
 */
public data class SubgraphExecutionFailedContext(
    override val eventId: String,
    override val executionInfo: AgentExecutionInfo,
    val subgraph: AIAgentSubgraphBase<*, *>,
    val context: AIAgentGraphContextBase,
    val input: Any?,
    val inputType: TypeToken,
    val throwable: Throwable
) : SubgraphExecutionEventContext {
    override val eventType: AgentLifecycleEventType = AgentLifecycleEventType.SubgraphExecutionFailed
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## Источники
[ИСТОЧНИК 1] AIAgentSubgraphExt.kt · providing · line 286 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 2] AIAgentSubgraphBuilder.kt · for · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 3] AIAgentSubgraphBuilder.kt · for · line 194 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 4] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 5] SubgraphExecutionEventContext.kt · SubgraphExecutionEventContext · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt


---
## Summary
| Метрика | Значение |
|---------|---------|
| Всего вопросов | 29 |
| Avg top score | 0,88 |
| retrievalTopK | 15 |
| threshold | 0.33 |
| rerankStrategy | LLM |
| finalTopK | 5 |
