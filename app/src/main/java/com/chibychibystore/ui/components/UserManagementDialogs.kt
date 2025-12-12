package com.chibychibystore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.ui.viewmodel.CreateUserFormState
import com.chibychibystore.ui.viewmodel.EditUserFormState
import com.chibychibystore.ui.viewmodel.ResetPasswordFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementDialogs(
    showCreateDialog: Boolean,
    showEditDialog: Boolean,
    showDeleteDialog: Boolean,
    showResetPasswordDialog: Boolean,
    createUserFormState: CreateUserFormState,
    editUserFormState: EditUserFormState,
    resetPasswordFormState: ResetPasswordFormState,
    selectedUser: Pengguna?,
    onCreateUser: () -> Unit,
    onUpdateUser: () -> Unit,
    onDeleteUser: () -> Unit,
    onResetPassword: () -> Unit,
    onDismissCreate: () -> Unit,
    onDismissEdit: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissResetPassword: () -> Unit,
    onCreateUsernameChange: (String) -> Unit,
    onCreatePasswordChange: (String) -> Unit,
    onCreateConfirmPasswordChange: (String) -> Unit,
    onCreateRoleChange: (Role) -> Unit,
    onEditUsernameChange: (String) -> Unit,
    onEditRoleChange: (Role) -> Unit
) {
    // Create User Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = onDismissCreate,
            title = { Text("Tambah Pengguna Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = createUserFormState.username,
                        onValueChange = onCreateUsernameChange,
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = createUserFormState.errorMessage?.contains("username", ignoreCase = true) == true
                    )

                    OutlinedTextField(
                        value = createUserFormState.password,
                        onValueChange = onCreatePasswordChange,
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = createUserFormState.errorMessage?.contains("password", ignoreCase = true) == true
                    )

                    OutlinedTextField(
                        value = createUserFormState.confirmPassword,
                        onValueChange = onCreateConfirmPasswordChange,
                        label = { Text("Konfirmasi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = createUserFormState.errorMessage?.contains("konfirmasi", ignoreCase = true) == true
                    )

                    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = createUserFormState.role.name,
                            onValueChange = {},
                            label = { Text("Role") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Role.values().forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        onCreateRoleChange(role)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    createUserFormState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onCreateUser,
                    enabled = !createUserFormState.isSubmitting
                ) {
                    if (createUserFormState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Tambah")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreate) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit User Dialog
    if (showEditDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = onDismissEdit,
            title = { Text("Edit Pengguna") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editUserFormState.username,
                        onValueChange = onEditUsernameChange,
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = editUserFormState.errorMessage?.contains("username", ignoreCase = true) == true
                    )

                    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = editUserFormState.role.name,
                            onValueChange = {},
                            label = { Text("Role") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Role.values().forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        onEditRoleChange(role)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    editUserFormState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onUpdateUser,
                    enabled = !editUserFormState.isSubmitting
                ) {
                    if (editUserFormState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Simpan")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEdit) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete User Dialog
    if (showDeleteDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Hapus Pengguna") },
            text = {
                Text("Apakah Anda yakin ingin menghapus pengguna '${selectedUser.username}'? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = onDeleteUser,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Batal")
                }
            }
        )
    }

    // Reset Password Dialog
    if (showResetPasswordDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = onDismissResetPassword,
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Reset password untuk pengguna '${selectedUser.username}'")

                    OutlinedTextField(
                        value = resetPasswordFormState.newPassword,
                        onValueChange = { /* onResetPasswordChange */ },
                        label = { Text("Password Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = resetPasswordFormState.errorMessage?.contains("password", ignoreCase = true) == true
                    )

                    OutlinedTextField(
                        value = resetPasswordFormState.confirmPassword,
                        onValueChange = { /* onResetPasswordConfirmChange */ },
                        label = { Text("Konfirmasi Password Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = resetPasswordFormState.errorMessage?.contains("konfirmasi", ignoreCase = true) == true
                    )

                    resetPasswordFormState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onResetPassword,
                    enabled = !resetPasswordFormState.isSubmitting
                ) {
                    if (resetPasswordFormState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Reset Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetPassword) {
                    Text("Batal")
                }
            }
        )
    }
}