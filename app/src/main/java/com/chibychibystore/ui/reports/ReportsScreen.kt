package com.chibychibystore.ui.reports
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.model.*
import com.chibychibystore.service.*
import com.chibychibystore.ui.components.special.*
import com.chibychibystore.ui.components.shared.*
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Reports Screen with PDF Export
 * Displays various business reports with filtering and visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle PDF export success
    LaunchedEffect(uiState.exportSuccess) {
        uiState.exportSuccess?.let { filePath ->
            viewModel.clearExportSuccess()
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
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Buka PDF"))
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Laporan",
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = { navController.popBackStack() },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportCurrentReportToPdf() },
                        enabled = !uiState.isExporting && uiState.reportData != null
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, "Export PDF")
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
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    ReportTypeSelector(
                        selectedType = uiState.selectedReportType,
                        onTypeSelected = { viewModel.selectReportType(it) }
                    )
                }

                item {
                    DateRangeFilter(
                        startDate = uiState.startDate,
                        endDate = uiState.endDate,
                        onStartDateChange = { viewModel.setStartDate(it) },
                        onEndDateChange = { viewModel.setEndDate(it) }
                    )
                }

                item {
                    when (uiState.selectedReportType) {
                        ReportType.GROSS_SALES -> GrossSalesReport(uiState.reportData)
                        ReportType.PROFIT_MARGIN -> ProfitMarginReport(uiState.reportData)
                        ReportType.NET_PROFIT -> NetProfitReport(uiState.reportData)
                        ReportType.SALES_BY_PRODUCT -> SalesByProductReport(uiState.reportData)
                        ReportType.SALES_BY_CATEGORY -> SalesByCategoryReport(uiState.reportData)
                        ReportType.SALES_TREND -> SalesTrendReport(uiState.reportData)
                        ReportType.INCOME_STATEMENT -> IncomeStatementReport(uiState.reportData)
                        ReportType.CASH_FLOW -> CashFlowReport(uiState.reportData)
                        ReportType.EXPENSE_REPORT -> ExpenseReport(uiState.reportData)
                        ReportType.BALANCE_SHEET -> BalanceSheetReport(uiState.reportData)
                    }
                }
            }

            if (uiState.isLoading) {
                LoadingIndicator()
            }

            uiState.error?.let { error ->
                ErrorMessage(message = error)
            }
        }
    }
}

@Composable
private fun ReportTypeSelector(
    selectedType: ReportType,
    onTypeSelected: (ReportType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Jenis Laporan",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ReportType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    Text(
                        text = type.displayName,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeFilter(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Periode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                Text("${startDate?.format(formatter) ?: "-"} s/d ${endDate?.format(formatter) ?: "-"}")
            }
        }
    }
}

@Composable
private fun GrossSalesReport(data: Any?) {
    when (data) {
        is com.chibychibystore.service.GrossSalesReport -> {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricCard(
                    title = "Total Penjualan",
                    value = "Rp ${"%,.0f".format(data.totalSales)}"
                )
                MetricCard(
                    title = "Rata-rata Transaksi",
                    value = "Rp ${"%,.0f".format(data.averageTransaction)}"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun ProfitMarginReport(data: Any?) {
    when (data) {
        is com.chibychibystore.service.ProfitMarginReport -> {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricCard(
                    title = "Pendapatan",
                    value = "Rp ${"%,.0f".format(data.totalRevenue)}"
                )
                MetricCard(
                    title = "HPP",
                    value = "Rp ${"%,.0f".format(data.totalCost)}"
                )
                MetricCard(
                    title = "Laba Kotor",
                    value = "Rp ${"%,.0f".format(data.grossProfit)}"
                )
                MetricCard(
                    title = "Margin",
                    value = "${"%.2f".format(data.profitMargin)}%"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun NetProfitReport(data: Any?) {
    when (data) {
        is com.chibychibystore.service.NetProfitReport -> {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricCard(
                    title = "Laba Kotor",
                    value = "Rp ${"%,.0f".format(data.grossProfit)}"
                )
                MetricCard(
                    title = "Total Biaya",
                    value = "Rp ${"%,.0f".format(data.totalExpenses)}"
                )
                MetricCard(
                    title = "Laba Bersih",
                    value = "Rp ${"%,.0f".format(data.netProfit)}"
                )
                MetricCard(
                    title = "Margin Bersih",
                    value = "${"%.2f".format(data.profitMargin)}%"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun SalesByProductReport(data: Any?) {
    when (data) {
        is List<*> -> {
            if (data.isNotEmpty() && data.first() is ProductSales) {
                val productSales = data as List<ProductSales>
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top 10 Produk",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    BarChart(
                        title = "Top 10 Produk",
                        data = productSales.take(10).map { it.productName to it.totalRevenue.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            } else {
                EmptyReportMessage()
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun SalesByCategoryReport(data: Any?) {
    when (data) {
        is List<*> -> {
            if (data.isNotEmpty() && data.first() is CategorySales) {
                val categorySales = data as List<CategorySales>
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Penjualan per Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    BarChart(
                        title = "Penjualan per Kategori",
                        data = categorySales.map { it.categoryName to it.totalRevenue.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            } else {
                EmptyReportMessage()
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun SalesTrendReport(data: Any?) {
    when (data) {
        is List<*> -> {
            if (data.isNotEmpty() && data.first() is TrendData) {
                val trendData = data as List<TrendData>
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trend Penjualan Harian",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LineChart(
                        title = "Trend Penjualan Harian",
                        data = trendData.map { it.date.toString() to it.sales.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            } else {
                EmptyReportMessage()
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun IncomeStatementReport(data: Any?) {
    when (data) {
        is IncomeStatement -> {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricCard(
                    title = "Pendapatan",
                    value = "Rp ${"%,.0f".format(data.revenue)}"
                )
                MetricCard(
                    title = "HPP",
                    value = "Rp ${"%,.0f".format(data.costOfGoodsSold)}"
                )
                MetricCard(
                    title = "Laba Kotor",
                    value = "Rp ${"%,.0f".format(data.grossProfit)}"
                )
                MetricCard(
                    title = "Biaya Operasional",
                    value = "Rp ${"%,.0f".format(data.operatingExpenses)}"
                )
                MetricCard(
                    title = "Laba Bersih",
                    value = "Rp ${"%,.0f".format(data.netIncome)}"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun CashFlowReport(data: Any?) {
    when (data) {
        is CashFlow -> {
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
                    title = "Arus Kas Finansial",
                    value = "Rp ${"%,.0f".format(data.financingCashFlow)}"
                )
                MetricCard(
                    title = "Arus Kas Bersih",
                    value = "Rp ${"%,.0f".format(data.netCashFlow)}"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun ExpenseReport(data: Any?) {
    when (data) {
        is com.chibychibystore.service.ExpenseReport -> {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricCard(
                    title = "Total Pengeluaran",
                    value = "Rp ${"%,.0f".format(data.totalExpenses)}"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pengeluaran per Kategori",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PieChart(
                    title = "Distribusi Pengeluaran",
                    data = data.expensesByCategory.entries.map { it.key.displayName to it.value.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun BalanceSheetReport(data: Any?) {
    when (data) {
        is BalanceSheet -> {
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
                    title = "Nilai Persediaan",
                    value = "Rp ${"%,.0f".format(data.inventoryValue)}"
                )
            }
        }
        else -> EmptyReportMessage()
    }
}

@Composable
private fun EmptyReportMessage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Memuat data laporan...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
