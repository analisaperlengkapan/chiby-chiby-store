@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.sales
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.ui.components.shared.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    navController: NavController,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Riwayat Penjualan",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { viewModel.clearFilters() }) {
                        Icon(Icons.Default.Clear, "Clear Filters")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Bar
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Cari berdasarkan ID, metode pembayaran..."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showDatePicker(DatePickerType.START) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                uiState.startDate?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                } ?: "Tanggal Mulai"
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.showDatePicker(DatePickerType.END) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                uiState.endDate?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                } ?: "Tanggal Akhir"
                            )
                        }
                    }
                }
            }

            // Sales List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicator()
                    }

                    uiState.error != null -> {
                        ErrorMessage(
                            message = uiState.error ?: "", 
                            onRetry = { viewModel.loadSales() },
                            onDismiss = { viewModel.clearError() }
                        )
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.sales,
                                key = { it.id }
                            ) { sale ->
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
private fun SaleItem(
    sale: com.chibychibystore.data.local.entity.Penjualan,
    onClick: () -> Unit,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Penjualan #${sale.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    currencyFormat.format(sale.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                dateFormat.format(sale.saleDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Metode: ${sale.paymentMethod.name}",
                    style = MaterialTheme.typography.bodySmall
                )

                Icon(
                    Icons.Default.Receipt,
                    contentDescription = "Lihat Struk",
                    tint = MaterialTheme.colorScheme.primary
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
        title = {
            Text(
                "Struk Penjualan #${saleWithItems.penjualan.id}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text("Tanggal: ${dateFormat.format(saleWithItems.penjualan.saleDate)}")
                Text("Metode Pembayaran: ${saleWithItems.penjualan.paymentMethod.name}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Items:", fontWeight = FontWeight.Bold)

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(saleWithItems.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x Item #${item.productId}")
                            Text(currencyFormat.format(item.totalPrice))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text(
                        currencyFormat.format(saleWithItems.penjualan.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrintReceipt,
                    enabled = !isPrinting
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(if (isPrinting) "Mencetak..." else "Cetak Struk")
                }

                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
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
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("Oke")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}