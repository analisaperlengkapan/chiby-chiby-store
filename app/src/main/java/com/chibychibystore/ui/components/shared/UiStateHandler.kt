package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chibychibystore.ui.common.UiState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info

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
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.Text(
                                    text = state.message,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (onRetry != null) {
                                androidx.compose.foundation.layout.Row {
                                    androidx.compose.material3.TextButton(onClick = onRetry) {
                                        androidx.compose.material3.Text("Coba Lagi")
                                    }
                                }
                            }
                        }
                    }
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
                EmptyState(
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    title = "Tidak ada data",
                    message = emptyMessage
                )
            } else {
                successContent(data)
            }
        }
    )
}
