@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ButtonPrimary
import com.chibychibystore.ui.components.shared.TextFieldOutlined
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    navController: NavController,
    userId: Long,
    viewModel: UserDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus pengguna ${uiState.user?.username ?: ""}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteUser {
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detail Pengguna",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleEditMode() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(
                        onClick = {
                            // Show delete confirmation dialog
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.user != null -> {
                uiState.user?.let { user ->
                    UserDetailContent(
                        user = user,
                        isEditMode = uiState.isEditMode,
                        onUsernameChange = viewModel::updateUsername,
                        onRoleChange = viewModel::updateRole,
                        onSave = {
                            scope.launch {
                                viewModel.saveUser()
                            }
                        },
                        onCancel = viewModel::cancelEdit,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailContent(
    user: com.chibychibystore.data.local.entity.Pengguna,
    isEditMode: Boolean,
    onUsernameChange: (String) -> Unit,
    onRoleChange: (Role) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember(user.username) { mutableStateOf(user.username) }
    var selectedRole by remember(user.role) { mutableStateOf(user.role) }
    val roles = remember { Role.values() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isEditMode) {
            TextFieldOutlined(
                value = username,
                onValueChange = {
                    username = it
                    onUsernameChange(it)
                },
                label = "Username",
                modifier = Modifier.fillMaxWidth()
            )

            // Role Selection
            Text("Role", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roles.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = {
                            selectedRole = role
                            onRoleChange(role)
                        },
                        label = { Text(role.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }
                ButtonPrimary(
                    text = "Simpan",
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // View Mode
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Username: ${user.username}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Role: ${user.role.displayName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Dibuat: ${user.createdAt}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (user.updatedAt != user.createdAt) {
                        Text(
                            text = "Diupdate: ${user.updatedAt}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userManagementService: com.chibychibystore.service.UserManagementService,
    private val authService: com.chibychibystore.service.AuthService,
    savedStateHandle: SavedStateHandle
) : androidx.lifecycle.ViewModel() {

    private val userId: Long = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private var originalUser: com.chibychibystore.data.local.entity.Pengguna? = null

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
                    val error = result.exceptionOrNull()?.message ?: "Gagal memuat pengguna"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEditMode = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal menyimpan pengguna"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat menyimpan"
                )
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
                        error = result.exceptionOrNull()?.message ?: "Gagal menghapus pengguna"
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

data class UserDetailUiState(
    val user: com.chibychibystore.data.local.entity.Pengguna? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)