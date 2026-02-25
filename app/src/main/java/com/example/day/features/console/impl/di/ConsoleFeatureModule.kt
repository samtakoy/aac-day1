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
import com.example.day.features.console.impl.domain.agents.WorkerTools
import com.example.day.features.console.impl.domain.agents.WorkerToolsImpl
import dagger.Binds
import dagger.Module

@Module
internal abstract class ConsoleFeatureModule {
    // TODO посмотреть на предмет - правильности модуля и расположения
    @Binds
    abstract fun bindWorkerTools(impl: WorkerToolsImpl): WorkerTools
}