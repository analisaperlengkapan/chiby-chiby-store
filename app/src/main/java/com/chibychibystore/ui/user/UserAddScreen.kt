@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ButtonPrimary
import com.chibychibystore.ui.components.shared.TextFieldOutlined
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAddScreen(
    navController: NavController,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(Role.CASHIER) }

    Scaffold(
        topBar = {
                AppTopBar(
                title = "Tambah Pengguna",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextFieldOutlined(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                modifier = Modifier.fillMaxWidth()
            )

            TextFieldOutlined(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                modifier = Modifier.fillMaxWidth()
            )

            TextFieldOutlined(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Konfirmasi Password",
                modifier = Modifier.fillMaxWidth()
            )

            // Role Selection
            Text("Role", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Role.values().forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ButtonPrimary(
                text = "Simpan",
                onClick = {
                    scope.launch {
                        val validationError = validateInput(username, password, confirmPassword)
                        if (validationError == null) {
                            val result = viewModel.createUser(username, password, selectedRole)
                            if (result.isSuccess) {
                                navController.navigateUp()
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = result.exceptionOrNull()?.message ?: "Gagal membuat pengguna"
                                )
                            }
                        } else {
                            snackbarHostState.showSnackbar(validationError)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
        }
    }
}

private fun validateInput(username: String, password: String, confirmPassword: String): String? {
    if (username.isBlank()) return "Username tidak boleh kosong"
    if (password.length < 6) return "Password minimal 6 karakter"
    if (password != confirmPassword) return "Password tidak cocok"
    return null
}
