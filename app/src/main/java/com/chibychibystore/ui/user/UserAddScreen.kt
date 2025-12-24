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

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val roles = remember { Role.values() }

    fun validateInput(): Boolean {
        // Reset errors
        usernameError = null
        passwordError = null
        confirmPasswordError = null

        var isValid = true

        if (username.isBlank()) {
            usernameError = "Username tidak boleh kosong"
            isValid = false
        }
        if (password.length < 6) {
            passwordError = "Password minimal 6 karakter"
            isValid = false
        }
        if (password != confirmPassword) {
            confirmPasswordError = "Password tidak cocok"
            isValid = false
        }
        return isValid
    }

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
                onValueChange = {
                    username = it
                    usernameError = null
                },
                label = "Username",
                modifier = Modifier.fillMaxWidth(),
                isError = usernameError != null,
                errorMessage = usernameError
            )

            TextFieldOutlined(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = "Password",
                modifier = Modifier.fillMaxWidth(),
                isError = passwordError != null,
                errorMessage = passwordError,
                isPassword = true
            )

            TextFieldOutlined(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = "Konfirmasi Password",
                modifier = Modifier.fillMaxWidth(),
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError,
                isPassword = true
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
                        onClick = { selectedRole = role },
                        label = { Text(role.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ButtonPrimary(
                text = "Simpan",
                onClick = {
                    if (validateInput()) {
                        scope.launch {
                            val result = viewModel.createUser(username, password, selectedRole)
                            if (result.isSuccess) {
                                navController.navigateUp()
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = result.exceptionOrNull()?.message ?: "Gagal membuat pengguna"
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
        }
    }
}
