package com.chibychibystore.ui.common

/**
 * Generic UI State untuk consistent state management across ViewModels
 * 
 * Menggunakan sealed class untuk type-safe state handling dengan
 * support untuk loading, success, dan error states.
 * 
 * @param T Type dari data yang akan di-hold oleh state
 */
sealed class UiState<out T> {
    
    /**
     * Initial state sebelum data loading dimulai
     */
    object Idle : UiState<Nothing>()
    
    /**
     * Loading state ketika data sedang di-fetch
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Success state dengan data
     * 
     * @param data Data yang berhasil di-fetch
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state dengan error message
     * 
     * @param message Error message dalam Bahasa Indonesia
     * @param throwable Optional throwable untuk debugging
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Helper untuk check apakah state adalah loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Helper untuk check apakah state adalah success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Helper untuk check apakah state adalah error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Helper untuk get data jika success, null otherwise
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Helper untuk get error message jika error, null otherwise
     */
    fun getErrorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
}

/**
 * Extension function untuk map UiState data
 */
fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Idle -> UiState.Idle
        is UiState.Loading -> UiState.Loading
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> UiState.Error(message, throwable)
    }
}

/**
 * Extension function untuk handle UiState dengan callbacks
 */
inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

inline fun <T> UiState<T>.onError(action: (String, Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) action(message, throwable)
    return this
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}
