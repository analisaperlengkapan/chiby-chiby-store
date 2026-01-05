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

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.ui.components.DatePickerDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(
    navController: NavController,
    viewModel: ExpenseAddViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
                AppTopBar(
                title = "Tambah Expense",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() }
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
                    androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = androidx.compose.material3.MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.material3.Text(text = uiState.error ?: "")
                    }
                }
                }
                else -> {
                    // Amount Field
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.updateAmount(it) },
                        label = { Text("Jumlah (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        supportingText = {
                            Text("Masukkan jumlah expense")
                        }
                    )

                    // Category Dropdown
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedCategory?.displayName ?: "",
                            onValueChange = { },
                            label = { Text("Category") },
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
                            KategoriPengeluaran.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        viewModel.updateCategory(category)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Field
                    OutlinedTextField(
                        value = uiState.expenseDate?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        onValueChange = { },
                        label = { Text("Tanggal Expense") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                            }
                        },
                        supportingText = {
                            Text("Pilih tanggal expense")
                        }
                    )

                    // Description Field
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Text("Jelaskan detail expense (opsional)")
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveExpense {
                                navController.navigateUp()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isFormValid
                    ) {
                        Text("Simpan Expense")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = uiState.expenseDate,
            onDateSelected = { date ->
                viewModel.updateDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}