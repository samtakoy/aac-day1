package com.example.day.core.core_features.agent.domain.workers.branch

sealed class BranchResult {
    data class Success(val message: String) : BranchResult()
    data class Error(val message: String) : BranchResult()
}
