package com.example.day.core.core_features.agent.data.local.mapper

import com.example.day.core.core_features.agent.data.local.model.StrategyTypeEntity
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import javax.inject.Inject

class CtxStrategyTypeMapper @Inject constructor() {

    fun toDomain(value: StrategyTypeEntity): CtxStrategyType {
        return when (value) {
            StrategyTypeEntity.SLIDING_WINDOW -> CtxStrategyType.SLIDING_WINDOW
            StrategyTypeEntity.SUMMARIZATION -> CtxStrategyType.SUMMARIZATION
            StrategyTypeEntity.FULL_CONTEXT -> CtxStrategyType.FULL_CONTEXT
            StrategyTypeEntity.STICKY_FACTS -> CtxStrategyType.STICKY_FACTS
            StrategyTypeEntity.BRANCHING -> CtxStrategyType.BRANCHING
        }
    }

    fun toEntity(value: CtxStrategyType): StrategyTypeEntity {
        return when (value) {
            CtxStrategyType.SLIDING_WINDOW -> StrategyTypeEntity.SLIDING_WINDOW
            CtxStrategyType.SUMMARIZATION -> StrategyTypeEntity.SUMMARIZATION
            CtxStrategyType.FULL_CONTEXT -> StrategyTypeEntity.FULL_CONTEXT
            CtxStrategyType.STICKY_FACTS -> StrategyTypeEntity.STICKY_FACTS
            CtxStrategyType.BRANCHING -> StrategyTypeEntity.BRANCHING
        }
    }
}