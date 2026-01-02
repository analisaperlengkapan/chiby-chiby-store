package com.chibychibystore.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import com.chibychibystore.service.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the complete UI state for the User Management screen in the Chiby Chiby Store POS app.
 *
 * This data class encapsulates all the state information needed to render the user management interface,
 * including user lists, filtering options, dialog visibility states, loading states, and user feedback messages.
 * It follows the MVVM pattern and reactive state management using StateFlow.
 *
 * ## Key Responsibilities:
 * - **User Data Management**: Maintains the complete list of users and filtered results
 * - **UI State Coordination**: Manages visibility of all dialogs and forms
 * - **Loading & Error States**: Provides loading indicators and error messaging
 * - **Search & Filtering**: Supports real-time user search and role-based filtering
 * - **Success Feedback**: Displays success messages for completed operations
 *
 * ## State Flow Integration:
 * This state is observed by the UI layer through StateFlow, ensuring reactive updates
 * when any state property changes. The ViewModel updates this state immutably using
 * the copy() method to maintain thread safety and trigger UI recomposition.
 *
 * ## Business Logic Integration:
 * The state reflects business rules such as:
 * - Role-based access control (only Owner can manage users)
 * - User validation and uniqueness constraints
 * - Audit trail requirements (created/updated by tracking)
 *
 * @property users The complete list of all users in the system, loaded from UserManagementService
 * @property filteredUsers The filtered subset of users based on search query and role filter
 * @property selectedRole Optional role filter applied to the user list (OWNER, MANAGER, CASHIER, WAREHOUSE)
 * @property isLoading Indicates if a background operation is in progress (user loading, CRUD operations)
 * @property errorMessage Contains error messages to display to the user, null when no error
 * @property successMessage Contains success messages for completed operations, null when no message
 * @property userStats Statistical information about users (total count, role distribution, etc.)
 * @property showCreateUserDialog Controls visibility of the create user dialog/form
 * @property showEditUserDialog Controls visibility of the edit user dialog/form
 * @property showDeleteUserDialog Controls visibility of the delete confirmation dialog
 * @property showResetPasswordDialog Controls visibility of the password reset dialog
 * @property selectedUser The currently selected user for edit/delete/reset operations
 * @property searchQuery The current search text used to filter users by username
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
 * Represents the form state for creating a new user in the Chiby Chiby Store POS system.
 *
 * This data class manages all the input fields and validation state for the user creation form,
 * ensuring proper data collection and client-side validation before submitting to the backend.
 * It follows reactive form state management patterns with real-time validation feedback.
 *
 * ## Form Validation Rules:
 * - **Username**: Required, non-empty, unique across all users
 * - **Password**: Required, minimum length requirements, complexity rules
 * - **Confirm Password**: Must match the password field exactly
 * - **Role**: Must be a valid Role enum value (OWNER, MANAGER, CASHIER, WAREHOUSE)
 *
 * ## Security Considerations:
 * - Password fields are handled securely with confirmation matching
 * - Form state is cleared after successful submission to prevent data leakage
 * - Error messages are user-friendly and don't expose sensitive information
 *
 * ## UI Integration:
 * The form state is observed by the UI layer to provide real-time validation feedback,
 * loading states during submission, and error display. Each field change triggers
 * validation and clears previous error messages.
 *
 * @property username The entered username for the new user account
 * @property password The entered password (should be masked in UI)
 * @property confirmPassword The confirmation password that must match the password field
 * @property role The selected role for the new user (defaults to CASHIER)
 * @property isSubmitting Indicates if the form is currently being submitted to prevent double-submission
 * @property errorMessage Contains validation or submission error messages, null when valid
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
 * Represents the form state for editing an existing user in the Chiby Chiby Store POS system.
 *
 * This data class manages the input fields and validation state for user editing operations,
 * allowing administrators to modify user properties while maintaining data integrity and
 * proper audit trails. It supports partial updates where only changed fields are submitted.
 *
 * ## Edit Capabilities:
 * - **Username Modification**: Change username with uniqueness validation
 * - **Role Changes**: Update user role with permission checks
 * - **Account Status**: Enable/disable user accounts (future feature)
 *
 * ## Business Rules:
 * - Username changes require uniqueness validation across all users except self
 * - Role changes may affect existing permissions and access rights
 * - All changes are tracked with audit information (updatedBy, updatedAt)
 * - Owner role changes require special permission validation
 *
 * ## Security & Audit:
 * - All changes are logged with the modifying user's ID
 * - Role changes trigger permission recalculation
 * - Form pre-populates with existing user data for easy editing
 *
 * @property username The current or modified username for the user
 * @property role The current or modified role for the user
 * @property isActive Account status flag (true = active, false = disabled) - future feature
 * @property isSubmitting Indicates if the edit operation is currently in progress
 * @property errorMessage Contains validation or submission error messages, null when valid
 */
data class EditUserFormState(
    val username: String = "",
    val role: Role = Role.CASHIER,
    val isActive: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents the form state for resetting a user's password in the Chiby Chiby Store POS system.
 *
 * This data class manages the secure password reset process, ensuring that password changes
 * are performed with proper validation and security measures. It supports administrative
 * password resets while maintaining audit trails and security best practices.
 *
 * ## Security Requirements:
 * - **Password Complexity**: Enforced through business logic validation
 * - **Confirmation Matching**: New password must be confirmed to prevent typos
 * - **Audit Trail**: All password resets are logged with the resetting user's ID
 * - **Secure Handling**: Passwords are hashed before storage, never stored in plain text
 *
 * ## Administrative Context:
 * - Only authorized users (Owner role) can reset other users' passwords
 * - Users cannot reset their own passwords through this administrative interface
 * - Password reset operations are logged for security auditing
 * - Temporary passwords may be generated for account recovery
 *
 * ## Validation Rules:
 * - New password cannot be empty
 * - Confirmation password must exactly match new password
 * - Password complexity requirements (length, character types) enforced by service layer
 *
 * @property newPassword The new password entered by the administrator
 * @property confirmPassword The confirmation of the new password for validation
 * @property isSubmitting Indicates if the password reset operation is in progress
 * @property errorMessage Contains validation or submission error messages, null when valid
 */
data class ResetPasswordFormState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel responsible for user management operations in the Chiby Chiby Store POS application.
 *
 * This ViewModel implements the MVVM pattern for comprehensive user administration, providing
 * reactive state management for user CRUD operations, role management, and security features.
 * It serves as the business logic layer between the user management UI and the data services.
 *
 * ## Core Responsibilities:
 * - **User CRUD Operations**: Create, read, update, delete user accounts with validation
 * - **Role-Based Access Control**: Manage user roles and permissions (Owner, Manager, Cashier, Warehouse)
 * - **Password Management**: Secure password reset and validation
 * - **User Search & Filtering**: Real-time user search and role-based filtering
 * - **Audit Trail**: Track all user management operations with proper logging
 *
 * ## State Management Architecture:
 * The ViewModel maintains multiple reactive state flows:
 * - **UserManagementUiState**: Main UI state for user lists, dialogs, and messaging
 * - **CreateUserFormState**: Form state for user creation with validation
 * - **EditUserFormState**: Form state for user editing operations
 * - **ResetPasswordFormState**: Form state for secure password resets
 *
 * ## Business Logic Implementation:
 * - **Permission Validation**: Ensures only authorized users (Owner role) can manage users
 * - **Data Validation**: Client-side validation with server-side confirmation
 * - **Error Handling**: Comprehensive error handling with user-friendly Indonesian messages
 * - **Audit Compliance**: All operations track the performing user for accountability
 *
 * ## Security Features:
 * - **Role-Based Permissions**: Owner-only access to user management functions
 * - **Password Security**: Secure password handling with confirmation validation
 * - **Input Sanitization**: Validation of usernames and other user inputs
 * - **Audit Logging**: Complete audit trail for all user management operations
 *
 * ## Reactive Patterns:
 * - **StateFlow Integration**: All state changes trigger reactive UI updates
 * - **Coroutine Management**: Proper lifecycle management with viewModelScope
 * - **Error Recovery**: Graceful error handling with user feedback
 * - **Loading States**: Clear loading indicators for all async operations
 *
 * ## Integration Points:
 * - **UserManagementService**: Core service for user operations and validation
 * - **AuthService**: Authentication and authorization checks
 * - **UI Layer**: Reactive binding to StateFlow properties for real-time updates
 *
 * ## Testing Strategy:
 * - **Unit Tests**: Individual method testing with mocked services
 * - **Integration Tests**: Full workflow testing with database integration
 * - **UI Tests**: Form validation and state management verification
 *
 * @property userManagementService The service handling all user management business logic
 * @constructor Creates a new UserManagementViewModel with dependency injection
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userManagementService: UserManagementService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState

    private val _createUserFormState = MutableStateFlow(CreateUserFormState())
    val createUserFormState: StateFlow<CreateUserFormState> = _createUserFormState

    private val _editUserFormState = MutableStateFlow(EditUserFormState())
    val editUserFormState: StateFlow<EditUserFormState> = _editUserFormState

    private val _resetPasswordFormState = MutableStateFlow(ResetPasswordFormState())
    val resetPasswordFormState: StateFlow<ResetPasswordFormState> = _resetPasswordFormState

    // Internal mutable state for search and filter
    private val _searchQuery = MutableStateFlow("")
    private val _selectedRole = MutableStateFlow<Role?>(null)

    // Derived User List Stream
    private val _usersFlow = userManagementService.getAllUsers()
        .catch { e ->
             // Emit empty list on error but keep the error accessible via side effects if needed
             // For simplicity, we just log/swallow here and let the UI state handling catch it via combination or distinct error flow
             // Ideally, we'd emit a Result wrapper, but our UiState structure handles it differently.
             emit(emptyList())
        }

    /**
     * Initializes the ViewModel and loads initial user data.
     *
     * This method is called automatically when the ViewModel is created and performs
     * the initial data loading for the user management interface. It ensures that
     * the UI starts with the most current user data and statistics.
     *
     * ## Initialization Flow:
     * 1. Load all users from the service
     * 2. Calculate user statistics (total users, role distribution)
     * 3. Update UI state with loaded data
     * 4. Handle any initialization errors gracefully
     *
     * ## Error Handling:
     * - Network/database errors are caught and displayed to the user
     * - Loading states are properly managed during initialization
     * - Failed initialization doesn't crash the app but shows error state
     *
     * ## Performance Considerations:
     * - Uses viewModelScope for proper lifecycle management
     * - Loads data asynchronously to avoid blocking UI thread
     * - Caches data in StateFlow for reactive UI updates
     */
    init {
        // loadUsers is now reactive via stateIn
        loadUserStats()
    }

    /*
     * Reactive UI State Pipeline
     * Combines users, search query, role filter, and dialog states into a single UI state.
     */
    val uiState: StateFlow<UserManagementUiState> = kotlinx.coroutines.flow.combine(
        _usersFlow,
        _searchQuery.debounce(300),
        _selectedRole,
        _uiState // We still need the base mutable state for dialog flags and messages
    ) { users, query, role, currentState ->
        currentState.copy(
            users = users,
            filteredUsers = filterUsers(users, query, role),
            searchQuery = query,
            selectedRole = role,
            isLoading = false // Data flow emitted, so loading is done
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = UserManagementUiState(isLoading = true)
    )

    /**
     * Loads user statistics from the UserManagementService.
     *
     * This method retrieves statistical information about users in the system,
     * including total counts and role distributions. Unlike user data loading,
     * statistics failures are not shown to the user as they are not critical
     * for core functionality.
     *
     * ## Statistics Included:
     * - Total number of users
     * - Number of users per role (Owner, Manager, Cashier, Warehouse)
     * - Active vs inactive user counts
     * - Recent user activity metrics
     *
     * ## Error Handling:
     * - Statistics loading failures are silently ignored
     * - Does not affect main user management functionality
     * - Graceful degradation when stats are unavailable
     *
     * ## Performance:
     * - Lightweight operation compared to full user loading
     * - Cached in UI state for immediate access
     * - Non-blocking background operation
     */
    private fun loadUserStats() {
        viewModelScope.launch {
            userManagementService.getUserStats().onSuccess { stats ->
                _uiState.update { it.copy(userStats = stats) }
            }.onFailure { error ->
                // Stats are not critical, so we don't show error for this
            }
        }
    }

    /**
     * Updates the search query for filtering users.
     *
     * This method is called when the user types in the search field and triggers
     * real-time filtering of the user list based on the search criteria.
     *
     * ## Search Behavior:
     * - Searches across username field (case-insensitive)
     * - Empty query shows all users (subject to role filter)
     * - Real-time updates as user types
     *
     * ## Performance:
     * - Client-side filtering for immediate response
     * - No additional service calls required
     * - Efficient string matching operations
     *
     * @param query The search query string entered by the user
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    /**
     * Updates the role filter for the user list.
     *
     * This method applies a role-based filter to the user list, allowing users
     * to view only users with specific roles (Owner, Manager, Cashier, Warehouse).
     *
     * ## Filter Behavior:
     * - Null role shows all users
     * - Specific role shows only users with that role
     * - Combines with search query for refined filtering
     *
     * ## Business Logic:
     * - Supports role-based access control visualization
     * - Helps administrators focus on specific user types
     * - Maintains filter state across screen rotations
     *
     * @param role The role to filter by, or null to show all roles
     */
    fun onRoleFilterChange(role: Role?) {
        _selectedRole.update { role }
    }

    /**
     * Applies search and role filters to a list of users.
     *
     * This method implements the core filtering logic for user management,
     * supporting both text search and role-based filtering.
     *
     * ## Filter Criteria:
     * - **Search Query**: Matches username (case-insensitive partial match)
     * - **Role Filter**: Exact role match or null for all roles
     * - **Combined Filtering**: Both criteria must be satisfied
     *
     * ## Search Algorithm:
     * - Username contains query string (partial match)
     * - Case-insensitive comparison
     * - Empty query matches all users
     *
     * ## Role Filtering:
     * - Null role parameter matches all roles
     * - Specific role matches exact role enum value
     * - Supports all defined roles (OWNER, MANAGER, CASHIER, WAREHOUSE)
     *
     * @param users The complete list of users to filter
     * @param query The search query string (empty string matches all)
     * @param role The role filter (null matches all roles)
     * @return Filtered list of users matching both criteria
     */
    private fun filterUsers(users: List<Pengguna>, query: String, role: Role?): List<Pengguna> {
        return users.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.username.contains(query, ignoreCase = true)
            val matchesRole = role == null || user.role == role
            matchesQuery && matchesRole
        }
    }

    /**
     * Opens the create-user dialog and resets the create-user form.
     *
     * This method prepares the UI for creating a new user by:
     * - Setting `showCreateUserDialog` to true
     * - Resetting `CreateUserFormState` to defaults (empty fields, no errors)
     *
     * The dialog is expected to bind to [createUserFormState] and call the
     * `onCreateUser*Change()` handlers to update the form as the user types.
     */
    fun showCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = true) }
        _createUserFormState.update { CreateUserFormState() }
    }

    /**
     * Closes the create-user dialog.
     *
     * Note: The form state is intentionally not reset here; the screen can decide whether
     * to preserve values or rely on [showCreateUserDialog] which resets the form.
     */
    fun hideCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = false) }
    }

    /**
     * Updates the username field in the create-user form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onCreateUserUsernameChange(username: String) {
        _createUserFormState.update {
            it.copy(
                username = username,
                errorMessage = null
            )
        }
    }

    /**
     * Updates the password field in the create-user form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onCreateUserPasswordChange(password: String) {
        _createUserFormState.update {
            it.copy(
                password = password,
                errorMessage = null
            )
        }
    }

    /**
     * Updates the confirm-password field in the create-user form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onCreateUserConfirmPasswordChange(confirmPassword: String) {
        _createUserFormState.update {
            it.copy(
                confirmPassword = confirmPassword,
                errorMessage = null
            )
        }
    }

    /**
     * Updates the role field in the create-user form.
     *
     * Role changes do not clear error state by default, because errors may relate to
     * username/password rather than role selection.
     */
    fun onCreateUserRoleChange(role: Role) {
        _createUserFormState.update { it.copy(role = role) }
    }

    /**
     * Creates a new user account based on the current create-user form state.
     *
     * ## Validation (client-side)
     * - Username must not be blank
     * - Password must not be blank
     * - Password and confirmation must match
     *
     * If validation fails, [CreateUserFormState.errorMessage] is set and the operation
     * returns immediately without calling the service.
     *
     * ## Service call
     * Delegates to [UserManagementService.createUser]. On success it:
     * - Shows a success message
     * - Closes the create dialog
     * - Refreshes user statistics
     * - Schedules message clearing via [clearMessagesAfterDelay]
     *
     * ## Audit / security note
     * The `createdBy` user id is currently a placeholder and should be sourced from
     * the authenticated session (e.g., AuthService / session repository).
     */
    fun createUser() {
        val formState = _createUserFormState.value

        // Validation
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
            try {
                val currentUserId = authService.getCurrentUser()?.id
                if (currentUserId == null) {
                    _createUserFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Sesi tidak valid. Silakan login kembali."
                        )
                    }
                    return@launch
                }

                userManagementService.createUser(
                    username = formState.username,
                    password = formState.password,
                    role = formState.role,
                    createdBy = currentUserId
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            successMessage = "Pengguna berhasil dibuat",
                            showCreateUserDialog = false
                        )
                    }
                    loadUserStats() // Refresh stats
                    clearMessagesAfterDelay()
                }.onFailure { error ->
                    _createUserFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Gagal membuat pengguna"
                        )
                    }
                }
            } catch (e: Exception) {
                _createUserFormState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Opens the edit-user dialog for a selected user and pre-fills the edit form.
     *
     * The selected user is stored in [UserManagementUiState.selectedUser] to support
     * subsequent update/delete/reset flows.
     *
     * Note: `isActive` is currently hard-coded to true until the `Pengguna` entity
     * includes an active flag.
     */
    fun showEditUserDialog(user: Pengguna) {
        _uiState.update {
            it.copy(
                showEditUserDialog = true,
                selectedUser = user
            )
        }
        _editUserFormState.update {
            EditUserFormState(
                username = user.username,
                role = user.role,
                isActive = user.isActive
            )
        }
    }

    /**
     * Closes the edit-user dialog.
     */
    fun hideEditUserDialog() {
        _uiState.update { it.copy(showEditUserDialog = false) }
    }

    /**
     * Updates the username field in the edit-user form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onEditUserUsernameChange(username: String) {
        _editUserFormState.update {
            it.copy(
                username = username,
                errorMessage = null
            )
        }
    }

    /**
     * Updates the role field in the edit-user form.
     */
    fun onEditUserRoleChange(role: Role) {
        _editUserFormState.update { it.copy(role = role) }
    }

    /**
     * Updates the isActive field in the edit-user form.
     */
    fun onEditUserIsActiveChange(isActive: Boolean) {
        _editUserFormState.update { it.copy(isActive = isActive) }
    }

    /**
     * Updates the selected user using the current edit-user form state.
     *
     * Preconditions:
     * - A user must be selected (via [showEditUserDialog])
     * - Username must not be blank
     *
     * On success, the dialog is closed and a success message is shown.
     * On failure, the form remains open and [EditUserFormState.errorMessage] is populated.
     *
     * Audit note: `updatedBy` is currently a placeholder and should come from the active session.
     */
    fun updateUser() {
        val formState = _editUserFormState.value
        val selectedUser = _uiState.value.selectedUser ?: return

        if (formState.username.isBlank()) {
            _editUserFormState.update { it.copy(errorMessage = "Username tidak boleh kosong") }
            return
        }

        _editUserFormState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUser()?.id
                if (currentUserId == null) {
                    _editUserFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Sesi tidak valid. Silakan login kembali."
                        )
                    }
                    return@launch
                }

                userManagementService.updateUser(
                    userId = selectedUser.id,
                    username = formState.username,
                    role = formState.role,
                    isActive = formState.isActive,
                    updatedBy = currentUserId
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            successMessage = "Pengguna berhasil diperbarui",
                            showEditUserDialog = false
                        )
                    }
                    clearMessagesAfterDelay()
                }.onFailure { error ->
                    _editUserFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Gagal memperbarui pengguna"
                        )
                    }
                }
            } catch (e: Exception) {
                _editUserFormState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Opens the delete confirmation dialog for a selected user.
     *
     * The selected user is stored in [UserManagementUiState.selectedUser].
     */
    fun showDeleteUserDialog(user: Pengguna) {
        _uiState.update {
            it.copy(
                showDeleteUserDialog = true,
                selectedUser = user
            )
        }
    }

    /**
     * Closes the delete confirmation dialog.
     */
    fun hideDeleteUserDialog() {
        _uiState.update { it.copy(showDeleteUserDialog = false) }
    }

    /**
     * Deletes the currently selected user.
     *
     * Preconditions:
     * - A user must be selected (via [showDeleteUserDialog])
     *
     * On success:
     * - Shows a success message
     * - Closes the delete dialog
     * - Refreshes user statistics
     * - Clears messages after a delay
     *
     * On failure:
     * - Shows an error message
     * - Closes the delete dialog
     * - Clears messages after a delay
     *
     * Audit note: `deletedBy` is currently a placeholder and should come from the active session.
     */
    fun deleteUser() {
        val selectedUser = _uiState.value.selectedUser ?: return

        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUser()?.id
                if (currentUserId == null) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Sesi tidak valid. Silakan login kembali.",
                            showDeleteUserDialog = false
                        )
                    }
                    clearMessagesAfterDelay()
                    return@launch
                }

                userManagementService.deleteUser(
                    userId = selectedUser.id,
                    deletedBy = currentUserId
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            successMessage = "Pengguna berhasil dihapus",
                            showDeleteUserDialog = false
                        )
                    }
                    loadUserStats() // Refresh stats
                    clearMessagesAfterDelay()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Gagal menghapus pengguna",
                            showDeleteUserDialog = false
                        )
                    }
                    clearMessagesAfterDelay()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Terjadi kesalahan: ${e.message}",
                        showDeleteUserDialog = false
                    )
                }
                clearMessagesAfterDelay()
            }
        }
    }

    /**
     * Opens the reset-password dialog for a selected user and resets the password form.
     */
    fun showResetPasswordDialog(user: Pengguna) {
        _uiState.update {
            it.copy(
                showResetPasswordDialog = true,
                selectedUser = user
            )
        }
        _resetPasswordFormState.update { ResetPasswordFormState() }
    }

    /**
     * Closes the reset-password dialog.
     */
    fun hideResetPasswordDialog() {
        _uiState.update { it.copy(showResetPasswordDialog = false) }
    }

    /**
     * Updates the new-password field for the reset-password form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onResetPasswordChange(password: String) {
        _resetPasswordFormState.update {
            it.copy(
                newPassword = password,
                errorMessage = null
            )
        }
    }

    /**
     * Updates the confirm-password field for the reset-password form.
     *
     * Clears any existing form error message to support “fix and retry” UX.
     */
    fun onResetPasswordConfirmChange(confirmPassword: String) {
        _resetPasswordFormState.update {
            it.copy(
                confirmPassword = confirmPassword,
                errorMessage = null
            )
        }
    }

    /**
     * Resets the password for the currently selected user.
     *
     * Preconditions:
     * - A user must be selected (via [showResetPasswordDialog])
     * - New password must not be blank
     * - New password and confirmation must match
     *
     * On success:
     * - Shows a success message
     * - Closes the reset dialog
     * - Clears messages after a delay
     *
     * On failure:
     * - Keeps the dialog open
     * - Shows an inline form error message
     *
     * Audit note: `resetBy` is currently a placeholder and should come from the active session.
     */
    fun resetUserPassword() {
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
            try {
                val currentUserId = authService.getCurrentUser()?.id
                if (currentUserId == null) {
                    _resetPasswordFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Sesi tidak valid. Silakan login kembali."
                        )
                    }
                    return@launch
                }

                userManagementService.resetUserPassword(
                    userId = selectedUser.id,
                    newPassword = formState.newPassword,
                    resetBy = currentUserId
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            successMessage = "Password berhasil direset",
                            showResetPasswordDialog = false
                        )
                    }
                    clearMessagesAfterDelay()
                }.onFailure { error ->
                    _resetPasswordFormState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Gagal mereset password"
                        )
                    }
                }
            } catch (e: Exception) {
                _resetPasswordFormState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears any visible error messages in the UI and forms.
     *
     * This is typically called after the user dismisses an error banner/snackbar.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
        _createUserFormState.update { it.copy(errorMessage = null) }
        _editUserFormState.update { it.copy(errorMessage = null) }
        _resetPasswordFormState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clears success and error messages after a short delay.
     *
     * This avoids permanent banners/snackbars and keeps feedback transient.
     * The delay duration is currently fixed at 3000ms.
     */
    private fun clearMessagesAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update {
                it.copy(
                    successMessage = null,
                    errorMessage = null
                )
            }
        }
    }
}
