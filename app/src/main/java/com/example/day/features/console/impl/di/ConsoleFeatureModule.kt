package com.example.day.features.console.impl.di

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.llm.data.LlmRepositoryImpl
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApi
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApiImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.LlmRepository
import dagger.Module

/**
 * DI модуль для console фичи.
 *
 * Примечание: AgentTools, ChatTools и Workers теперь находятся в core слое
 * и должны быть привязаны в AgentCoreFeatureModule.
 */
@Module
internal abstract class ConsoleFeatureModule