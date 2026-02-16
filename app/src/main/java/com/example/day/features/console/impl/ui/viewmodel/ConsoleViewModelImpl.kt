package com.example.day.features.console.impl.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.day.features.console.impl.domain.LlmRequestUseCase
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsoleViewModelImpl(
    private val requestUseCase: LlmRequestUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ConsoleViewModel {

    private val _state = MutableStateFlow(
        State(
            inputInitialValue = "",
            response = "",
            type = State.Type.Data
        )
    )

    override fun getStateAsFlow(): StateFlow<State> = _state

    override fun onEvent(event: ConsoleViewModel.Event) {
        when (event) {
            is ConsoleViewModel.Event.InputChanged -> {
                _state.update { it.copy(inputInitialValue = event.text) }
            }
            is ConsoleViewModel.Event.SubmitButtonClick -> {
                sendRequest(event.inputText)
            }
        }
    }

    private fun sendRequest(inputText: String) {
        _state.update { it.copy(response = "waiting...", type = State.Type.Loading) }
        launchCatching(
            onError = { error ->
                _state.update { it.copy(response = error.stackTraceToString(), type = State.Type.Error) }
            }
        ) {
            requestUseCase.exec(inputText)
                .onSuccess { result ->
                    _state.update { it.copy(response = result, type = State.Type.Data) }
                }
                .onFailure { result ->
                    _state.update { it.copy(response = result.stackTraceToString(), type = State.Type.Error) }
                }
                .getOrNull()
        }
    }

    private fun launchCatching(
        onError: (suspend (Throwable) -> Unit)? = null,
        onFinally: (suspend () -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError?.invoke(e)
            } finally {
                onFinally?.invoke()
            }
        }
    }

    class Factory @Inject constructor(
        private val requestUseCase: LlmRequestUseCase
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ConsoleViewModelImpl(requestUseCase, savedStateHandle) as T
        }
    }
}