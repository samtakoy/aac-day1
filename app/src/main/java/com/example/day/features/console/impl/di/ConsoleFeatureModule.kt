package com.example.day.features.console.impl.di

import com.example.day.core.core_features.agent.domain.workers.tools.ContextFormatter
import com.example.day.core.core_features.agent.domain.workers.tools.ContextFormatterImpl
import dagger.Binds
import dagger.Module

/**
 * DI модуль для console фичи.
 *
 * Примечание: ChatTools уже привязан в ConsoleFeatureDeps.
 * Здесь добавляем только ContextFormatter.
 */
@Module
internal abstract class ConsoleFeatureModule {
    //@Binds
    //abstract fun bindContextFormatter(impl: ContextFormatterImpl): ContextFormatter
}
