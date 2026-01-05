package com.chibychibystore.ui.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.User
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserDetailUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userManagementService: UserManagementService,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private var originalUser: User? = null

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = userManagementService.getUserById(userId)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    originalUser = user
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Gagal memuat user"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat user"
                )
            }
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = !_uiState.value.isEditMode)
    }

    fun updateUsername(username: String) {
        _uiState.value.user?.let { user ->
            _uiState.value = _uiState.value.copy(
                user = user.copy(username = username)
            )
        }
    }

    fun updateRole(role: Role) {
        _uiState.value.user?.let { user ->
            _uiState.value = _uiState.value.copy(
                user = user.copy(role = role)
            )
        }
    }

    fun saveUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Sesi telah berakhir. Silakan login kembali."
                        )
                    }
                    return@launch
                }

                val userToSave = _uiState.value.user ?: return@launch

                val result = userManagementService.updateUser(
                    userId = userToSave.id,
                    username = userToSave.username,
                    role = userToSave.role,
                    isActive = null, // Not updating active status here
                    updatedBy = currentUser.id
                )

                if (result.isSuccess) {
                    originalUser = userToSave
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = false
                        )
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Gagal menyimpan user"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Terjadi kesalahan saat menyimpan"
                    )
                }
            }
        }
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            user = originalUser,
            isEditMode = false
        )
    }

    fun deleteUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Sesi telah berakhir. Silakan login kembali."
                    )
                    return@launch
                }

                val result = userManagementService.deleteUser(
                    userId = userId,
                    deletedBy = currentUser.id
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal menghapus user"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat menghapus"
                )
            }
        }
    }
}
