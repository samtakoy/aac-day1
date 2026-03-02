package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType

sealed class SetupResult {
    data class Updated(val strategy: CtxStrategyType, val params: AContextParams) : SetupResult()
    data class Migrated(
        val from: CtxStrategyType,
        val to: CtxStrategyType,
        val messagesMigrated: Int,
        val totalMessages: Int
    ) : SetupResult()
}
