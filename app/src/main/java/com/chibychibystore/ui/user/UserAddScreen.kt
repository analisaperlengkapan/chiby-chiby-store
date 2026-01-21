@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.R
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
    val formState by viewModel.createUserFormState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val roles = remember { Role.values() }

    // Handle success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                navController.navigateUp()
            }
        }
    }

    // Handle form error messages from ViewModel
    LaunchedEffect(formState.errorMessage) {
        formState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            // Note: In a real app we might want to clear this, but the VM handles it on next input
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tambah User",
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
            if (uiState.isLoading || formState.isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            TextFieldOutlined(
                value = formState.username,
                onValueChange = viewModel::onCreateUserUsernameChange,
                label = stringResource(R.string.username),
                modifier = Modifier.fillMaxWidth(),
                isError = false, // VM handles validation via snackbar/error message in state
                errorMessage = null
            )

            TextFieldOutlined(
                value = formState.password,
                onValueChange = viewModel::onCreateUserPasswordChange,
                label = stringResource(R.string.password),
                modifier = Modifier.fillMaxWidth(),
                isError = false,
                errorMessage = null,
                isPassword = true
            )

            TextFieldOutlined(
                value = formState.confirmPassword,
                onValueChange = viewModel::onCreateUserConfirmPasswordChange,
                label = "Konfirmasi Password",
                modifier = Modifier.fillMaxWidth(),
                isError = false,
                errorMessage = null,
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
                        selected = formState.role == role,
                        onClick = { viewModel.onCreateUserRoleChange(role) },
                        label = { Text(role.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ButtonPrimary(
                text = stringResource(R.string.common_save),
                onClick = viewModel::createUser,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !formState.isSubmitting
            )
        }
    }
}
