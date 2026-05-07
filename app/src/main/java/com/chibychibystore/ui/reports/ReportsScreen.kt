package com.chibychibystore.ui.reports

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.R
import com.chibychibystore.service.*
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyInput

import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.Error
import com.chibychibystore.ui.theme.Success
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Reports Screen
 * Menampilkan berbagai laporan bisnis dengan filter dan visualisasi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle export success
    LaunchedEffect(uiState.exportSuccess) {
        uiState.exportSuccess?.let { filePath ->
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Buka PDF"))
            }
            viewModel.clearExportSuccess()
        }
    }

    ChibyScaffold(
        title = stringResource(R.string.reports_title),
        onNavigateUp = onNavigateBack,
        actions = {
            IconButton(
                onClick = { viewModel.exportCurrentReportToPdf() },
                enabled = !uiState.isExporting && uiState.reportData != null
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Report Type Selector
            ReportTypeSelector(
                selectedType = uiState.selectedReportType,
                onTypeSelected = viewModel::selectReportType
            )

            // Date Range Filter
            DateRangeFilter(
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onStartDateSelected = viewModel::setStartDate,
                onEndDateSelected = viewModel::setEndDate
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChibyPinkPrimary)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Report Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedReportType) {
                        ReportType.GROSS_SALES -> GrossSalesReportScreen(uiState.reportData as? LaporanPenjualanKotor)
                        ReportType.PROFIT_MARGIN -> ProfitMarginReportScreen(uiState.reportData as? LaporanMarginLaba)
                        ReportType.NET_PROFIT -> NetProfitReportScreen(uiState.reportData as? LaporanLabaBersih)
                        ReportType.SALES_BY_PRODUCT -> SalesByProductReportScreen(uiState.reportData as? List<PenjualanProduk>)
                        ReportType.SALES_BY_CATEGORY -> SalesByCategoryReportScreen(uiState.reportData as? List<PenjualanKategori>)
                        ReportType.SALES_TREND -> SalesTrendReportScreen(uiState.reportData as? List<DataTren>)
                        ReportType.INCOME_STATEMENT -> IncomeStatementReportScreen(uiState.reportData as? LaporanLabaRugi)
                        ReportType.CASH_FLOW -> CashFlowReportScreen(uiState.reportData as? ArusKas)
                        ReportType.EXPENSE_REPORT -> ExpenseReportScreen(uiState.reportData as? LaporanPengeluaran)
                        ReportType.BALANCE_SHEET -> BalanceSheetReportScreen(uiState.reportData as? NeracaSaldo)
                        ReportType.STOCK_MOVEMENT -> StockMovementReportScreen(uiState.reportData as? List<StockMovement>)
                        ReportType.PERIODIC_SUMMARY -> PeriodicSummaryReportScreen(uiState.reportData as? List<PeriodicPerformance>)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTypeSelector(
    selectedType: ReportType,
    onTypeSelected: (ReportType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ChibyInput(
            value = selectedType.displayName,
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.reports_select_type),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },

        ) {
            ReportType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ChibyInput(
                value = startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                onValueChange = {},
                label = stringResource(R.string.reports_start_date),
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select start date", tint = ChibyPinkPrimary)
                    }
                }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
             ChibyInput(
                value = endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                onValueChange = {},
                label = stringResource(R.string.reports_end_date),
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select end date", tint = ChibyPinkPrimary)
                    }
                }
            )
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.toEpochDay()?.times(24 * 60 * 60 * 1000)
        )

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onStartDateSelected(selectedDate)
                    }
                    showStartDatePicker = false
                }) {
                    Text(stringResource(R.string.common_ok), color = ChibyPinkPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.toEpochDay()?.times(24 * 60 * 60 * 1000)
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onEndDateSelected(selectedDate)
                    }
                    showEndDatePicker = false
                }) {
                    Text(stringResource(R.string.common_ok), color = ChibyPinkPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Report display components

@Composable
private fun GrossSalesReportScreen(data: LaporanPenjualanKotor?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Penjualan",
            value = "Rp ${"%,.0f".format(data.totalPenjualan)}",
            subtitle = "${data.totalTransaksi} transaksi"
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Rata-rata per Transaksi",
            value = "Rp ${"%,.0f".format(data.rataRataTransaksi)}"
        )
    }
}

@Composable
private fun StockMovementReportScreen(data: List<StockMovement>?) {
    if (data == null) return
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (data.isEmpty()) {
            item {
                Text("Tidak ada pergerakan stok dalam periode ini", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(data) { movement ->
                ChibyCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movement.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${movement.date} • ${movement.type} • ${movement.warehouseName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Ref: ${movement.referenceId}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = if (movement.quantity > 0) "+${movement.quantity}" else movement.quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (movement.quantity > 0) Success else Error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfitMarginReportScreen(data: LaporanMarginLaba?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Pendapatan",
            value = "Rp ${"%,.0f".format(data.totalPendapatan)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Total Biaya",
            value = "Rp ${"%,.0f".format(data.totalBiaya)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.labaKotor)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Margin Keuntungan",
            value = "${"%.1f".format(data.marginLaba)}%"
        )
    }
}

@Composable
private fun NetProfitReportScreen(data: LaporanLabaBersih?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.labaKotor)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Total Pengeluaran",
            value = "Rp ${"%,.0f".format(data.totalPengeluaran)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Laba Bersih",
            value = "Rp ${"%,.0f".format(data.labaBersih)}",
            subtitle = "${"%.1f".format(data.marginLaba)}% margin"
        )
    }
}

@Composable
private fun SalesByProductReportScreen(data: List<PenjualanProduk>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.take(10).map { it.namaProduk to it.totalPendapatan.toFloat() }
            BarChart(
                data = chartData,
                title = "Penjualan per Produk (Top 10)"
            )
        } else {
            Text("Tidak ada data penjualan produk")
        }
    }
}

@Composable
private fun SalesByCategoryReportScreen(data: List<PenjualanKategori>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.map { it.namaKategori to it.totalPendapatan.toFloat() }
            BarChart(
                data = chartData,
                title = "Penjualan per Kategori"
            )
        } else {
            Text("Tidak ada data penjualan kategori")
        }
    }
}

@Composable
private fun SalesTrendReportScreen(data: List<DataTren>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.map { it.tanggal.toString() to it.penjualan.toFloat() }
            LineChart(
                data = chartData,
                title = "Trend Penjualan Harian"
            )
        } else {
            Text("Tidak ada data trend penjualan")
        }
    }
}

@Composable
private fun IncomeStatementReportScreen(data: LaporanLabaRugi?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Pendapatan",
            value = "Rp ${"%,.0f".format(data.pendapatan)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Harga Pokok Penjualan",
            value = "Rp ${"%,.0f".format(data.hargaPokokPenjualan)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.labaKotor)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Beban Operasional",
            value = "Rp ${"%,.0f".format(data.bebanOperasional)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Laba Bersih",
            value = "Rp ${"%,.0f".format(data.labaBersih)}"
        )
    }
}

@Composable
private fun CashFlowReportScreen(data: ArusKas?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Arus Kas Operasional",
            value = "Rp ${"%,.0f".format(data.arusKasOperasional)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Arus Kas Investasi",
            value = "Rp ${"%,.0f".format(data.arusKasInvestasi)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Arus Kas Pendanaan",
            value = "Rp ${"%,.0f".format(data.arusKasPendanaan)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Arus Kas Bersih",
            value = "Rp ${"%,.0f".format(data.arusKasBersih)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Saldo Akhir",
            value = "Rp ${"%,.0f".format(data.saldoAkhir)}"
        )
    }
}

@Composable
private fun ExpenseReportScreen(data: LaporanPengeluaran?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Pengeluaran",
            value = "Rp ${"%,.0f".format(data.totalPengeluaran)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        if (data.pengeluaranPerKategori.isNotEmpty()) {
            val chartData = data.pengeluaranPerKategori.map {
                val name = if (it.key is Enum<*>) (it.key as Enum<*>).name else it.key.toString()
                name to it.value.toFloat()
            }
            PieChart(
                data = chartData,
                title = "Pengeluaran per Kategori"
            )
        }
    }
}

@Composable
private fun PeriodicSummaryReportScreen(data: List<PeriodicPerformance>?) {
    if (data == null) return
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (data.isEmpty()) {
            item {
                Text("Tidak ada data untuk periode ini", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(data) { perf ->
                ChibyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(perf.period, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Penjualan", style = MaterialTheme.typography.bodySmall)
                                Text("Rp ${"%,.0f".format(perf.sales)}", fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Profit", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Rp ${"%,.0f".format(perf.netProfit)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (perf.netProfit >= 0) Success else Error
                                )
                            }
                        }
                        Text("${perf.transactionCount} transaksi", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSheetReportScreen(data: NeracaSaldo?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Aset",
            value = "Rp ${"%,.0f".format(data.aset)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Total Liabilitas",
            value = "Rp ${"%,.0f".format(data.liabilitas)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Ekuitas",
            value = "Rp ${"%,.0f".format(data.ekuitas)}"
        )
         Spacer(modifier = Modifier.height(8.dp))
        MetricCard(
            title = "Nilai Inventaris",
            value = "Rp ${"%,.0f".format(data.nilaiPersediaan)}"
        )
    }
}

