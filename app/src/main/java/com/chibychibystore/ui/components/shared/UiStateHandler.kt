package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chibychibystore.ui.common.UiState

/**
 * Composable untuk handle UiState dengan consistent UI patterns
 * 
 * Automatically shows loading, error, atau success content
 * berdasarkan state yang diberikan.
 * 
 * @param state UiState yang akan di-handle
 * @param onRetry Callback untuk retry action ketika error
 * @param modifier Modifier untuk container
 * @param loadingContent Custom loading content (optional)
 * @param errorContent Custom error content (optional)
 * @param successContent Content yang ditampilkan ketika success
 */
@Composable
fun <T> UiStateHandler(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loadingContent: @Composable (() -> Unit)? = null,
    errorContent: @Composable ((String) -> Unit)? = null,
    successContent: @Composable (T) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is UiState.Idle -> {
                // Show nothing or placeholder
            }
            
            is UiState.Loading -> {
                if (loadingContent != null) {
                    loadingContent()
                } else {
                    LoadingIndicator()
                }
            }
            
            is UiState.Error -> {
                if (errorContent != null) {
                    errorContent(state.message)
                } else {
                    ErrorMessage(
                        message = state.message,
                        onRetry = onRetry
                    )
                }
            }
            
            is UiState.Success -> {
                successContent(state.data)
            }
        }
    }
}

/**
 * Simplified version untuk list data dengan empty state
 */
@Composable
fun <T> ListUiStateHandler(
    state: UiState<List<T>>,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Tidak ada data",
    successContent: @Composable (List<T>) -> Unit
) {
    UiStateHandler(
        state = state,
        onRetry = onRetry,
        modifier = modifier,
        successContent = { data ->
            if (data.isEmpty()) {
                EmptyState(message = emptyMessage)
            } else {
                successContent(data)
            }
        }
    )
}
