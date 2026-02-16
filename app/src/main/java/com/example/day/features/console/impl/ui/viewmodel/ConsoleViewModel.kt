package com.example.day.features.console.impl.ui.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

@Immutable
internal interface ConsoleViewModel {
    fun getStateAsFlow(): StateFlow<State>
    fun onEvent(event: Event)

    data class State(
        val inputInitialValue: String,
        val response: String,
        val type: Type
    ) {
        @Immutable
        sealed interface Type {
            object Loading : Type
            object Data: Type
            object Error: Type
        }
    }

    sealed interface Event {
        class SubmitButtonClick(val inputText: String) : Event
        class InputChanged(val text: String) : Event
    }
}