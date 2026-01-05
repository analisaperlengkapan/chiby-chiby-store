@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.ui.components.*
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesHistoryScreen(
    navController: NavController,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    ChibyScaffold(
        title = "Riwayat Penjualan",
        onNavigateUp = { navController.popBackStack() },
        actions = {
            IconButton(onClick = { viewModel.clearFilters() }) {
                Icon(Icons.Default.Clear, "Clear Filters", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters Section
            ChibyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 2
            ) {
                Column {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChibyPinkPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Bar
                    ChibyInput(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = "Cari Transaksi",
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = ChibyPinkPrimary) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

// Date Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChibyOutlinedButton(
                            text = uiState.startDate?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                            } ?: "Tanggal Mulai",
                            onClick = { viewModel.showDatePicker(DatePickerType.START) },
                            modifier = Modifier.weight(1f)
                        )

                        ChibyOutlinedButton(
                            text = uiState.endDate?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                            } ?: "Tanggal Akhir",
                            onClick = { viewModel.showDatePicker(DatePickerType.END) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sales List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingIndicator("Memuat riwayat penjualan...")

                    uiState.error != null -> {
                       ChibyCard(
                           modifier = Modifier.padding(16.dp),
                           containerColor = MaterialTheme.colorScheme.errorContainer
                       ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { viewModel.loadSales() }) { Text("Coba Lagi") }
                            }
                       }
                    }

                    uiState.sales.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Receipt,
                            title = "Tidak ada data penjualan",
                            message = "Belum ada transaksi penjualan yang tercatat"
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = uiState.sales, key = { it.id }) { sale ->
                                SaleItem(
                                    sale = sale,
                                    onClick = { viewModel.loadReceipt(sale.id) },
                                    dateFormat = dateFormat,
                                    currencyFormat = currencyFormat
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt Dialog
    uiState.selectedSale?.let { saleWithItems ->
        if (uiState.showReceiptDialog) {
            ReceiptDialog(
                saleWithItems = saleWithItems,
                isPrinting = uiState.isPrintingReceipt,
                onPrintReceipt = { viewModel.printReceipt() },
                onDismiss = { viewModel.hideReceiptDialog() },
                dateFormat = dateFormat,
                currencyFormat = currencyFormat
            )
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val initialDate = when (uiState.datePickerType) {
            DatePickerType.START -> uiState.startDate ?: Date()
            DatePickerType.END -> uiState.endDate ?: Date()
        }

        AppDatePickerDialog(
            onDateSelected = { date ->
                when (uiState.datePickerType) {
                    DatePickerType.START -> viewModel.setStartDate(date)
                    DatePickerType.END -> viewModel.setEndDate(date)
                }
                viewModel.hideDatePicker()
            },
            onDismiss = { viewModel.hideDatePicker() },
            initialDate = initialDate
        )
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SaleItem(
    sale: com.chibychibystore.data.local.entity.Penjualan,
    onClick: () -> Unit,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    ChibyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Penjualan #${sale.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateFormat.format(sale.saleDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                currencyFormat.format(sale.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ChibyPinkPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(modifier = Modifier.width(4.dp))
                 Text(
                    "${sale.paymentMethod.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lihat Struk", style = MaterialTheme.typography.labelMedium, color = ChibyPinkPrimary)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ChibyPinkPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ReceiptDialog(
    saleWithItems: com.chibychibystore.data.local.entity.PenjualanWithItems,
    isPrinting: Boolean,
    onPrintReceipt: () -> Unit,
    onDismiss: () -> Unit,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Struk Penjualan #${saleWithItems.penjualan.id}") },
        text = {
            Column {
                Text("Tanggal: ${dateFormat.format(saleWithItems.penjualan.saleDate)}")
                Text("Metode: ${saleWithItems.penjualan.paymentMethod.name}")
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(saleWithItems.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x Item", style = MaterialTheme.typography.bodySmall)
                            Text(currencyFormat.format(item.totalPrice), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text(
                        currencyFormat.format(saleWithItems.penjualan.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = ChibyPinkPrimary
                    )
                }
            }
        },
        confirmButton = {
            ChibyButton(
                text = if (isPrinting) "Mencetak..." else "Cetak Struk",
                onClick = onPrintReceipt,
                isLoading = isPrinting,
                enabled = !isPrinting
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    initialDate: Date
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.time)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Date(millis))
                }
            }) { Text("Oke", color = ChibyPinkPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}