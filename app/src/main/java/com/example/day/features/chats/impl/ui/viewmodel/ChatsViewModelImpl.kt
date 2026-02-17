package com.example.day.features.chats.impl.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatListAsFlowUseCase
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModel.State
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ChatsViewModelImpl(
    private val createChatUseCase: CreateChatUseCase,
    private val getChatsUseCase: GetChatListAsFlowUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ChatsViewModel {

    private val _state = MutableStateFlow(
        State(
            (emptyList<State.Chat>()).toPersistentList()
        )
    )

    init {
        getChatsUseCase()
            .onEach { chatModels ->
                _state.update { state ->
                    val selectedIndex = state.chips.indexOfFirst { it.isSelected }
                    state.copy(
                        chips = chatModels.mapIndexed { idx, model ->
                            State.Chat(
                                model.id,
                                title = model.title,
                                isSelected = selectedIndex == idx
                            )
                        }.toPersistentList()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun getStateAsFlow(): StateFlow<State> = _state

    override fun onEvent(event: ChatsViewModel.Event) {
        when (event) {
            ChatsViewModel.Event.AddChatClick -> onChatAddClick()
            is ChatsViewModel.Event.ChatClick -> onChatChipsClick(event.id)
            is ChatsViewModel.Event.SwipeTo -> onChatChipsClick(event.id)
        }
    }

    private fun onChatChipsClick(id: Long) {
        _state.update { currentState ->
            currentState.copy(
                chips = currentState.chips.mutate { list ->
                    // Сбрасываем старый выбор и устанавливаем новый за один проход
                    for (i in list.indices) {
                        val chip = list[i]
                        val shouldBeSelected = chip.id == id

                        if (chip.isSelected != shouldBeSelected) {
                            list[i] = chip.copy(isSelected = shouldBeSelected)
                        }
                    }
                }
            )
        }
    }

    private fun onChatAddClick() {
        launchCatching {
            createChatUseCase("Chat${(Math.random()*100).toInt()}")
        }
    }

    /** TODO дублирование */
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
        private val createChatUseCase: CreateChatUseCase,
        private val getChatsUseCase: GetChatListAsFlowUseCase
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ChatsViewModelImpl(
                createChatUseCase,
                getChatsUseCase,
                savedStateHandle
            ) as T
        }
    }


}