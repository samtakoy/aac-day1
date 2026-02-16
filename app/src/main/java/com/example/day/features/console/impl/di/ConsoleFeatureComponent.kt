package com.example.day.features.console.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.console.impl.data.LlmRepositoryImpl
import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.data.remote.RemoteLlmApiImpl
import com.example.day.features.console.impl.domain.LlmRepository
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModelImpl
import dagger.Binds
import dagger.Component

@Immutable
@ConsoleFeatureScope
@Component(dependencies = [ConsoleFeatureDeps::class], modules = [ConsoleFeatureModule::class])
internal interface ConsoleFeatureComponent {
    @Component.Factory
    interface Factory {
        fun create(deps: ConsoleFeatureDeps): ConsoleFeatureComponent
    }

    fun getViewModelFactory(): ConsoleViewModelImpl.Factory
}