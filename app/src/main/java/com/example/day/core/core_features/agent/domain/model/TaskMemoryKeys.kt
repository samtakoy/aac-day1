package com.example.day.core.core_features.agent.domain.model

/**
 * LTM memory keys for task state machine.
 * All keys follow the pattern: category:key_name
 */
object TaskMemoryKeys {
    // Workflow state
    const val CURRENT_STATE = "workflow:current_state"
    const val CURRENT_STEP = "workflow:current_step"

    // Init artifacts
    const val INIT_TITLE = "init:title"
    const val INIT_DESCRIPTION = "init:description"
    const val INIT_GOAL = "init:goal"
    const val INIT_EXPERT = "init:expert"

    // Planning artifacts
    const val PLAN_TOTAL_STAGES = "plan:total_stages"
    const val PLAN_CURRENT_STAGE = "plan:current_stage"

    // Stage artifacts - format: plan:stageN, plan:stageN_desc, plan:stageN_expert, plan:stageN_status
    fun planStageName(n: Int) = "plan:stage$n"
    fun planStageDesc(n: Int) = "plan:stage${n}_desc"
    fun planStageExpert(n: Int) = "plan:stage${n}_expert"
    fun planStageStatus(n: Int) = "plan:stage${n}_status"

    // Execution artifacts - format: plan:stageN_artifact, exec:stageN_result
    fun planStageArtifact(n: Int) = "plan:stage${n}_artifact"
    fun execStageResult(n: Int) = "exec:stage${n}_result"

    // Verification artifacts - format: verif:stageN_feedback, verif:stageN_score
    fun verifStageFeedback(n: Int) = "verif:stage${n}_feedback"
    fun verifStageScore(n: Int) = "verif:stage${n}_score"

    // Categories for LTM
    const val CAT_WORKFLOW = "workflow"
    const val CAT_INIT = "init"
    const val CAT_PLAN = "plan"
    const val CAT_EXEC = "exec"
    const val CAT_VERIF = "verif"
}
