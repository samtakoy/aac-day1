package com.example.day.features.console.impl.di

import com.example.day.core.core_features.llm.data.LlmRepositoryImpl
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApi
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApiImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.LlmRepository
import dagger.Binds
import dagger.Module

@Module
internal interface ConsoleFeatureModule {

}