package com.chibychibystore.service

import android.content.Context
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.PeriodicPerformance
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import java.time.LocalDate

class PdfExportServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var reportingService: ReportingService

    private lateinit var pdfExportService: PdfExportService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        pdfExportService = PdfExportService(context, reportingService, UnconfinedTestDispatcher())
    }

    @Test
    fun `exportGrossSalesReport should create PDF file successfully`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockGrossSalesReport()

        `when`(reportingService.getGrossSales(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // Mock file creation
        val mockFile = mock(File::class.java)
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.absolutePath).thenReturn("/test/path/report.pdf")

        // When
        val result = pdfExportService.exportGrossSalesReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Penjualan_Kotor"))
        assertTrue(filePath.contains("pdf"))
    }

    @Test
    fun `exportGrossSalesReport should return error when reporting service fails`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        `when`(reportingService.getGrossSales(startDate, endDate))
            .thenReturn(Result.failure(Exception("Database error")))

        // When
        val result = pdfExportService.exportGrossSalesReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertEquals("Database error", error.exception.message)
    }

    @Test
    fun `exportProfitMarginReport should handle profit calculations correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockProfitMarginReport()

        `when`(reportingService.getProfitMargin(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportProfitMarginReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Margin_Laba"))
    }

    @Test
    fun `exportNetProfitReport should include expense calculations`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockNetProfitReport()

        `when`(reportingService.getNetProfit(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportNetProfitReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Laba_Bersih"))
    }

    @Test
    fun `exportSalesByProductReport should format product data correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockSalesByProductReport()

        `when`(reportingService.getSalesByProduct(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportSalesByProductReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Penjualan_Produk"))
    }

    @Test
    fun `exportSalesTrendReport should handle date ranges correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockSalesTrendReport()

        `when`(reportingService.getSalesTrend(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportSalesTrendReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Tren_Penjualan"))
    }

    @Test
    fun `exportIncomeStatement should format financial data properly`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockIncomeStatement()

        `when`(reportingService.getIncomeStatement(date)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportIncomeStatement(date)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Laba_Rugi"))
    }

    @Test
    fun `exportExpenseReport should categorize expenses correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockExpenseReport()

        `when`(reportingService.getExpenseReport(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportExpenseReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Pengeluaran"))
    }

    @Test
    fun `exportSalesByCategoryReport should create category report PDF`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockReportData = listOf(
            mapOf("categoryName" to "Food", "quantity" to 100, "revenue" to 1000000.0)
        )

        `when`(reportingService.getSalesByCategory(startDate, endDate)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportSalesByCategoryReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Penjualan_Kategori"))
    }

    @Test
    fun `exportCashFlowReport should create cash flow PDF when data available`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val cashFlow = com.chibychibystore.service.CashFlow(
            operatingCashFlow = 1000.0,
            investingCashFlow = -200.0,
            financingCashFlow = 0.0,
            netCashFlow = 800.0,
            beginningCash = 0.0,
            endingCash = 800.0,
            period = "2025-01-01 - 2025-01-31"
        )

        `when`(reportingService.getCashFlow(startDate, endDate)).thenReturn(Result.Success(cashFlow))

        // When
        val result = pdfExportService.exportCashFlowReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Arus_Kas"))
    }

    @Test
    fun `exportBalanceSheet should calculate equity correctly`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 31)
        val mockReportData = createMockBalanceSheet()

        `when`(reportingService.getBalanceSheet(date)).thenReturn(Result.Success(mockReportData))

        // When
        val result = pdfExportService.exportBalanceSheet(date)

        // Then
        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Neraca"))
    }

    // Helper methods to create mock data
    private fun createMockGrossSalesReport(): Map<String, Any> {
        return mapOf(
            "totalSales" to 1500000.0,
            "totalTransactions" to 150,
            "averageTransaction" to 10000.0,
            "period" to "Januari 2025"
        )
    }

    private fun createMockProfitMarginReport(): Map<String, Any> {
        return mapOf(
            "revenue" to 1500000.0,
            "costOfGoodsSold" to 900000.0,
            "grossProfit" to 600000.0,
            "marginPercentage" to 40.0,
            "period" to "Januari 2025"
        )
    }

    private fun createMockNetProfitReport(): Map<String, Any> {
        return mapOf(
            "grossProfit" to 600000.0,
            "totalExpenses" to 300000.0,
            "netProfit" to 300000.0,
            "period" to "Januari 2025"
        )
    }

    private fun createMockSalesByProductReport(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "productName" to "Product A",
                "quantity" to 50,
                "revenue" to 500000.0
            ),
            mapOf(
                "productName" to "Product B",
                "quantity" to 30,
                "revenue" to 300000.0
            )
        )
    }

    private fun createMockSalesTrendReport(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "date" to LocalDate.of(2025, 1, 1),
                "sales" to 50000.0
            ),
            mapOf(
                "date" to LocalDate.of(2025, 1, 2),
                "sales" to 75000.0
            )
        )
    }

    private fun createMockIncomeStatement(): Map<String, Any> {
        return mapOf(
            "revenue" to 1500000.0,
            "costOfGoodsSold" to 900000.0,
            "grossProfit" to 600000.0,
            "operatingExpenses" to 300000.0,
            "netIncome" to 300000.0,
            "date" to LocalDate.of(2025, 1, 31)
        )
    }

    private fun createMockExpenseReport(): Map<String, Any> {
        return mapOf(
            "totalExpenses" to 300000.0,
            "expensesByCategory" to mapOf(
                "Utilitas" to 150000.0,
                "Supplies" to 100000.0,
                "Lainnya" to 50000.0
            ),
            "period" to "Januari 2025"
        )
    }

    private fun createMockBalanceSheet(): Map<String, Any> {
        return mapOf(
            "assets" to 2000000.0,
            "liabilities" to 500000.0,
            "equity" to 1500000.0,
            "inventoryValue" to 800000.0,
            "date" to LocalDate.of(2025, 1, 31)
        )
    }

    // ── exportPeriodicSummaryReport tests ────────────────────────────────────

    @Test
    fun `exportPeriodicSummaryReport should create PDF file successfully`() = runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockData = listOf(
            PeriodicPerformance(period = "2025-01-01", sales = 10000.0, netProfit = 1500.0, transactionCount = 3)
        )

        `when`(reportingService.getPeriodicPerformanceSummary(startDate, endDate))
            .thenReturn(Result.success(mockData))

        val result = pdfExportService.exportPeriodicSummaryReport(startDate, endDate)

        assertTrue(result is Result.Success)
        val filePath = (result as Result.Success).data
        assertTrue(filePath.contains("Laporan_Ringkasan_Performa"))
        assertTrue(filePath.contains("pdf"))
    }

    @Test
    fun `exportPeriodicSummaryReport should propagate failure when service returns error`() = runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        `when`(reportingService.getPeriodicPerformanceSummary(startDate, endDate))
            .thenReturn(Result.failure(Exception("Service error")))

        val result = pdfExportService.exportPeriodicSummaryReport(startDate, endDate)

        assertTrue(result is Result.Failure)
    }
}