package com.chibychibystore.ui.components.shared

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manager untuk menampilkan Snackbar messages
 * 
 * Provides centralized way untuk show success, error, dan info messages
 * dengan consistent styling dan behavior.
 */
class SnackbarManager(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    
    /**
     * Show success message
     */
    fun showSuccess(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        show(
            message = "✓ $message",
            actionLabel = actionLabel,
            onAction = onAction,
            duration = SnackbarDuration.Short
        )
    }
    
    /**
     * Show error message
     */
    fun showError(
        message: String,
        actionLabel: String? = "Coba Lagi",
        onAction: (() -> Unit)? = null
    ) {
        show(
            message = "✗ $message",
            actionLabel = actionLabel,
            onAction = onAction,
            duration = SnackbarDuration.Long
        )
    }
    
    /**
     * Show info message
     */
    fun showInfo(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        show(
            message = "ℹ $message",
            actionLabel = actionLabel,
            onAction = onAction,
            duration = SnackbarDuration.Short
        )
    }
    
    /**
     * Show warning message
     */
    fun showWarning(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        show(
            message = "⚠ $message",
            actionLabel = actionLabel,
            onAction = onAction,
            duration = SnackbarDuration.Long
        )
    }
    
    /**
     * Generic show method
     */
    private fun show(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
            
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }
    
    /**
     * Dismiss current snackbar
     */
    fun dismiss() {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

/**
 * Extension function untuk SnackbarHostState
 */
fun SnackbarHostState.asManager(scope: CoroutineScope) = SnackbarManager(this, scope)
