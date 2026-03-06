package com.example.day.core.core_features.agent.domain.model

import kotlinx.serialization.Serializable

/**
 * Type-safe memory keys for TaskWorker state machine.
 * Replaces string constants from TaskMemoryKeys with type-safe sealed class.
 * Provides compile-time validation and backward compatibility.
 */
@Serializable
sealed class MemoryKey {
    abstract val key: String
    
    // =====================================================
    // Task State Keys (new type-safe format)
    // =====================================================
    
    /** Current task state - INIT, PLANNING, EXECUTION, VERIFICATION, DONE */
    data object TaskState : MemoryKey() {
        override val key = "task_state"
    }
    
    /** Planning result data */
    data object PlanningResult : MemoryKey() {
        override val key = "planning_result"
    }
    
    /** Execution steps results */
    data object ExecutionResults : MemoryKey() {
        override val key = "execution_results"
    }
    
    /** Verification results */
    data object VerificationResults : MemoryKey() {
        override val key = "verification_results"
    }
    
    // =====================================================
    // Simple Legacy Keys (mapped from TaskMemoryKeys)
    // =====================================================
    
    /** Planning state key */
    data object Planning : MemoryKey() {
        override val key = "planning"
    }
    
    /** Execution state key */
    data object Execution : MemoryKey() {
        override val key = "execution"
    }
    
    /** Verification state key */
    data object Verification : MemoryKey() {
        override val key = "verification"
    }
    
    /** User task description */
    data object UserTask : MemoryKey() {
        override val key = "user_task"
    }
    
    /** Error message */
    data object Error : MemoryKey() {
        override val key = "error"
    }
    
    /** Final result */
    data object Result : MemoryKey() {
        override val key = "result"
    }
    
    /** Verification passed flag */
    data object VerificationPassed : MemoryKey() {
        override val key = "verification_passed"
    }
    
    /** Total steps count */
    data object TotalSteps : MemoryKey() {
        override val key = "total_steps"
    }
    
    /** Verification feedback */
    data object Feedback : MemoryKey() {
        override val key = "feedback"
    }
    
    /** Execution results summary */
    data object ExecutionResultsSummary : MemoryKey() {
        override val key = "execution_results"
    }
    
    /** Plan content */
    data object Plan : MemoryKey() {
        override val key = "plan"
    }
    
    // =====================================================
    // Workflow State Keys
    // =====================================================
    
    /** Current workflow state */
    data object CurrentState : MemoryKey() {
        override val key = "workflow:current_state"
    }
    
    /** Current step number */
    data object CurrentStep : MemoryKey() {
        override val key = "workflow:current_step"
    }
    
    // Init artifacts
    data object InitTitle : MemoryKey() {
        override val key = "init:title"
    }
    
    data object InitDescription : MemoryKey() {
        override val key = "init:description"
    }
    
    data object InitGoal : MemoryKey() {
        override val key = "init:goal"
    }
    
    data object InitExpert : MemoryKey() {
        override val key = "init:expert"
    }
    
    // Planning artifacts
    data object PlanTotalStages : MemoryKey() {
        override val key = "plan:total_stages"
    }
    
    data object PlanCurrentStage : MemoryKey() {
        override val key = "plan:current_stage"
    }
    
    // =====================================================
    // Parameterized Keys (require stage/step number)
    // =====================================================
    
    /** Plan stage name (n = stage number) */
    data class PlanStageName(val n: Int) : MemoryKey() {
        override val key = "plan:stage$n"
    }
    
    /** Plan stage description (n = stage number) */
    data class PlanStageDesc(val n: Int) : MemoryKey() {
        override val key = "plan:stage${n}_desc"
    }
    
    /** Plan stage expert (n = stage number) */
    data class PlanStageExpert(val n: Int) : MemoryKey() {
        override val key = "plan:stage${n}_expert"
    }
    
    /** Plan stage status (n = stage number) */
    data class PlanStageStatus(val n: Int) : MemoryKey() {
        override val key = "plan:stage${n}_status"
    }
    
    /** Plan stage artifact (n = stage number) */
    data class PlanStageArtifact(val n: Int) : MemoryKey() {
        override val key = "plan:stage${n}_artifact"
    }
    
    /** Execution stage result (n = stage number) */
    data class ExecStageResult(val n: Int) : MemoryKey() {
        override val key = "exec:stage${n}_result"
    }
    
    /** Verification stage feedback (n = stage number) */
    data class VerifStageFeedback(val n: Int) : MemoryKey() {
        override val key = "verif:stage${n}_feedback"
    }
    
    /** Verification stage score (n = stage number) */
    data class VerifStageScore(val n: Int) : MemoryKey() {
        override val key = "verif:stage${n}_score"
    }
    
    // =====================================================
    // Companion Object for Backward Compatibility
    // =====================================================
    
    companion object {
        /**
         * Parse a string key to MemoryKey.
         * Returns null if the key is not recognized.
         * For backward compatibility with legacy code.
         */
        fun fromString(key: String): MemoryKey? = when (key) {
            // New type-safe keys
            TaskState.key -> TaskState
            PlanningResult.key -> PlanningResult
            ExecutionResults.key -> ExecutionResults
            VerificationResults.key -> VerificationResults
            
            // Simple legacy keys
            Planning.key -> Planning
            Execution.key -> Execution
            Verification.key -> Verification
            UserTask.key -> UserTask
            Error.key -> Error
            Result.key -> Result
            VerificationPassed.key -> VerificationPassed
            TotalSteps.key -> TotalSteps
            Feedback.key -> Feedback
            ExecutionResultsSummary.key -> ExecutionResultsSummary
            Plan.key -> Plan
            
            // Legacy workflow keys
            CurrentState.key -> CurrentState
            CurrentStep.key -> CurrentStep
            
            // Legacy init keys
            InitTitle.key -> InitTitle
            InitDescription.key -> InitDescription
            InitGoal.key -> InitGoal
            InitExpert.key -> InitExpert
            
            // Legacy plan keys
            PlanTotalStages.key -> PlanTotalStages
            PlanCurrentStage.key -> PlanCurrentStage
            
            else -> null
        }
        
        /**
         * Parse a string key that may include parameters (e.g., "plan:stage1")
         */
        fun fromStringWithParams(key: String): MemoryKey? {
            // Try to match parameterized keys
            val stageMatch = Regex("""plan:stage(\d+)$""").find(key)
            if (stageMatch != null) {
                return PlanStageName(stageMatch.groupValues[1].toInt())
            }
            
            val stageDescMatch = Regex("""plan:stage(\d+)_desc$""").find(key)
            if (stageDescMatch != null) {
                return PlanStageDesc(stageDescMatch.groupValues[1].toInt())
            }
            
            val stageExpertMatch = Regex("""plan:stage(\d+)_expert$""").find(key)
            if (stageExpertMatch != null) {
                return PlanStageExpert(stageExpertMatch.groupValues[1].toInt())
            }
            
            val stageStatusMatch = Regex("""plan:stage(\d+)_status$""").find(key)
            if (stageStatusMatch != null) {
                return PlanStageStatus(stageStatusMatch.groupValues[1].toInt())
            }
            
            val stageArtifactMatch = Regex("""plan:stage(\d+)_artifact$""").find(key)
            if (stageArtifactMatch != null) {
                return PlanStageArtifact(stageArtifactMatch.groupValues[1].toInt())
            }
            
            val execResultMatch = Regex("""exec:stage(\d+)_result$""").find(key)
            if (execResultMatch != null) {
                return ExecStageResult(execResultMatch.groupValues[1].toInt())
            }
            
            val verifFeedbackMatch = Regex("""verif:stage(\d+)_feedback$""").find(key)
            if (verifFeedbackMatch != null) {
                return VerifStageFeedback(verifFeedbackMatch.groupValues[1].toInt())
            }
            
            val verifScoreMatch = Regex("""verif:stage(\d+)_score$""").find(key)
            if (verifScoreMatch != null) {
                return VerifStageScore(verifScoreMatch.groupValues[1].toInt())
            }
            
            // Fall back to simple keys
            return fromString(key)
        }
        
        /**
         * Convert MemoryKey to string.
         * For backward compatibility with legacy code.
         */
        fun toString(memoryKey: MemoryKey): String = memoryKey.key
        
        /**
         * Create MemoryKey from TaskMemoryKeys constant.
         * Provides migration path from old to new API.
         */
        fun fromTaskMemoryKey(constant: String): MemoryKey {
            return fromStringWithParams(constant) 
                ?: throw IllegalArgumentException("Unknown memory key: $constant")
        }
    }
}

/**
 * Extension function for string keys to get MemoryKey.
 * Provides convenient migration from legacy code.
 */
fun String.toMemoryKey(): MemoryKey? = MemoryKey.fromStringWithParams(this)

/**
 * Extension function for MemoryKey to get string key.
 */
fun MemoryKey.toKeyString(): String = this.key
