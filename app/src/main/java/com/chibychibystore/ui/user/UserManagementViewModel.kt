package com.chibychibystore.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val users: List<Pengguna> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userManagementService: UserManagementService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    suspend fun createUser(username: String, password: String, role: Role): Result<Long> {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val currentUser = authService.getCurrentUser()
            val createdBy = currentUser?.id ?: 0L // Fallback to 0 if no user found, though practically should be logged in

            val result = userManagementService.createUser(
                username = username,
                password = password,
                role = role,
                createdBy = createdBy
            )

            if (result.isSuccess) {
                // Refresh list
                loadUsers()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Gagal membuat pengguna"
                )
            }
            return result
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Terjadi kesalahan saat membuat pengguna"
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage
            )
            return Result.failure(e)
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                userManagementService.getAllUsers().collect { users ->
                    _uiState.value = _uiState.value.copy(
                        users = users,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat pengguna"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}