package com.chibychibystore.data.model

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
    
    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun exceptionOrNull(): Exception? =
        when (this) {
            is Failure -> exception
            else -> null
        }

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            else -> null
        }

    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (Exception) -> Unit): Result<T> {
        if (this is Failure) action(exception)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun failure(exception: Exception): Result<Nothing> = Failure(exception)
    }
}
