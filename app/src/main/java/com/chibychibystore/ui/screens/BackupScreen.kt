@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.CardItem
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.viewmodel.BackupUiState
import com.chibychibystore.ui.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val restoreProgress by viewModel.restoreProgress.collectAsState()
    
    val currentBackupProgress = backupProgress
    // Capture restore into a stable local variable to allow smart casts
    val currentRestoreProgress = restoreProgress

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
                AppTopBar(
                title = "Backup & Restore",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(
                message = "Memuat riwayat backup...",
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
                // Create Backup Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Buat Backup Baru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Backup semua data bisnis ke file terenkripsi yang aman.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (currentBackupProgress != null && currentBackupProgress.isInProgress) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = currentBackupProgress.currentStep,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    LinearProgressIndicator(
                                        progress = currentBackupProgress.progress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "${currentBackupProgress.currentStepIndex}/${currentBackupProgress.totalSteps}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.createBackup() },
                                enabled = !uiState.isCreatingBackup && (currentBackupProgress == null || !currentBackupProgress.isInProgress),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buat Backup")
                            }
                        }
                    }
                }

                // Backup History Section
                item {
                    Text(
                        text = "Riwayat Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.backupHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Belum ada backup",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.backupHistory) { backupInfo ->
                        BackupItem(
                            backupInfo = backupInfo,
                            onDelete = { showDeleteDialog = backupInfo.id },
                            onRestore = { showRestoreDialog = backupInfo.filePath }
                        )
                    }
                }

                // Restore progress indicator (if restoring)
                if (currentRestoreProgress != null && currentRestoreProgress.isInProgress) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Mengembalikan backup...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                LinearProgressIndicator(
                                    progress = currentRestoreProgress.progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = currentRestoreProgress.currentStep,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Error Message
                uiState.error?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.clearError() }) {
                                        Text("Tutup")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { backupId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Backup") },
            text = { Text("Apakah Anda yakin ingin menghapus backup ini? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackup(backupId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    showRestoreDialog?.let { backupPath ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("Pulihkan Data") },
            text = { Text("Apakah Anda yakin ingin memulihkan data dari backup ini? Data yang ada saat ini akan digantikan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromBackup(backupPath)
                        showRestoreDialog = null
                    }
                ) {
                    Text("Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun BackupItem(
    backupInfo: com.chibychibystore.service.BackupInfo,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backupInfo.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(backupInfo.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ukuran: ${formatFileSize(backupInfo.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Pulihkan",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Pulihkan")
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
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}