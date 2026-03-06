package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.MemoryKey
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Implementation of TaskStateStore that provides:
 * - In-memory caching for fast access
 * - Long-Term Memory (LTM) persistence via AgentMemoryRepository
 * - Backward compatibility with legacy format
 * 
 * This implementation stores state in both new and legacy format during
 * the migration period to ensure backward compatibility.
 */
class TaskStateStoreImpl @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json
) : TaskStateStore {
    
    companion object {
        // New format key for storing complete state
        private const val STATE_FORMAT_KEY = "task_state_data"
        private const val STATE_VERSION = "v1"
        
        // Legacy keys for backward compatibility
        private const val LEGACY_STATE_KEY = TaskMemoryKeys.CURRENT_STATE
        private const val LEGACY_PLANNING_KEY = "planning_result"
        private const val LEGACY_EXECUTION_KEY = "execution_results"
        private const val LEGACY_VERIFICATION_KEY = "verification_results"
        
        // Memory category for LTM
        private const val MEMORY_CATEGORY = "task"
    }
    
    // In-memory cache for fast access
    private val stateCache = mutableMapOf<String, TaskStateData>()
    
    override suspend fun getState(agentId: String): TaskStateData? {
        // Check cache first
        stateCache[agentId]?.let { return it }
        
        // Try new format
        val newState = loadNewFormat(agentId)
        if (newState != null) {
            stateCache[agentId] = newState
            return newState
        }
        
        // Fallback to legacy format
        val legacyState = loadLegacyFormat(agentId)
        if (legacyState != null) {
            // Migrate to new format
            saveNewFormat(agentId, legacyState)
            stateCache[agentId] = legacyState
            return legacyState
        }
        
        return null
    }
    
    override suspend fun updateState(agentId: String, update: TaskStateUpdate): TaskStateData {
        val currentState = getState(agentId) ?: TaskStateData.initial(
            (update as? TaskStateUpdate.StartTask)?.userTask ?: ""
        )
        
        val newState = applyUpdate(currentState, update)
        
        // Save in both formats during migration
        saveNewFormat(agentId, newState)
        saveLegacyFormat(agentId, newState)
        
        stateCache[agentId] = newState
        return newState
    }
    
    override suspend fun clearState(agentId: String) {
        stateCache.remove(agentId)
        
        // Clear new format
        agentMemoryRepository.deleteFact(
            agentId.toLongOrNull() ?: return,
            STATE_FORMAT_KEY,
            MEMORY_CATEGORY
        )
        
        // Clear legacy keys
        agentMemoryRepository.deleteFact(
            agentId.toLongOrNull() ?: return,
            LEGACY_STATE_KEY,
            MEMORY_CATEGORY
        )
    }
    
    override fun getStateInMemory(agentId: String): TaskStateData? {
        return stateCache[agentId]
    }
    
    override suspend fun hasState(agentId: String): Boolean {
        return getState(agentId) != null
    }
    
    override suspend fun reloadState(agentId: String): TaskStateData? {
        stateCache.remove(agentId)
        return getState(agentId)
    }
    
    override suspend fun saveMemory(agentId: String, key: MemoryKey, value: String) {
        val agentIdLong = agentId.toLongOrNull() ?: return
        agentMemoryRepository.upsertFact(
            agentId = agentIdLong,
            memoryKey = key.key,
            category = MEMORY_CATEGORY,
            fact = value
        )
    }
    
    override suspend fun getMemory(agentId: String, key: MemoryKey): String? {
        val agentIdLong = agentId.toLongOrNull() ?: return null
        return agentMemoryRepository.getFactByKey(
            agentId = agentIdLong,
            memoryKey = key.key
        )?.fact
    }
    
    override suspend fun clearMemory(agentId: String, key: MemoryKey) {
        val agentIdLong = agentId.toLongOrNull() ?: return
        agentMemoryRepository.deleteFact(
            agentId = agentIdLong,
            memoryKey = key.key,
            category = MEMORY_CATEGORY
        )
    }
    
    // Private helper methods
    
    private suspend fun loadNewFormat(agentId: String): TaskStateData? {
        val agentIdLong = agentId.toLongOrNull() ?: return null
        val fact = agentMemoryRepository.getFactByKey(agentIdLong, STATE_FORMAT_KEY)
            ?: return null
        
        return try {
            json.decodeFromString<TaskStateData>(fact.fact)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveNewFormat(agentId: String, state: TaskStateData) {
        val agentIdLong = agentId.toLongOrNull() ?: return
        val jsonStr = json.encodeToString(TaskStateData.serializer(), state)
        agentMemoryRepository.upsertFact(
            agentId = agentIdLong,
            memoryKey = STATE_FORMAT_KEY,
            category = MEMORY_CATEGORY,
            fact = jsonStr
        )
    }
    
    private suspend fun loadLegacyFormat(agentId: String): TaskStateData? {
        val agentIdLong = agentId.toLongOrNull() ?: return null
        
        val stateStr = agentMemoryRepository.getFactByKey(agentIdLong, LEGACY_STATE_KEY)?.fact
            ?: return null
        
        return try {
            parseLegacyState(stateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveLegacyFormat(agentId: String, state: TaskStateData) {
        val agentIdLong = agentId.toLongOrNull() ?: return
        
        // Save state string
        agentMemoryRepository.upsertFact(
            agentId = agentIdLong,
            memoryKey = LEGACY_STATE_KEY,
            category = MEMORY_CATEGORY,
            fact = state.state.name
        )
        
        // Save planning result
        when (state) {
            is TaskStateData.Planning -> {
                state.planningResult?.let { result ->
                    val jsonStr = json.encodeToString(PlanningData.serializer(), result)
                    agentMemoryRepository.upsertFact(
                        agentId = agentIdLong,
                        memoryKey = LEGACY_PLANNING_KEY,
                        category = MEMORY_CATEGORY,
                        fact = jsonStr
                    )
                }
            }
            is TaskStateData.Execution -> {
                val jsonStr = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(ExecutionStep.serializer()),
                    state.executionResults
                )
                agentMemoryRepository.upsertFact(
                    agentId = agentIdLong,
                    memoryKey = LEGACY_EXECUTION_KEY,
                    category = MEMORY_CATEGORY,
                    fact = jsonStr
                )
            }
            is TaskStateData.Verification -> {
                state.verificationResults?.let { results ->
                    val jsonStr = json.encodeToString(VerificationData.serializer(), results)
                    agentMemoryRepository.upsertFact(
                        agentId = agentIdLong,
                        memoryKey = LEGACY_VERIFICATION_KEY,
                        category = MEMORY_CATEGORY,
                        fact = jsonStr
                    )
                }
            }
            else -> {
                // INIT and DONE states don't need additional legacy data
            }
        }
    }
    
    private fun applyUpdate(currentState: TaskStateData, update: TaskStateUpdate): TaskStateData {
        return when (update) {
            is TaskStateUpdate.StartTask -> {
                TaskStateData.Init(userTask = update.userTask)
            }
            
            is TaskStateUpdate.SetPlanningError -> {
                if (currentState is TaskStateData.Planning) {
                    currentState.copy(
                        errorMessage = update.error,
                        retryCount = update.retryCount
                    )
                } else currentState
            }
            
            is TaskStateUpdate.TransitionToPlanning -> {
                TaskStateData.Planning(
                    userTask = currentState.userTask,
                    planningResult = update.planningResult
                )
            }
            
            is TaskStateUpdate.TransitionToExecution -> {
                TaskStateData.Execution(
                    userTask = currentState.userTask,
                    plan = update.plan
                )
            }
            
            is TaskStateUpdate.StartStep -> {
                if (currentState is TaskStateData.Execution) {
                    val newSteps = currentState.executionResults.toMutableList()
                    if (update.stepNumber < newSteps.size) {
                        newSteps[update.stepNumber] = newSteps[update.stepNumber].copy(
                            status = StepStatus.RUNNING,
                            startedAt = System.currentTimeMillis()
                        )
                    }
                    currentState.copy(
                        executionResults = newSteps,
                        currentStep = update.stepNumber,
                        isStepRunning = true
                    )
                } else currentState
            }
            
            is TaskStateUpdate.CompleteStep -> {
                if (currentState is TaskStateData.Execution) {
                    val newSteps = currentState.executionResults.toMutableList()
                    if (update.stepNumber < newSteps.size) {
                        newSteps[update.stepNumber] = newSteps[update.stepNumber].copy(
                            result = update.result,
                            status = update.status,
                            completedAt = System.currentTimeMillis()
                        )
                    }
                    currentState.copy(
                        executionResults = newSteps,
                        isStepRunning = false
                    )
                } else currentState
            }
            
            is TaskStateUpdate.FailStep -> {
                if (currentState is TaskStateData.Execution) {
                    val newSteps = currentState.executionResults.toMutableList()
                    if (update.stepNumber < newSteps.size) {
                        newSteps[update.stepNumber] = newSteps[update.stepNumber].copy(
                            error = update.error,
                            status = StepStatus.FAILED,
                            completedAt = System.currentTimeMillis()
                        )
                    }
                    currentState.copy(
                        executionResults = newSteps,
                        isStepRunning = false
                    )
                } else currentState
            }
            
            is TaskStateUpdate.TransitionToVerification -> {
                if (currentState is TaskStateData.Execution) {
                    TaskStateData.Verification(
                        userTask = currentState.userTask,
                        plan = currentState.plan,
                        executionResults = update.executionResults
                    )
                } else currentState
            }
            
            is TaskStateUpdate.SetVerificationResults -> {
                if (currentState is TaskStateData.Verification) {
                    currentState.copy(verificationResults = update.verificationResults)
                } else currentState
            }
            
            is TaskStateUpdate.CompleteWithResult -> {
                TaskStateData.Done(
                    userTask = currentState.userTask,
                    finalResult = update.finalResult,
                    verificationPassed = update.verificationPassed
                )
            }
        }
    }
    
    /**
     * Parse legacy state format to TaskStateData.
     * This is a best-effort migration from the old string-based format.
     */
    private suspend fun parseLegacyState(stateStr: String): TaskStateData? {
        val agentIdLong = stateStr.hashCode().toLong() // Placeholder - not used in legacy
        
        return when (stateStr.uppercase()) {
            "INIT" -> TaskStateData.Init(userTask = "")
            "PLANNING" -> {
                // Try to load planning result from legacy key
                val planningJson = agentMemoryRepository.getFactByKey(
                    agentIdLong,
                    LEGACY_PLANNING_KEY
                )?.fact
                val planningResult = planningJson?.let {
                    try {
                        json.decodeFromString<PlanningData>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                TaskStateData.Planning(
                    userTask = "",
                    planningResult = planningResult
                )
            }
            "EXECUTION" -> {
                // Try to load execution results from legacy key
                val executionJson = agentMemoryRepository.getFactByKey(
                    agentIdLong,
                    LEGACY_EXECUTION_KEY
                )?.fact
                val executionResults = executionJson?.let {
                    try {
                        json.decodeFromString<List<ExecutionStep>>(it)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
                
                TaskStateData.Execution(
                    userTask = "",
                    plan = PlanningData(
                        taskBreakdown = emptyList(),
                        reasoning = "",
                        estimatedSteps = executionResults.size
                    ),
                    executionResults = executionResults
                )
            }
            "VERIFICATION" -> {
                // Try to load verification results from legacy key
                val verifJson = agentMemoryRepository.getFactByKey(
                    agentIdLong,
                    LEGACY_VERIFICATION_KEY
                )?.fact
                val verifResults = verifJson?.let {
                    try {
                        json.decodeFromString<VerificationData>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                TaskStateData.Verification(
                    userTask = "",
                    plan = PlanningData(emptyList(), "", 0),
                    executionResults = emptyList(),
                    verificationResults = verifResults
                )
            }
            "DONE" -> TaskStateData.Done(
                userTask = "",
                finalResult = "",
                verificationPassed = false
            )
            else -> null
        }
    }
}
