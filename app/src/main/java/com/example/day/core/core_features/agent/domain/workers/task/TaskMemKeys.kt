package com.example.day.core.core_features.agent.domain.workers.task

/**
 * LTM memory keys for task state machine.
 */
object TaskMemKeys {
    const val USER_APPROVE = "user_approve"
    const val NEXT_STATE = "next_state"
    const val MEM_UPDATES = "memory_updates"
    const val REPLY_TO_USER = "reply_to_user"
    const val TRUE = "true"
    const val PLANNING = "planning"
    const val EXECUTION = "execution"
    const val VERIFICATION = "verification"

    // Init artifacts
    const val INIT_TITLE = "title"
    const val INIT_DESC = "description"
    const val INIT_GOAL = "goal"
    const val INIT_EXPERT = "expert"

    // Planning artifacts
    const val PLAN_TOTAL_STAGES = "total_stages"
    const val PLAN_CURRENT_STAGE = "current_stage"
    const val PLAN_TASK_DETAILS = "task_details"

    const val EXECUTION_RESULT = "result"

    const val VERIF_RESUME = "resume"
    const val VERIF_SCORE = "score"

    fun planStageName(n: Int) = "stage_$n"
    fun planStageNameN() = "stage_N"
    fun planStageDesc(n: Int) = "stage_${n}_desc"
    fun planStageDescN() = "stage_N_desc"
    fun planStageExpert(n: Int) = "stage_${n}_expert"
    fun planStageExpertN() = "stage_N_expert"
    fun planStageStatus(n: Int) = "stage_${n}_status"

    fun execStageResult(n: Int) = "stage_${n}_result"

    fun verifStageFeedback(n: Int) = "stage_${n}_feedback"
    fun verifStageFeedbackN() = "stage_N_feedback"
    fun verifStageScore(n: Int) = "stage_${n}_score"
    fun verifStageScoreN() = "stage_N_score"
}
