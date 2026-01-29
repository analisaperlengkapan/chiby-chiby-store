package com.chibychibystore.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import com.chibychibystore.service.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the complete UI state for the User Management screen in the Chiby Chiby Store POS app.
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

/**
 * Represents the form state for creating a new user.
 */
data class CreateUserFormState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: Role = Role.CASHIER,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents the form state for editing an existing user.
 */
data class EditUserFormState(
    val username: String = "",
    val role: Role = Role.CASHIER,
    val isActive: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents the form state for resetting a user's password.
 */
data class ResetPasswordFormState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel responsible for user management operations.
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userManagementService: UserManagementService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())

    private val _createUserFormState = MutableStateFlow(CreateUserFormState())
    val createUserFormState: StateFlow<CreateUserFormState> = _createUserFormState

    private val _editUserFormState = MutableStateFlow(EditUserFormState())
    val editUserFormState: StateFlow<EditUserFormState> = _editUserFormState

    private val _resetPasswordFormState = MutableStateFlow(ResetPasswordFormState())
    val resetPasswordFormState: StateFlow<ResetPasswordFormState> = _resetPasswordFormState

    private val _searchQuery = MutableStateFlow("")
    private val _selectedRole = MutableStateFlow<Role?>(null)

    init {
        loadUserStats()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<UserManagementUiState> = combine(
        _searchQuery.debounce(300L).flatMapLatest { query ->
            if (query.isBlank()) {
                userManagementService.getAllUsers()
            } else {
                userManagementService.searchUsers(query)
            }
        }.catch { emit(emptyList()) },
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

    private fun loadUserStats() {
        viewModelScope.launch {
            userManagementService.getUserStats().onSuccess { stats ->
                _uiState.update { it.copy(userStats = stats) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onRoleFilterChange(role: Role?) {
        _selectedRole.update { role }
    }

    fun showCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = true) }
        _createUserFormState.update { CreateUserFormState() }
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
            _createUserFormState.update { it.copy(errorMessage = "Password tidak boleh kosong") }
            return
        }
        if (formState.password != formState.confirmPassword) {
            _createUserFormState.update { it.copy(errorMessage = "Password konfirmasi tidak cocok") }
            return
        }

        _createUserFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUserId = authService.getCurrentUser()?.id ?: 0L // Fallback if necessary
            userManagementService.createUser(
                username = formState.username,
                password = formState.password,
                role = formState.role,
                createdBy = currentUserId
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "User berhasil dibuat", showCreateUserDialog = false) }
                loadUserStats()
            }.onFailure { error ->
                _createUserFormState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Gagal membuat user") }
            }
        }
    }

    fun showEditUserDialog(user: Pengguna) {
        _uiState.update { it.copy(showEditUserDialog = true, selectedUser = user) }
        _editUserFormState.update {
            EditUserFormState(
                username = user.username,
                role = user.role,
                isActive = user.isActive
            )
        }
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
        if (formState.username.isBlank()) {
            _editUserFormState.update { it.copy(errorMessage = "Username tidak boleh kosong") }
            return
        }

        _editUserFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUserId = authService.getCurrentUser()?.id ?: 0L
            userManagementService.updateUser(
                userId = selectedUser.id,
                username = formState.username,
                role = formState.role,
                isActive = formState.isActive,
                updatedBy = currentUserId
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "User berhasil diperbarui", showEditUserDialog = false) }
            }.onFailure { error ->
                _editUserFormState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Gagal memperbarui user") }
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
            val currentUserId = authService.getCurrentUser()?.id ?: 0L
            userManagementService.deleteUser(selectedUser.id, currentUserId).onSuccess {
                _uiState.update { it.copy(successMessage = "User berhasil dihapus", showDeleteUserDialog = false) }
                loadUserStats()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Gagal menghapus user", showDeleteUserDialog = false) }
            }
        }
    }

    fun showResetPasswordDialog(user: Pengguna) {
        _uiState.update { it.copy(showResetPasswordDialog = true, selectedUser = user) }
        _resetPasswordFormState.update { ResetPasswordFormState() }
    }

    fun hideResetPasswordDialog() {
        _uiState.update { it.copy(showResetPasswordDialog = false) }
    }

    fun onResetPasswordNewPasswordChange(password: String) {
        _resetPasswordFormState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onResetPasswordConfirmPasswordChange(password: String) {
        _resetPasswordFormState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun resetPassword() {
        val formState = _resetPasswordFormState.value
        val selectedUser = _uiState.value.selectedUser ?: return
        if (formState.newPassword.isBlank()) {
            _resetPasswordFormState.update { it.copy(errorMessage = "Password baru tidak boleh kosong") }
            return
        }
        if (formState.newPassword != formState.confirmPassword) {
            _resetPasswordFormState.update { it.copy(errorMessage = "Password konfirmasi tidak cocok") }
            return
        }

        _resetPasswordFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val currentUserId = authService.getCurrentUser()?.id ?: 0L
            userManagementService.resetUserPassword(selectedUser.id, formState.newPassword, currentUserId).onSuccess {
                _uiState.update { it.copy(successMessage = "Password berhasil direset", showResetPasswordDialog = false) }
            }.onFailure { error ->
                _resetPasswordFormState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Gagal mereset password") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
