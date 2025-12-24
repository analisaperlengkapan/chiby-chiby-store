package com.chibychibystore.ui.screens

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
import kotlinx.coroutines.launch
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.ui.components.shared.*
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.UserManagementDialogs
import com.chibychibystore.ui.viewmodel.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val createUserFormState by viewModel.createUserFormState.collectAsState()
    val editUserFormState by viewModel.editUserFormState.collectAsState()
    val resetPasswordFormState by viewModel.resetPasswordFormState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manajemen Pengguna",
                navigationIcon = Icons.Default.Menu,
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = { viewModel.showCreateUserDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Pengguna")
                    }
                }
            )
        }
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
                    placeholder = { Text("Cari pengguna...") },
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
                        modifier = Modifier.width(120.dp)
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
                        Role.values().forEach { role ->
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
                    UserStatCard("Manager", stats.managers.toString(), Modifier.weight(1f))
                    UserStatCard("Cashier", stats.cashiers.toString(), Modifier.weight(1f))
                    UserStatCard("Warehouse", stats.warehouseStaff.toString(), Modifier.weight(1f))
                }
            }

            // Messages
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Tutup")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            uiState.successMessage?.let { success ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(success)
                }
            }

            // Users List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onEdit = { viewModel.showEditUserDialog(user) },
                            onDelete = { viewModel.showDeleteUserDialog(user) },
                            onResetPassword = { viewModel.showResetPasswordDialog(user) },
                            onClick = { onNavigateToRoute(Screen.UserDetail.createRoute(user.id)) }
                        )
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
        onResetPassword = viewModel::resetUserPassword,
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
        onResetPasswordChange = viewModel::onResetPasswordChange,
        onResetPasswordConfirmChange = viewModel::onResetPasswordConfirmChange
    )
}

@Composable
private fun UserStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCard(
    user: Pengguna,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.role.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dibuat: ${user.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onResetPassword) {
                    Icon(Icons.Default.Lock, contentDescription = "Reset Password")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}