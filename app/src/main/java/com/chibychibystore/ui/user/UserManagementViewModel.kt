package com.chibychibystore.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import com.chibychibystore.service.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for User Management
 */
data class UserManagementUiState(
    val users: List<Pengguna> = emptyList(),
    val filteredUsers: List<Pengguna> = emptyList(),
    val selectedRole: Role? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val userStats: UserStats? = null,
    val showCreateUserDialog: Boolean = false,
    val showEditUserDialog: Boolean = false,
    val showDeleteUserDialog: Boolean = false,
    val showResetPasswordDialog: Boolean = false,
    val selectedUser: Pengguna? = null,
    val searchQuery: String = ""
)

data class CreateUserFormState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: Role = Role.CASHIER,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

data class EditUserFormState(
    val username: String = "",
    val role: Role = Role.CASHIER,
    val isActive: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

data class ResetPasswordFormState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userManagementService: UserManagementService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    private val _createUserFormState = MutableStateFlow(CreateUserFormState())
    private val _editUserFormState = MutableStateFlow(EditUserFormState())
    private val _resetPasswordFormState = MutableStateFlow(ResetPasswordFormState())

    private val _searchQuery = MutableStateFlow("")
    private val _selectedRole = MutableStateFlow<Role?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<UserManagementUiState> = run {
        val usersFlow = _searchQuery
            .debounce(300L)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    userManagementService.getAllUsers()
                } else {
                    userManagementService.searchUsers(query)
                }
            }
            .catch { emit(emptyList()) }

        combine(
            usersFlow,
            _searchQuery,
            _selectedRole,
            _uiState
        ) { users, query, role, currentState ->
            val finalFilteredUsers = if (role != null) {
                users.filter { it.role == role }
            } else {
                users
            }

            currentState.copy(
                users = users,
                filteredUsers = finalFilteredUsers,
                searchQuery = query,
                selectedRole = role,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserManagementUiState(isLoading = true)
        )
    }

    val createUserFormState: StateFlow<CreateUserFormState> = _createUserFormState
    val editUserFormState: StateFlow<EditUserFormState> = _editUserFormState
    val resetPasswordFormState: StateFlow<ResetPasswordFormState> = _resetPasswordFormState

    init {
        loadUserStats()
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            userManagementService.getUserStats().onSuccess { stats ->
                _uiState.update { it.copy(userStats = stats) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onRoleFilterChange(role: Role?) {
        _selectedRole.value = role
    }

    fun showCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = true) }
        _createUserFormState.value = CreateUserFormState()
    }

    fun hideCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = false) }
    }

    fun onCreateUserUsernameChange(username: String) {
        _createUserFormState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onCreateUserPasswordChange(password: String) {
        _createUserFormState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onCreateUserConfirmPasswordChange(confirmPassword: String) {
        _createUserFormState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun onCreateUserRoleChange(role: Role) {
        _createUserFormState.update { it.copy(role = role) }
    }

    fun createUser() {
        val formState = _createUserFormState.value
        if (formState.username.isBlank()) {
            _createUserFormState.update { it.copy(errorMessage = "Username tidak boleh kosong") }
            return
        }
        if (formState.password.isBlank()) {
            _createUserFormState.update { it.copy(errorMessage = "Kata sandi tidak boleh kosong") }
            return
        }
        if (formState.password != formState.confirmPassword) {
            _createUserFormState.update { it.copy(errorMessage = "Konfirmasi kata sandi tidak cocok") }
            return
        }

        _createUserFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            userManagementService.createUser(
                username = formState.username,
                password = formState.password,
                role = formState.role,
                createdBy = currentUser?.id ?: 0L
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Pengguna berhasil dibuat", showCreateUserDialog = false) }
                loadUserStats()
                clearMessagesAfterDelay()
            }.onFailure { e ->
                _createUserFormState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Gagal membuat pengguna") }
            }
        }
    }

    fun showEditUserDialog(user: Pengguna) {
        _uiState.update { it.copy(showEditUserDialog = true, selectedUser = user) }
        _editUserFormState.value = EditUserFormState(
            username = user.username,
            role = user.role,
            isActive = user.isActive
        )
    }

    fun hideEditUserDialog() {
        _uiState.update { it.copy(showEditUserDialog = false) }
    }

    fun onEditUserUsernameChange(username: String) {
        _editUserFormState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onEditUserRoleChange(role: Role) {
        _editUserFormState.update { it.copy(role = role) }
    }

    fun onEditUserIsActiveChange(isActive: Boolean) {
        _editUserFormState.update { it.copy(isActive = isActive) }
    }

    fun updateUser() {
        val formState = _editUserFormState.value
        val selectedUser = _uiState.value.selectedUser ?: return

        _editUserFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            userManagementService.updateUser(
                userId = selectedUser.id,
                username = formState.username,
                role = formState.role,
                isActive = formState.isActive,
                updatedBy = currentUser?.id ?: 0L
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Pengguna berhasil diperbarui", showEditUserDialog = false) }
                clearMessagesAfterDelay()
            }.onFailure { e ->
                _editUserFormState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Gagal memperbarui pengguna") }
            }
        }
    }

    fun showDeleteUserDialog(user: Pengguna) {
        _uiState.update { it.copy(showDeleteUserDialog = true, selectedUser = user) }
    }

    fun hideDeleteUserDialog() {
        _uiState.update { it.copy(showDeleteUserDialog = false) }
    }

    fun deleteUser() {
        val selectedUser = _uiState.value.selectedUser ?: return
        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            userManagementService.deleteUser(selectedUser.id, currentUser?.id ?: 0L).onSuccess {
                _uiState.update { it.copy(successMessage = "Pengguna berhasil dihapus", showDeleteUserDialog = false) }
                loadUserStats()
                clearMessagesAfterDelay()
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Gagal menghapus pengguna", showDeleteUserDialog = false) }
                clearMessagesAfterDelay()
            }
        }
    }

    fun showResetPasswordDialog(user: Pengguna) {
        _uiState.update { it.copy(showResetPasswordDialog = true, selectedUser = user) }
        _resetPasswordFormState.value = ResetPasswordFormState()
    }

    fun hideResetPasswordDialog() {
        _uiState.update { it.copy(showResetPasswordDialog = false) }
    }

    fun onResetPasswordChange(password: String) {
        _resetPasswordFormState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onResetPasswordConfirmChange(confirmPassword: String) {
        _resetPasswordFormState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun resetUserPassword() {
        val formState = _resetPasswordFormState.value
        val selectedUser = _uiState.value.selectedUser ?: return

        if (formState.newPassword != formState.confirmPassword) {
            _resetPasswordFormState.update { it.copy(errorMessage = "Konfirmasi kata sandi tidak cocok") }
            return
        }

        _resetPasswordFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            userManagementService.resetUserPassword(selectedUser.id, formState.newPassword, currentUser?.id ?: 0L).onSuccess {
                _uiState.update { it.copy(successMessage = "Kata sandi berhasil direset", showResetPasswordDialog = false) }
                clearMessagesAfterDelay()
            }.onFailure { e ->
                _resetPasswordFormState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Gagal mereset kata sandi") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun clearMessagesAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }
}
