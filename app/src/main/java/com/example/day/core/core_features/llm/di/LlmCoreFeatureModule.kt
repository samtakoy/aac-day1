package com.example.day.core.core_features.llm.di

import com.example.day.core.core_features.llm.data.LlmRepositoryImpl
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApi
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApiImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.LlmRepository
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.LlmRequestUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal interface LlmCoreFeatureModule {
    @Binds
    fun bindsRequestMapper(impl: ModelRequestMapperImpl): ModelRequestMapper
    @Binds
    fun bindsResponseMapper(impl: ModelResponseMapperImpl): ModelResponseMapper
    @Binds
    fun bindsApi(impl: RemoteLlmApiImpl): RemoteLlmApi
    @Binds
    fun bindsRepository(impl: LlmRepositoryImpl): LlmRepository
    @Binds
    fun bindsLlmRequestUseCase(impl: LlmRequestUseCaseImpl): LlmRequestUseCase
}