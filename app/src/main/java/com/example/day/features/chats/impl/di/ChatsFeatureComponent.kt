package com.example.day.features.chats.impl.di

import androidx.compose.runtime.Immutable
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModelImpl
import dagger.Component

@Immutable
@ChatsFeatureScope
@Component(dependencies = [ChatsFeatureDeps::class], modules = [ChatsFeatureModule::class])
internal interface ChatsFeatureComponent {
    @Component.Factory
    interface Factory {
        fun create(
            deps: ChatsFeatureDeps
        ): ChatsFeatureComponent
    }

    fun getViewModelFactory(): ChatsViewModelImpl.Factory
}
