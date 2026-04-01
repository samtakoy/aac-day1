package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.state_machine.domain.model.StateId
import com.example.day.core.core_features.state_machine.domain.model.UnknownStateStrategy
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Factory interface for creating state handlers dynamically.
 * TODO не используется
 */
interface TaskStateHandlerFactory {
    /**
     * Create a handler for the given state name.
     * @return handler or null if not supported
     */
    fun createHandler(stateName: StateId): TaskStateHandler?
}

/**
 * Allows configuring states, transitions, handlers, and error handling strategies.
 *
 * Features:
 * - Validation in init block
 * - BFS reachability check for final states
 * - Unknown state handling strategy
 * - Handler factory for custom states
 *
 * Example usage:
 * ```
 * val defaultConfig = TaskStateConfig.default()
 * val customConfig = TaskStateConfig(
 *     states = listOf("init", "planning", "custom", "done"),
 *     transitions = mapOf(
 *         "init" to listOf("planning"),
 *         "planning" to listOf("custom"),
 *         "custom" to listOf("done"),
 *         "done" to emptyList()
 *     ),
 *     handlers = mapOf(
 *         "init" to InitStateHandler(),
 *         "planning" to PlanningStateHandler(),
 *         "custom" to CustomStateHandler(),
 *         "done" to DoneStateHandler()
 *     ),
 *     initialState = "init",
 *     finalStates = listOf("done")
 * )
 * ```
 */
class StateConfig(
    /**
     * List of valid state names.
     */
    val states: List<StateId>,

    /**
     * Transition map: from state -> list of allowed next states.
     */
    val transitions: Map<StateId, List<StateId>> = emptyMap(),

    /**
     * Handler map: state name -> handler instance.
     */
    val handlers: Map<StateId, TaskStateHandler> = emptyMap(),

    /**
     * Serializer map: state name -> serializer for that state.
     * Used for JSON serialization/deserialization of state data.
     */
    val serializers: Map<StateId, KSerializer<out StateData>> = emptyMap(),

    /**
     * Initial state name.
     */
    val initialState: StateId,

    /**
     * Final states (terminal states with no outgoing transitions).
     */
    val finalStates: List<StateId> = emptyList(),

    /**
     * Strategy for handling unknown states.
     */
    val unknownStateStrategy: UnknownStateStrategy = UnknownStateStrategy.FAIL,

    /**
     * Fallback state name when unknown state is encountered.
     */
    val fallbackState: StateId,

    val fallbackStateData: StateData,

    val stateInfoProvider: StateInfoProvider,

    /**
     * Factory for creating custom handlers dynamically.
     */
    val handlerFactory: TaskStateHandlerFactory? = null,

    /**
     * Custom handler for unknown states (when strategy is CUSTOM).
     */
    val customUnknownStateHandler: ((StateId) -> StateData)? = null
) {
    init {
        validate()
    }

    /**
     * Validate configuration integrity.
     * Called automatically in init block.
     */
    private fun validate() {
        validateStatesNonEmpty()
        validateInitialStateExists()
        validateTransitionsSourceStates()
        validateTransitionsTargetStates()
        validateFinalStatesReachability()
        validateHandlersForStates()
        validateFallbackState()
        validateSerializers()
    }

    private fun validateStatesNonEmpty() {
        require(states.isNotEmpty()) { "States list cannot be empty" }
    }

    private fun validateInitialStateExists() {
        require(initialState in states) {
            "Initial state '$initialState' not in states: $states"
        }
    }

    private fun validateTransitionsSourceStates() {
        val allStates = states.toSet()
        transitions.forEach { (from, _) ->
            require(from in allStates) {
                "Transition source '$from' not in states: $states"
            }
        }
    }

    private fun validateTransitionsTargetStates() {
        val allStates = states.toSet()
        transitions.forEach { (_, toList) ->
            require(toList.all { it in allStates }) {
                "Transition targets $toList not in states: $states"
            }
        }
    }

    private fun validateFinalStatesReachability() {
        // BFS check: all final states must be reachable from initialState
        val reachable = mutableSetOf<StateId>()
        val queue = ArrayDeque<StateId>()

        queue.add(initialState)
        reachable.add(initialState)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextStates = transitions[current] ?: emptyList()

            for (next in nextStates) {
                if (next !in reachable) {
                    reachable.add(next)
                    queue.add(next)
                }
            }
        }

        finalStates.forEach { finalState ->
            require(finalState in reachable) {
                "Final state '$finalState' is not reachable from initial state '$initialState'. " +
                "Reachable states: $reachable"
            }
        }
    }

    private fun validateHandlersForStates() {
        val allStates = states.toSet()
        handlers.keys.forEach { handlerState ->
            require(handlerState in allStates) {
                "Handler defined for '$handlerState' which is not in states: $states"
            }
        }
    }

    private fun validateFallbackState() {
        require(fallbackState in states) {
            "Fallback state '$fallbackState' not in states: $states"
        }
    }

    private fun validateSerializers() {
        // Check that serializers are provided for all states (if serializers map is not empty)
        if (serializers.isNotEmpty()) {
            val allStates = states.toSet()
            serializers.keys.forEach { serializerState ->
                require(serializerState in allStates) {
                    "Serializer defined for '$serializerState' which is not in states: $states"
                }
            }
        }
    }

    /**
     * Get serializer for a state.
     */
    fun getSerializer(state: StateId): KSerializer<out StateData> {
        return serializers[state]
            ?: error("No serializer registered for state: '$state'. Available: ${serializers.keys}")
    }

    /**
     * Check if transition from -> to is allowed.
     */
    fun canTransition(from: StateId, to: StateId): Boolean {
        return transitions[from]?.contains(to) == true
    }

    /**
     * Get handler for state.
     * Falls back to handlerFactory if not found in handlers map.
     */
    fun getHandler(state: StateId): TaskStateHandler? {
        return handlers[state] ?: handlerFactory?.createHandler(state)
    }

    /**
     * Get initial state.
     */
    fun resolveInitialState(): StateId = initialState

    /**
     * Check if state is final.
     */
    fun isFinalState(state: StateId): Boolean = state in finalStates

    /**
     * Handle unknown state according to unknownStateStrategy.
     */
    fun handleUnknownState(stateName: StateId): StateData? {
        return when (unknownStateStrategy) {
            UnknownStateStrategy.FAIL ->
                error("Unknown state: $stateName")
            UnknownStateStrategy.FALLBACK -> {
                error("Fallback not set: $stateName")
            }
            UnknownStateStrategy.IGNORE -> null
            UnknownStateStrategy.CUSTOM ->
                customUnknownStateHandler?.invoke(stateName)
        }
    }

    /**
     * Get all available states.
     */
    fun availableStates(): List<StateId> = states

    fun isAvailable(state: StateId): Boolean = availableStates().contains(state)

    fun toValidStateOrNull(state: String): StateId? = StateId.of(state.lowercase()).takeIf { isAvailable(it) }
}

// ============================================================================
// Extension functions for serialization/deserialization
// ============================================================================

/**
 * Serialize TaskStateData to JSON string using config's serializers.
 */
fun StateConfig.serialize(state: StateData): String {
    val serializer = getSerializer(state.state)
    @Suppress("UNCHECKED_CAST")
    return TaskStateJson.encodeToString(serializer as SerializationStrategy<StateData>, state)
}

/**
 * Deserialize JSON string to TaskStateData using config's serializers.
 */
fun StateConfig.deserialize(json: String): StateData {
    val element = TaskStateJson.parseToJsonElement(json)
    val stateName = element.jsonObject["state"]?.jsonPrimitive?.content
        ?: error("Missing 'state' field in TaskStateData JSON")

    val serializer = getSerializer(StateId.of(stateName))
    @Suppress("UNCHECKED_CAST")
    return TaskStateJson.decodeFromString(serializer as DeserializationStrategy<StateData>, json)
}

/**
 * Json instance configured for TaskStateData serialization.
 */
val TaskStateJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }
}
