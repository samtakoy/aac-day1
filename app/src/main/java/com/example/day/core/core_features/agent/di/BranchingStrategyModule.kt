package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.domain.strategy.impl.ContextBranchingStrategy
import dagger.Module
import dagger.Provides

@Module
class BranchingStrategyModule {

    @Provides
    fun provideContextBranchingStrategy(): ContextBranchingStrategy {
        return ContextBranchingStrategy()
    }
}
