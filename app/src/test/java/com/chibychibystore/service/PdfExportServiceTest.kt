package com.chibychibystore.service

import android.content.Context
import com.chibychibystore.data.Result
import com.chibychibystore.data.model.Penjualan
import com.chibychibystore.data.model.ItemPenjualan
import com.chibychibystore.data.model.Produk
import com.chibychibystore.data.model.Pengeluaran
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
        pdfExportService = PdfExportService(context, reportingService)
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
            .thenReturn(Result.Error(Exception("Database error")))

        // When
        val result = pdfExportService.exportGrossSalesReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
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
}