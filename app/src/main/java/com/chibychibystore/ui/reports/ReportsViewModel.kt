package com.chibychibystore.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.service.ReportingService
import com.chibychibystore.service.PdfExportService
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the Reports screen
 */
data class ReportsUiState(
    val selectedReportType: ReportType = ReportType.GROSS_SALES,
    val startDate: LocalDate? = LocalDate.now().minusMonths(1),
    val endDate: LocalDate? = LocalDate.now(),
    val reportData: Any? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isExporting: Boolean = false,
    val exportSuccess: String? = null
)

/**
 * Reports ViewModel
 * Mengelola state dan logika untuk layar laporan
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportingService: ReportingService,
    private val pdfExportService: PdfExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    fun selectReportType(reportType: ReportType) {
        _uiState.update { it.copy(selectedReportType = reportType) }
        loadReport()
    }

    fun setStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
        loadReport()
    }

    fun setEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
        loadReport()
    }

    fun exportCurrentReportToPdf() {
        val currentState = _uiState.value
        val startDate = currentState.startDate ?: return
        val endDate = currentState.endDate ?: return

        _uiState.update {
            it.copy(
                isExporting = true,
                error = null,
                exportSuccess = null
            )
        }

        viewModelScope.launch {
            try {
                val result = when (currentState.selectedReportType) {
                    ReportType.GROSS_SALES -> pdfExportService.exportGrossSalesReport(startDate, endDate)
                    ReportType.PROFIT_MARGIN -> pdfExportService.exportProfitMarginReport(startDate, endDate)
                    ReportType.NET_PROFIT -> pdfExportService.exportNetProfitReport(startDate, endDate)
                    ReportType.SALES_BY_PRODUCT -> pdfExportService.exportSalesByProductReport(startDate, endDate)
                    ReportType.SALES_BY_CATEGORY -> pdfExportService.exportSalesByCategoryReport(startDate, endDate)
                    ReportType.SALES_TREND -> pdfExportService.exportSalesTrendReport(startDate, endDate)
                    ReportType.INCOME_STATEMENT -> pdfExportService.exportIncomeStatement(endDate)
                    ReportType.CASH_FLOW -> pdfExportService.exportCashFlowReport(startDate, endDate)
                    ReportType.EXPENSE_REPORT -> pdfExportService.exportExpenseReport(startDate, endDate)
                    ReportType.BALANCE_SHEET -> {
                        val asOfDate = currentState.endDate ?: LocalDate.now()
                        pdfExportService.exportBalanceSheet(asOfDate)
                    }
                    ReportType.STOCK_MOVEMENT -> {
                        // Stock movement PDF export not yet implemented in service
                        Result.failure(Exception("Export PDF untuk pergerakan stok belum tersedia"))
                    }
                }

                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                exportSuccess = result.data
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                error = "Gagal export PDF: ${result.exception.message}",
                                isExporting = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Gagal export PDF: ${e.message}",
                        isExporting = false
                    )
                }
            }
        }
    }

    fun clearExportSuccess() {
        _uiState.update { it.copy(exportSuccess = null) }
    }

    private fun loadReport() {
        // Capture state before launching coroutine if needed, or inside
        // Here we just need the dates
        val currentState = _uiState.value
        val startDate = currentState.startDate ?: return
        val endDate = currentState.endDate ?: return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                when (currentState.selectedReportType) {
                    ReportType.GROSS_SALES -> {
                        when (val res = reportingService.getGrossSales(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.PROFIT_MARGIN -> {
                        when (val res = reportingService.getProfitMargin(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.NET_PROFIT -> {
                        when (val res = reportingService.getNetProfit(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.SALES_BY_PRODUCT -> {
                        when (val res = reportingService.getSalesByProduct(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.SALES_BY_CATEGORY -> {
                        when (val res = reportingService.getSalesByCategory(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.SALES_TREND -> {
                        when (val res = reportingService.getSalesTrend(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.INCOME_STATEMENT -> {
                        when (val res = reportingService.getIncomeStatement(endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.CASH_FLOW -> {
                        when (val res = reportingService.getCashFlow(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.EXPENSE_REPORT -> {
                        when (val res = reportingService.getExpenseReport(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.BALANCE_SHEET -> {
                        when (val res = reportingService.getBalanceSheet(endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                    ReportType.STOCK_MOVEMENT -> {
                        when (val res = reportingService.getStockMovementReport(startDate, endDate)) {
                            is Result.Success -> _uiState.update { it.copy(reportData = res.data, isLoading = false) }
                            is Result.Failure -> _uiState.update { it.copy(error = res.exception.message, isLoading = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Gagal memuat laporan: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}