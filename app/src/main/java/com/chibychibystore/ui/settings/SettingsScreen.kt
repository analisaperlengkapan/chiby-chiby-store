package com.chibychibystore.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.settings.ChangePasswordDialog
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.Error
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showAboutDialog by remember { mutableStateOf(false) }

    ChibyScaffold(
        title = "Pengaturan",
        onNavigateUp = { /* Top level screen, no up navigation */ }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator("Memuat pengaturan...")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Profile Section
                item {
                    ChibyCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = ChibyPinkPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    tint = ChibyPinkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            uiState.currentUser?.let { user ->
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.role.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Settings Section Title
                item {
                    Text(
                        text = "Akun & Keamanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Change Password
                item {
                    SettingsItem(
                        title = "Ubah Password",
                        subtitle = "Perbarui kata sandi akun Anda",
                        icon = Icons.Default.Lock,
                        onClick = { showChangePasswordDialog = true }
                    )
                }

                // Backup Data
                item {
                    SettingsItem(
                        title = "Backup Data",
                        subtitle = uiState.lastBackupDate?.let { "Terakhir: ${formatDate(it)}" } ?: "Kelola backup dan restore database",
                        icon = Icons.Default.Backup,
                        onClick = { onNavigateToRoute(Screen.Backup.route) }
                    )
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // About
                item {
                    SettingsItem(
                        title = "Tentang Aplikasi",
                        subtitle = "Versi 1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                }

                // Logout
                item {
                    SettingsItem(
                        title = "Keluar Aplikasi",
                        subtitle = "Logout dari sesi saat ini",
                        icon = Icons.Default.Logout,
                        onClick = { showLogoutDialog = true },
                        iconTint = Error,
                        textColor = Error
                    )
                }

                // Error Message Display
                uiState.error?.let { error ->
                    item {
                        ChibyCard(
                            containerColor = Error.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = error, color = Error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Tentang Chiby Chiby Store") },
            text = { Text("Versi 1.0.0\nAplikasi POS offline-first untuk toko retail.\nBuilt with ❤️ by Chiby Team") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup", color = ChibyPinkPrimary)
                }
            },
            icon = { Icon(Icons.Default.Store, contentDescription = null, tint = ChibyPinkPrimary) }
        )
    }

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
                    Text("Ya, Logout", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (uiState.passwordChangeSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPasswordChangeSuccess() },
            title = { Text("Berhasil") },
            text = { Text("Password berhasil diubah") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPasswordChangeSuccess() }) {
                    Text("Oke", color = ChibyPinkPrimary)
                }
            },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)) }
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = ChibyPinkPrimary,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ChibyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 0,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}