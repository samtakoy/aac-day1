package com.example.day.features.console.impl.di

import com.example.day.features.console.impl.data.LlmRepositoryImpl
import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.data.remote.RemoteLlmApiImpl
import com.example.day.features.console.impl.domain.LlmRepository
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import dagger.Binds
import dagger.Module

@Module
internal interface ConsoleFeatureModule {
    @Binds
    fun bindsApi(impl: RemoteLlmApiImpl): RemoteLlmApi
    @Binds
    fun bindsRepository(impl: LlmRepositoryImpl): LlmRepository
}