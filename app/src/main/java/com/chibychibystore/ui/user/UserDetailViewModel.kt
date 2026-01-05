package com.chibychibystore.ui.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pengguna
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
    val user: Pengguna? = null,
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

    private val userId: Long = try {
        val id = savedStateHandle.get<Long>("userId") ?: 0L
        id
    } catch (e: Exception) {
        0L
    }

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private var originalUser: Pengguna? = null

    init {
        if (userId > 0) {
            loadUser(userId)
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = userManagementService.getUserById(userId)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    originalUser = user
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoading = false
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Gagal memuat pengguna"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat pengguna"
                    )
                }
            }
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun updateUsername(username: String) {
        _uiState.value.user?.let { user ->
            _uiState.update {
                it.copy(
                    user = user.copy(username = username)
                )
            }
        }
    }

    fun updateRole(role: Role) {
        _uiState.value.user?.let { user ->
            _uiState.update {
                it.copy(
                    user = user.copy(role = role)
                )
            }
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
                    isActive = null,
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
                    val errorMessage = result.exceptionOrNull()?.message ?: "Gagal menyimpan pengguna"
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
        _uiState.update {
            it.copy(
                user = originalUser,
                isEditMode = false
            )
        }
    }

    fun deleteUser(onSuccess: () -> Unit) {
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

                val result = userManagementService.deleteUser(
                    userId = userId,
                    deletedBy = currentUser.id
                )

                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Gagal menghapus pengguna"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Terjadi kesalahan saat menghapus"
                    )
                }
            }
        }
    }
}
