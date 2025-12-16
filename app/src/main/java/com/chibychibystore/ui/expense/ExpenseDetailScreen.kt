@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.expense
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.ui.components.shared.*
import com.chibychibystore.ui.components.dialogs.ConfirmDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    navController: NavController,
    expenseId: Long,
    viewModel: ExpenseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId)
    }

    Scaffold(
        topBar = {
                AppTopBar(
                title = if (uiState.isEditing) "Edit Pengeluaran" else "Detail Pengeluaran",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    if (!uiState.isEditing && uiState.expense != null) {
                        IconButton(onClick = { viewModel.toggleEditMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null -> {
                    ErrorMessage(message = uiState.error ?: "")
                }
                uiState.expense == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Pengeluaran tidak ditemukan",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    uiState.expense?.let { expense ->

                        if (uiState.isEditing) {
                            // Edit Mode
                            OutlinedTextField(
                                value = uiState.editAmount,
                                onValueChange = { viewModel.updateEditAmount(it) },
                                label = { Text("Jumlah (Rp)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )

                            var categoryExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.editCategory?.displayName ?: "",
                                    onValueChange = { },
                                    label = { Text("Kategori") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    ExpenseCategory.values().forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.displayName) },
                                            onClick = {
                                                viewModel.updateEditCategory(category)
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = uiState.editDescription,
                                onValueChange = { viewModel.updateEditDescription(it) },
                                label = { Text("Deskripsi") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleEditMode() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Batal")
                                }
                                Button(
                                    onClick = {
                                        viewModel.saveExpense {
                                            viewModel.toggleEditMode()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.isEditFormValid
                                ) {
                                    Text("Simpan")
                                }
                            }
                        } else {
                            // View Mode
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Detail Pengeluaran",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    DetailRow("Jumlah", formatCurrency(expense.amount))
                                    DetailRow("Kategori", expense.category.displayName)
                                    DetailRow("Tanggal", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.expenseDate))
                                    DetailRow("Deskripsi", expense.description ?: "Tidak ada deskripsi")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Hapus Pengeluaran?",
            message = "Pengeluaran ini akan dihapus. Tindakan ini tidak dapat dibatalkan.",
            confirmText = "Hapus",
            dismissText = "Batal",
            onConfirm = {
                viewModel.deleteExpense {
                    showDeleteDialog = false
                    navController.navigateUp()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount)
}