package com.chibychibystore.ui.reports

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.chibychibystore.service.PdfExportService
import com.chibychibystore.service.ReportingService
import com.chibychibystore.testutils.BaseIntegrationTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidTest
class ReportsViewModelIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var reportingService: ReportingService

    @Mock
    lateinit var pdfExportService: PdfExportService

    private lateinit var viewModel: ReportsViewModel

    override fun setupDatabase() {
        super.setupDatabase()
        MockitoAnnotations.openMocks(this)
        viewModel = ReportsViewModel(reportingService, pdfExportService)
    }

    @Test
    fun `loadReport should load gross sales report and update state`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()

        // When
        viewModel.updateDateRange(startDate, endDate)
        viewModel.loadReport(ReportType.GROSS_SALES)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.reportData)
            assertTrue(loadedState.reportData is GrossSalesReport)

            val report = loadedState.reportData as GrossSalesReport
            assertTrue(report.totalSales > 0)
            assertTrue(report.totalTransactions > 0)
            assertTrue(report.averageTransaction > 0)
        }
    }

    @Test
    fun `loadReport should load profit margin report with correct calculations`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()

        // When
        viewModel.updateDateRange(startDate, endDate)
        viewModel.loadReport(ReportType.PROFIT_MARGIN)

        // Then
        viewModel.uiState.test {
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.reportData)
            assertTrue(loadedState.reportData is ProfitMarginReport)

            val report = loadedState.reportData as ProfitMarginReport
            assertTrue(report.totalRevenue >= 0)
            assertTrue(report.totalCost >= 0)
            assertEquals(report.totalRevenue - report.totalCost, report.grossProfit)
            assertTrue(report.profitMargin >= 0)
        }
    }

    @Test
    fun `loadReport should load sales by product report with chart data`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()

        // When
        viewModel.updateDateRange(startDate, endDate)
        viewModel.loadReport(ReportType.SALES_BY_PRODUCT)

        // Then
        viewModel.uiState.test {
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.reportData)
            assertTrue(loadedState.reportData is List<*>)

            val productSales = loadedState.reportData as List<ProductSales>
            assertTrue(productSales.isNotEmpty())
            productSales.forEach { sale ->
                assertNotNull(sale.productName)
                assertTrue(sale.totalRevenue >= 0)
                assertTrue(sale.quantitySold >= 0)
            }
        }
    }

    @Test
    fun `loadReport should load sales trend report with time series data`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()

        // When
        viewModel.updateDateRange(startDate, endDate)
        viewModel.loadReport(ReportType.SALES_TREND)

        // Then
        viewModel.uiState.test {
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.reportData)
            assertTrue(loadedState.reportData is List<*>)

            val trendData = loadedState.reportData as List<TrendData>
            assertTrue(trendData.isNotEmpty())
            trendData.forEach { data ->
                assertNotNull(data.date)
                assertTrue(data.sales >= 0)
            }
        }
    }

    @Test
    fun `loadReport should handle empty date range gracefully`() = runTest {
        // Given - no test data, empty database

        // When
        viewModel.loadReport(ReportType.GROSS_SALES)

        // Then
        viewModel.uiState.test {
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.reportData)
            assertTrue(loadedState.reportData is GrossSalesReport)

            val report = loadedState.reportData as GrossSalesReport
            assertEquals(0.0, report.totalSales, 0.0)
            assertEquals(0, report.totalTransactions)
            assertEquals(0.0, report.averageTransaction, 0.0)
        }
    }

    @Test
    fun `exportCurrentReportToPdf should call correct service method for gross sales`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        viewModel.updateDateRange(startDate, endDate)

        `when`(pdfExportService.exportGrossSalesReport(startDate, endDate)).thenReturn(
            com.chibychibystore.data.Result.Success("test_file_path.pdf")
        )

        // When
        viewModel.exportCurrentReportToPdf()

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            // Should show exporting
            val exportingState = awaitItem()
            assertTrue(exportingState.isExporting)
            assertNull(exportingState.exportSuccess)

            // Should show success
            val successState = awaitItem()
            assertFalse(successState.isExporting)
            assertEquals("test_file_path.pdf", successState.exportSuccess)
        }

        verify(pdfExportService).exportGrossSalesReport(startDate, endDate)
    }

    @Test
    fun `exportCurrentReportToPdf should handle export error gracefully`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        viewModel.updateDateRange(startDate, endDate)

        `when`(pdfExportService.exportGrossSalesReport(startDate, endDate)).thenReturn(
            com.chibychibystore.data.Result.Error("Export failed")
        )

        // When
        viewModel.exportCurrentReportToPdf()

        // Then
        viewModel.uiState.test {
            skipItems(1)

            // Should show exporting
            val exportingState = awaitItem()
            assertTrue(exportingState.isExporting)

            // Should show error
            val errorState = awaitItem()
            assertFalse(errorState.isExporting)
            assertEquals("Gagal export PDF: Export failed", errorState.error)
        }
    }

    @Test
    fun `clearExportSuccess should clear export success state`() = runTest {
        // Given
        setupTestData()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        viewModel.updateDateRange(startDate, endDate)

        `when`(pdfExportService.exportGrossSalesReport(startDate, endDate)).thenReturn(
            com.chibychibystore.data.Result.Success("test_file_path.pdf")
        )

        // Export first
        viewModel.exportCurrentReportToPdf()

        // Wait for success state
        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // exporting
            awaitItem() // success
        }

        // When
        viewModel.clearExportSuccess()

        // Then
        viewModel.uiState.test {
            val currentState = awaitItem()
            assertNull(currentState.exportSuccess)
        }
    }

    @Test
    fun `updateDateRange should update state correctly`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()

        // When
        viewModel.updateDateRange(startDate, endDate)

        // Then
        viewModel.uiState.test {
            val updatedState = awaitItem()
            assertEquals(startDate, updatedState.startDate)
            assertEquals(endDate, updatedState.endDate)
        }
    }
}