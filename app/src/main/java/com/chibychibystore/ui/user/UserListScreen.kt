package com.chibychibystore.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.ui.components.UserManagementDialogs
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.EmptyState
import com.chibychibystore.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavController,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val createUserFormState by viewModel.createUserFormState.collectAsState()
    val editUserFormState by viewModel.editUserFormState.collectAsState()
    val resetPasswordFormState by viewModel.resetPasswordFormState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manajemen User",
                actions = {
                    IconButton(
                        onClick = { viewModel.showCreateUserDialog() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah User"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Cari user...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedRole?.name ?: "Semua Role",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .width(140.dp)
                                .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Role") },
                            onClick = {
                                viewModel.onRoleFilterChange(null)
                                expanded = false
                            }
                        )
                        val roles = remember { Role.values() }
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    viewModel.onRoleFilterChange(role)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // User Stats Cards
            uiState.userStats?.let { stats ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserStatCard("Total", stats.totalUsers.toString(), Modifier.weight(1f))
                    UserStatCard("Owner", stats.owners.toString(), Modifier.weight(1f))
                    UserStatCard("Mgr", stats.managers.toString(), Modifier.weight(1f))
                    UserStatCard("Kasir", stats.cashiers.toString(), Modifier.weight(1f))
                    UserStatCard("Warehouse", stats.warehouseStaff.toString(), Modifier.weight(1f))
                }
            }

            // Content
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.filteredUsers.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Person,
                            title = "Belum ada user",
                            message = "Silakan tambah user baru atau ubah filter pencarian"
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.filteredUsers,
                                key = { it.id }
                            ) { user ->
                                UserListItem(
                                    user = user,
                                    onClick = {
                                        navController.navigate(Screen.UserDetail.createRoute(user.id))
                                    },
                                    onEdit = { viewModel.showEditUserDialog(user) },
                                    onDelete = { viewModel.showDeleteUserDialog(user) },
                                    onResetPassword = { viewModel.showResetPasswordDialog(user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    UserManagementDialogs(
        showCreateDialog = uiState.showCreateUserDialog,
        showEditDialog = uiState.showEditUserDialog,
        showDeleteDialog = uiState.showDeleteUserDialog,
        showResetPasswordDialog = uiState.showResetPasswordDialog,
        createUserFormState = createUserFormState,
        editUserFormState = editUserFormState,
        resetPasswordFormState = resetPasswordFormState,
        selectedUser = uiState.selectedUser,
        onCreateUser = viewModel::createUser,
        onUpdateUser = viewModel::updateUser,
        onDeleteUser = viewModel::deleteUser,
        onResetPassword = viewModel::resetPassword, // Match VM method name
        onDismissCreate = viewModel::hideCreateUserDialog,
        onDismissEdit = viewModel::hideEditUserDialog,
        onDismissDelete = viewModel::hideDeleteUserDialog,
        onDismissResetPassword = viewModel::hideResetPasswordDialog,
        onCreateUsernameChange = viewModel::onCreateUserUsernameChange,
        onCreatePasswordChange = viewModel::onCreateUserPasswordChange,
        onCreateConfirmPasswordChange = viewModel::onCreateUserConfirmPasswordChange,
        onCreateRoleChange = viewModel::onCreateUserRoleChange,
        onEditUsernameChange = viewModel::onEditUserUsernameChange,
        onEditRoleChange = viewModel::onEditUserRoleChange,
        onEditIsActiveChange = viewModel::onEditUserIsActiveChange,
        onResetPasswordChange = viewModel::onResetPasswordNewPasswordChange,
        onResetPasswordConfirmChange = viewModel::onResetPasswordConfirmPasswordChange
    )

    // Handle errors and success messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = success,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun UserStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserListItem(
    user: Pengguna,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${user.role}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onResetPassword) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Reset Password",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
