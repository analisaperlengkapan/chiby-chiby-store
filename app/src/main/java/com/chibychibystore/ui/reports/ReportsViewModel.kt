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
        _uiState.value = _uiState.value.copy(selectedReportType = reportType)
        loadReport()
    }

    fun setStartDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = date)
        loadReport()
    }

    fun setEndDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(endDate = date)
        loadReport()
    }

    fun exportCurrentReportToPdf() {
        val currentState = _uiState.value
        val startDate = currentState.startDate ?: return
        val endDate = currentState.endDate ?: return

        _uiState.value = currentState.copy(
            isExporting = true,
            error = null,
            exportSuccess = null
        )

        viewModelScope.launch {
            try {
                val result = when (currentState.selectedReportType) {
                    ReportType.GROSS_SALES -> pdfExportService.exportGrossSalesReport(startDate, endDate)
                    ReportType.PROFIT_MARGIN -> pdfExportService.exportProfitMarginReport(startDate, endDate)
                    ReportType.NET_PROFIT -> pdfExportService.exportNetProfitReport(startDate, endDate)
                    ReportType.SALES_BY_PRODUCT -> pdfExportService.exportSalesByProductReport(startDate, endDate)
                    ReportType.SALES_BY_CATEGORY -> pdfExportService.exportSalesByCategoryReport(startDate, endDate)
                    ReportType.SALES_TREND -> pdfExportService.exportSalesTrendReport(startDate, endDate)
                    ReportType.INCOME_STATEMENT -> pdfExportService.exportIncomeStatement(startDate, endDate)
                    ReportType.CASH_FLOW -> pdfExportService.exportCashFlowReport(startDate, endDate)
                    ReportType.EXPENSE_REPORT -> pdfExportService.exportExpenseReport(startDate, endDate)
                    ReportType.BALANCE_SHEET -> {
                        val asOfDate = currentState.endDate ?: LocalDate.now()
                        pdfExportService.exportBalanceSheet(asOfDate)
                    }
                }

                when (result) {
                    is Result.Success -> {
                        _uiState.value = currentState.copy(
                            isExporting = false,
                            exportSuccess = result.data
                        )
                    }
                    is Result.Failure -> {
                        _uiState.value = currentState.copy(
                            error = "Gagal export PDF: ${result.exception.message}",
                            isExporting = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = "Gagal export PDF: ${e.message}",
                    isExporting = false
                )
            }
        }
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = null)
    }

    private fun loadReport() {
        val currentState = _uiState.value
        val startDate = currentState.startDate ?: return
        val endDate = currentState.endDate ?: return

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val reportData = when (currentState.selectedReportType) {
                    ReportType.GROSS_SALES -> reportingService.getGrossSales(startDate, endDate)
                    ReportType.PROFIT_MARGIN -> reportingService.getProfitMargin(startDate, endDate)
                    ReportType.NET_PROFIT -> reportingService.getNetProfit(startDate, endDate)
                    ReportType.SALES_BY_PRODUCT -> reportingService.getSalesByProduct(startDate, endDate)
                    ReportType.SALES_BY_CATEGORY -> reportingService.getSalesByCategory(startDate, endDate)
                    ReportType.SALES_TREND -> reportingService.getSalesTrend(startDate, endDate)
                    ReportType.INCOME_STATEMENT -> reportingService.getIncomeStatement(startDate, endDate)
                    ReportType.CASH_FLOW -> reportingService.getCashFlow(startDate, endDate)
                    ReportType.EXPENSE_REPORT -> reportingService.getExpenseReport(startDate, endDate)
                    ReportType.BALANCE_SHEET -> reportingService.getBalanceSheet(endDate)
                }

                _uiState.value = currentState.copy(
                    reportData = reportData,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = "Gagal memuat laporan: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}