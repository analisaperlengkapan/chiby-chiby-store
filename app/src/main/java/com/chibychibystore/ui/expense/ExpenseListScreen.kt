package com.chibychibystore.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.ui.components.shared.*
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran

import com.chibychibystore.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    navController: NavController,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manajemen Pengeluaran",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ExpenseAdd.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Pengeluaran")
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
            // Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Range Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.startDate?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                            } ?: "",
                            onValueChange = { },
                            label = { Text("Tanggal Mulai") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDateRangePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = uiState.endDate?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                            } ?: "",
                            onValueChange = { },
                            label = { Text("Tanggal Akhir") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDateRangePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedCategory?.displayName ?: "Semua Kategori",
                            onValueChange = { },
                            label = { Text("Kategori") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Semua Kategori") },
                                onClick = {
                                    viewModel.setCategoryFilter(null)
                                    expanded = false
                                }
                            )
                            ExpenseCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        viewModel.setCategoryFilter(category)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Content
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null -> {
                    ErrorMessage(message = uiState.error!!)
                }
                uiState.expenses.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Belum ada pengeluaran",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tambah pengeluaran pertama Anda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(Screen.ExpenseAdd.route) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Pengeluaran")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.expenses) { expense ->
                            ExpenseItem(
                                expense = expense,
                                onClick = {
                                    navController.navigate(Screen.ExpenseDetail.createRoute(expense.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Pengeluaran,
    onClick: () -> Unit
) {
    CardItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = expense.description ?: "Tidak ada deskripsi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.expenseDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        DateRangePickerDialog(
            initialStartDate = uiState.startDate,
            initialEndDate = uiState.endDate,
            onDateRangeSelected = { startDate, endDate ->
                viewModel.setStartDate(startDate)
                viewModel.setEndDate(endDate)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount)
}