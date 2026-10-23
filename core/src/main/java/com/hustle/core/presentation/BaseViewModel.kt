package com.hustle.core.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<F>(capacity = Channel.BUFFERED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    protected fun updateState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: F) {
        _effects.trySend(effect)
    }

    abstract fun onEvent(event: E)
}
