package com.example.day.features.console.impl.di

import com.example.day.features.console.impl.data.LlmRepositoryImpl
import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.data.remote.RemoteLlmApiImpl
import com.example.day.features.console.impl.data.remote.mappers.ModelRequestMapper
import com.example.day.features.console.impl.data.remote.mappers.ModelRequestMapperImpl
import com.example.day.features.console.impl.data.remote.mappers.ModelResponseMapper
import com.example.day.features.console.impl.data.remote.mappers.ModelResponseMapperImpl
import com.example.day.features.console.impl.domain.LlmRepository
import dagger.Binds
import dagger.Module

@Module
internal interface ConsoleFeatureModule {
    @Binds
    fun bindsRequestMapper(impl: ModelRequestMapperImpl): ModelRequestMapper
    @Binds
    fun bindsResponseMapper(impl: ModelResponseMapperImpl): ModelResponseMapper
    @Binds
    fun bindsApi(impl: RemoteLlmApiImpl): RemoteLlmApi
    @Binds
    fun bindsRepository(impl: LlmRepositoryImpl): LlmRepository
}