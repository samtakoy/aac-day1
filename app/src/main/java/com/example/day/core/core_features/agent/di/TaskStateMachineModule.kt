package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.state_machine.domain.StateConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal interface TaskStateMachineModule {

    companion object {
        /**
         * Provides the default TaskStateConfig.
         * This is the standard workflow: INIT → PLANNING → EXECUTION → VERIFICATION → DONE
         * TODO пока только есть один конфиг и поэтому это работает
         */
        @Provides
        @Singleton
        internal fun provideTaskStateConfig(): StateConfig {
            return TaskStateConfig.config
        }
    }
}
