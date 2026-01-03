package com.chibychibystore.ui.reports

import android.content.Intent
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
import com.chibychibystore.ui.components.shared.AppTopBar
// DatePickerDialog is provided by Material3
// Chart components are in the same package, so no need to import them
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reports_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.exportCurrentReportToPdf() },
                        enabled = !uiState.isExporting && uiState.reportData != null
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Export PDF")
                        }
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
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Report Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedReportType) {
                        ReportType.GROSS_SALES -> GrossSalesReportScreen(uiState.reportData as? GrossSalesReport)
                        ReportType.PROFIT_MARGIN -> ProfitMarginReportScreen(uiState.reportData as? ProfitMarginReport)
                        ReportType.NET_PROFIT -> NetProfitReportScreen(uiState.reportData as? NetProfitReport)
                        ReportType.SALES_BY_PRODUCT -> SalesByProductReportScreen(uiState.reportData as? List<ProductSales>)
                        ReportType.SALES_BY_CATEGORY -> SalesByCategoryReportScreen(uiState.reportData as? List<CategorySales>)
                        ReportType.SALES_TREND -> SalesTrendReportScreen(uiState.reportData as? List<TrendData>)
                        ReportType.INCOME_STATEMENT -> IncomeStatementReportScreen(uiState.reportData as? IncomeStatement)
                        ReportType.CASH_FLOW -> CashFlowReportScreen(uiState.reportData as? CashFlow)
                        ReportType.EXPENSE_REPORT -> ExpenseReportScreen(uiState.reportData as? ExpenseReport)
                        ReportType.BALANCE_SHEET -> BalanceSheetReportScreen(uiState.reportData as? BalanceSheet)
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
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reports_select_type)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
        OutlinedTextField(
            value = startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            onValueChange = {},
            label = { Text(stringResource(R.string.reports_start_date)) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            trailingIcon = {
                IconButton(onClick = { showStartDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select start date")
                }
            }
        )

        OutlinedTextField(
            value = endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            onValueChange = {},
            label = { Text(stringResource(R.string.reports_end_date)) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            trailingIcon = {
                IconButton(onClick = { showEndDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select end date")
                }
            }
        )
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
                    Text(stringResource(R.string.common_ok))
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
                    Text(stringResource(R.string.common_ok))
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
private fun GrossSalesReportScreen(data: GrossSalesReport?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Sale",
            value = "Rp ${"%,.0f".format(data.totalSales)}",
            subtitle = "${data.totalTransactions} transaksi"
        )
        MetricCard(
            title = "Rata-rata per Transaksi",
            value = "Rp ${"%,.0f".format(data.averageTransaction)}"
        )
    }
}

@Composable
private fun ProfitMarginReportScreen(data: ProfitMarginReport?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Pendapatan",
            value = "Rp ${"%,.0f".format(data.totalRevenue)}"
        )
        MetricCard(
            title = "Total Biaya",
            value = "Rp ${"%,.0f".format(data.totalCost)}"
        )
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.grossProfit)}"
        )
        MetricCard(
            title = "Margin Keuntungan",
            value = "${"%.1f".format(data.profitMargin)}%"
        )
    }
}

@Composable
private fun NetProfitReportScreen(data: NetProfitReport?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.grossProfit)}"
        )
        MetricCard(
            title = "Total Expense",
            value = "Rp ${"%,.0f".format(data.totalExpenses)}"
        )
        MetricCard(
            title = "Laba Bersih",
            value = "Rp ${"%,.0f".format(data.netProfit)}",
            subtitle = "${"%.1f".format(data.profitMargin)}% margin"
        )
    }
}

@Composable
private fun SalesByProductReportScreen(data: List<ProductSales>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.take(10).map { it.productName to it.totalRevenue.toFloat() }
            BarChart(
                data = chartData,
                title = "Sale per Product (Top 10)"
            )
        } else {
            Text("Tidak ada data penjualan product")
        }
    }
}

@Composable
private fun SalesByCategoryReportScreen(data: List<CategorySales>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.map { it.categoryName to it.totalRevenue.toFloat() }
            BarChart(
                data = chartData,
                title = "Sale per Category"
            )
        } else {
            Text("Tidak ada data penjualan kategori")
        }
    }
}

@Composable
private fun SalesTrendReportScreen(data: List<TrendData>?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        if (data.isNotEmpty()) {
            val chartData = data.map { it.date.toString() to it.sales.toFloat() }
            LineChart(
                data = chartData,
                title = "Trend Sale Harian"
            )
        } else {
            Text("Tidak ada data trend penjualan")
        }
    }
}

@Composable
private fun IncomeStatementReportScreen(data: IncomeStatement?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Pendapatan",
            value = "Rp ${"%,.0f".format(data.revenue)}"
        )
        MetricCard(
            title = "Harga Pokok Sale",
            value = "Rp ${"%,.0f".format(data.costOfGoodsSold)}"
        )
        MetricCard(
            title = "Laba Kotor",
            value = "Rp ${"%,.0f".format(data.grossProfit)}"
        )
        MetricCard(
            title = "Beban Operasional",
            value = "Rp ${"%,.0f".format(data.operatingExpenses)}"
        )
        MetricCard(
            title = "Laba Bersih",
            value = "Rp ${"%,.0f".format(data.netIncome)}"
        )
    }
}

@Composable
private fun CashFlowReportScreen(data: CashFlow?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Arus Kas Operasional",
            value = "Rp ${"%,.0f".format(data.operatingCashFlow)}"
        )
        MetricCard(
            title = "Arus Kas Investasi",
            value = "Rp ${"%,.0f".format(data.investingCashFlow)}"
        )
        MetricCard(
            title = "Arus Kas Pendanaan",
            value = "Rp ${"%,.0f".format(data.financingCashFlow)}"
        )
        MetricCard(
            title = "Arus Kas Bersih",
            value = "Rp ${"%,.0f".format(data.netCashFlow)}"
        )
        MetricCard(
            title = "Saldo Akhir",
            value = "Rp ${"%,.0f".format(data.endingCash)}"
        )
    }
}

@Composable
private fun ExpenseReportScreen(data: ExpenseReport?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Expense",
            value = "Rp ${"%,.0f".format(data.totalExpenses)}"
        )
        if (data.expensesByCategory.isNotEmpty()) {
            val chartData = data.expensesByCategory.map {
                val name = if (it.key is Enum<*>) (it.key as Enum<*>).name else it.key.toString()
                name to it.value.toFloat()
            }
            PieChart(
                data = chartData,
                title = "Expense per Category"
            )
        }
    }
}

@Composable
private fun BalanceSheetReportScreen(data: BalanceSheet?) {
    if (data == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        MetricCard(
            title = "Total Aset",
            value = "Rp ${"%,.0f".format(data.assets)}"
        )
        MetricCard(
            title = "Total Liabilitas",
            value = "Rp ${"%,.0f".format(data.liabilities)}"
        )
        MetricCard(
            title = "Ekuitas",
            value = "Rp ${"%,.0f".format(data.equity)}"
        )
        MetricCard(
            title = "Nilai Inventaris",
            value = "Rp ${"%,.0f".format(data.inventoryValue)}"
        )
    }
}
