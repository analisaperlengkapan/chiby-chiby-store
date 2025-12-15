package com.chibychibystore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import com.chibychibystore.ui.components.shared.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.ui.components.settings.ChangePasswordDialog
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.BottomNavBar
import com.chibychibystore.ui.components.shared.CardItem
import com.chibychibystore.ui.components.shared.LoadingIndicator

import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard.route),
        BottomNavItem("Inventory", Icons.Default.Inventory, Screen.Inventory.route),
        BottomNavItem("Sales", Icons.Default.PointOfSale, Screen.SalesHistory.route),
        BottomNavItem("Reports", Icons.Default.Analytics, Screen.Reports.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Pengaturan"
            )
        },
        bottomBar = {
            BottomNavBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemClick = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(
                message = "Memuat pengaturan...",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Profile Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Profil Pengguna",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            uiState.currentUser?.let { user ->
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = user.role.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Settings Options
                item {
                    Text(
                        text = "Opsi Pengaturan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Change Password
                item {
                    CardItem(
                        title = "Ubah Password",
                        subtitle = "Ubah password akun Anda",
                        icon = Icons.Default.Person,
                        onClick = { showChangePasswordDialog = true }
                    )
                }

                // Backup Data
                item {
                    CardItem(
                        title = "Backup Data",
                        subtitle = uiState.lastBackupDate?.let {
                            "Terakhir: ${formatDate(it)}"
                        } ?: "Kelola backup data",
                        icon = Icons.Default.Backup,
                        onClick = { onNavigateToRoute(Screen.Backup.route) }
                    )
                }

                // About
                item {
                    CardItem(
                        title = "Tentang Aplikasi",
                        subtitle = "Versi 1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { /* TODO: Show about dialog */ }
                    )
                }

                // Logout
                item {
                    CardItem(
                        title = "Logout",
                        subtitle = "Keluar dari aplikasi",
                        icon = Icons.Default.Logout,
                        onClick = { showLogoutDialog = true }
                    )
                }

                // Error Message
                uiState.error?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = {
                showChangePasswordDialog = false
                viewModel.clearError()
                viewModel.clearPasswordChangeSuccess()
            },
            onConfirm = { oldPassword, newPassword ->
                viewModel.changePassword(oldPassword, newPassword)
                showChangePasswordDialog = false
            },
            isLoading = uiState.isChangingPassword,
            error = uiState.error
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dari aplikasi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogout)
                    }
                ) {
                    Text("Ya, Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Password Change Success Dialog
    if (uiState.passwordChangeSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPasswordChangeSuccess() },
            title = { Text("Berhasil") },
            text = { Text("Password berhasil diubah") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPasswordChangeSuccess() }) {
                    Text("Oke")
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}