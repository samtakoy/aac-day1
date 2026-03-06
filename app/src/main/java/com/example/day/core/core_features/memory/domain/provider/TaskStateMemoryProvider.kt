package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.MemoryKey
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateMachine
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateStore
import com.example.day.core.core_features.agent.domain.workers.task.prompt.PromptSanitizer
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Factory for creating TaskStateMemoryProvider instances with proper agentId.
 *
 * This class is injectable via DI but requires agentId to be provided at runtime.
 * Use [withAgentId] to create a working MemoryProvider instance.
 * 
 * Updated to support new type-safe TaskStateData while maintaining backward
 * compatibility with legacy string-based state storage.
 */
class TaskStateMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val chat: Chat,
    private val agentId: Long,
    private val taskStateStore: TaskStateStore? = null, // New optional store
    private val json: Json? = null // JSON for parsing new format
) : MemoryProvider {
    
    companion object {
        const val SYSTEM_PROMPT_PREFIX = "Ответ давай на русском языке. " +
            "Твой ответ должен быть валидным JSON объектом без markdown разметки, " +
            "следуя схеме в инструкциях.\n\n"
        
        // New format memory key for complete state
        private const val NEW_STATE_KEY = "task_state_data"
    }

    override suspend fun getMemoryContext(): List<AContextMessage> {
        // First, try to get the new format state
        val newStateData = loadNewFormatState()
        
        if (newStateData != null) {
            // Use new format - build from TaskStateData
            return buildContextFromNewFormat(newStateData)
        }
        
        // Fallback to legacy format
        val facts = agentMemoryRepository.getFacts(agentId)
            .associate { it.memoryKey to it.fact }
        return buildSystemPrompt(facts, agentId)
    }
    
    /**
     * Attempts to load state from new format (TaskStateData JSON).
     */
    private suspend fun loadNewFormatState(): TaskStateData? {
        if (taskStateStore != null) {
            // Use the new TaskStateStore if available
            return taskStateStore.getState(agentId.toString())
        }
        
        // Fallback: try to load directly from LTM
        val jsonInstance = json ?: return null
        val fact = agentMemoryRepository.getFactByKey(agentId, NEW_STATE_KEY)
            ?: return null
        
        return try {
            jsonInstance.decodeFromString<TaskStateData>(fact.fact)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Builds context from new type-safe TaskStateData.
     */
    private suspend fun buildContextFromNewFormat(stateData: TaskStateData): List<AContextMessage> {
        // Convert TaskStateData to facts map for backward compatibility
        val facts = stateDataToFactsMap(stateData)
        
        val sanitizedFacts = PromptSanitizer.sanitizeMap(facts)
        
        val context = TaskContext(
            chat = chat,
            agentId = agentId,
            facts = sanitizedFacts,
            currentState = stateData.toTaskState(),
            currentStep = getCurrentStep(stateData)
        )
        
        val stateMachine = TaskStateMachine()
        val handler = stateMachine.getHandler(stateData.toTaskState())
        val systemPrompt = SYSTEM_PROMPT_PREFIX + handler.buildSystemPrompt(context)
        
        return listOf(
            AContextMessage(
                role = Role.SYSTEM,
                content = systemPrompt
            )
        )
    }
    
    /**
     * Converts TaskStateData to a facts map for backward compatibility
     * with existing prompt builders.
     */
    private fun stateDataToFactsMap(stateData: TaskStateData): Map<String, String> {
        val facts = mutableMapOf<String, String>()
        
        // Always include current state
        facts[TaskMemoryKeys.CURRENT_STATE] = stateData.toTaskState().name
        
        when (stateData) {
            is TaskStateData.Init -> {
                facts[TaskMemoryKeys.USER_TASK] = stateData.userTask
            }
            is TaskStateData.Planning -> {
                facts[TaskMemoryKeys.USER_TASK] = stateData.userTask
                stateData.planningResult?.let { plan ->
                    facts[TaskMemoryKeys.PLANNING] = plan.reasoning
                }
                stateData.errorMessage?.let { error ->
                    facts[TaskMemoryKeys.ERROR] = error
                }
            }
            is TaskStateData.Execution -> {
                facts[TaskMemoryKeys.USER_TASK] = stateData.userTask
                facts[TaskMemoryKeys.PLAN] = stateData.plan.reasoning
                facts[TaskMemoryKeys.CURRENT_STEP] = stateData.currentStep.toString()
                facts[TaskMemoryKeys.TOTAL_STEPS] = stateData.executionResults.size.toString()
                
                // Add execution results
                val resultsJson = stateData.executionResults.joinToString("\n") { step ->
                    "${step.stepNumber}: ${step.description} -> ${step.status.name}"
                }
                facts[TaskMemoryKeys.EXECUTION] = resultsJson
            }
            is TaskStateData.Verification -> {
                facts[TaskMemoryKeys.USER_TASK] = stateData.userTask
                facts[TaskMemoryKeys.EXECUTION_RESULTS] = stateData.executionResults.joinToString(",") { it.stepNumber.toString() }
                stateData.verificationResults?.let { verif ->
                    facts[TaskMemoryKeys.VERIFICATION] = verif.overallPassed.toString()
                    verif.feedback?.let { facts[TaskMemoryKeys.FEEDBACK] = it }
                }
            }
            is TaskStateData.Done -> {
                facts[TaskMemoryKeys.USER_TASK] = stateData.userTask
                facts[TaskMemoryKeys.RESULT] = stateData.finalResult
                facts[TaskMemoryKeys.VERIFICATION_PASSED] = stateData.verificationPassed.toString()
            }
        }
        
        return facts
    }
    
    /**
     * Gets current step from TaskStateData.
     */
    private fun getCurrentStep(stateData: TaskStateData): Int {
        return when (stateData) {
            is TaskStateData.Execution -> stateData.currentStep + 1
            else -> 1
        }
    }

    /**
     * Builds system prompt based on current task state and facts.
     * This method contains the shared logic used by all provider instances.
     *
     * @param facts Map of memory key to value from LTM
     * @param agentId The agent ID for context
     * @return List containing the system message
     */
    internal fun buildSystemPrompt(
        facts: Map<String, String>,
        agentId: Long
    ): List<AContextMessage> {
        val currentState = determineCurrentState(facts)
        val currentStep = facts[TaskMemoryKeys.CURRENT_STEP]?.toIntOrNull() ?: 1

        val sanitizedFacts = PromptSanitizer.sanitizeMap(facts)

        val context = TaskContext(
            chat = chat,
            agentId = agentId,
            facts = sanitizedFacts,
            currentState = currentState,
            currentStep = currentStep
        )

        val stateMachine = TaskStateMachine()
        val handler = stateMachine.getHandler(currentState)
        val systemPrompt = SYSTEM_PROMPT_PREFIX + handler.buildSystemPrompt(context)

        return listOf(
            AContextMessage(
                role = Role.SYSTEM,
                content = systemPrompt
            )
        )
    }

    private fun determineCurrentState(facts: Map<String, String>): TaskState {
        val stateStr = facts[TaskMemoryKeys.CURRENT_STATE]
        return when {
            stateStr == null || stateStr.isBlank() -> TaskState.INIT
            stateStr.equals("DONE", ignoreCase = true) -> TaskState.INIT
            else -> TaskState.fromString(stateStr) ?: TaskState.INIT
        }
    }
    
    // Extension function to convert TaskStateData to TaskState
    private fun TaskStateData.toTaskState(): TaskState {
        return when (this) {
            is TaskStateData.Init -> TaskState.INIT
            is TaskStateData.Planning -> TaskState.PLANNING
            is TaskStateData.Execution -> TaskState.EXECUTION
            is TaskStateData.Verification -> TaskState.VERIFICATION
            is TaskStateData.Done -> TaskState.DONE
        }
    }
}
