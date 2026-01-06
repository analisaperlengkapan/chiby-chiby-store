package com.chibychibystore.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base ViewModel to handle common UI logic like loading, error, and one-time events.
 *
 * @param S The type of the UI State, must implement [UiState].
 * @param initialState The initial state of the UI.
 */
abstract class BaseViewModel<S : UiState>(initialState: S) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _errorEvent = Channel<String>()
    val errorEvent = _errorEvent.receiveAsFlow()

    private val _successEvent = Channel<String>()
    val successEvent = _successEvent.receiveAsFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /**
     * Updates the UI state atomically.
     */
    protected fun updateState(update: (S) -> S) {
        _uiState.update(update)
    }

    /**
     * Get current state value
     */
    protected val currentState: S
        get() = _uiState.value

    /**
     * Show loading indicator
     */
    protected fun showLoading() {
        _loading.value = true
    }

    /**
     * Hide loading indicator
     */
    protected fun hideLoading() {
        _loading.value = false
    }

    /**
     * Send an error message to the UI (one-time event)
     */
    protected fun sendError(message: String) {
        viewModelScope.launch {
            _errorEvent.send(message)
        }
    }

    /**
     * Send a success message to the UI (one-time event)
     */
    protected fun sendSuccess(message: String) {
        viewModelScope.launch {
            _successEvent.send(message)
        }
    }

    /**
     * Launch a coroutine with automatic loading and error handling.
     *
     * @param block The suspending block to execute.
     * @param onError Optional error handler. If null, the error message is sent to [errorEvent].
     */
    protected fun launchWithState(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            showLoading()
            try {
                block()
            } catch (e: Exception) {
                if (onError != null) {
                    onError(e)
                } else {
                    sendError(e.message ?: "Terjadi kesalahan yang tidak diketahui")
                }
            } finally {
                hideLoading()
            }
        }
    }
}
