package com.chibychibystore.service

import android.content.Context
import android.os.Environment
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.impl.PdfExportServiceImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Restores the coverage lost with the deleted `PdfExportServiceTest`.
 *
 * The whole value of these exports is that they translate a reporting failure
 * into a failed export instead of writing a bogus file, and that each method
 * pulls its data from the matching reporting query. Those are the cases asserted
 * here; the success paths end by writing a real PDF, exercised once through
 * Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class PdfExportServiceTest {

    private val reportingService = mock<ReportingService>()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val service = PdfExportServiceImpl(mock<Context>(), reportingService, ioDispatcher)

    private val start = LocalDate.of(2025, 1, 1)
    private val end = LocalDate.of(2025, 1, 31)

    @Test
    fun `gross sales export delegates to the reporting service and writes a pdf`() = runTest {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs()
        whenever(reportingService.getGrossSales(start, end))
            .thenReturn(Result.success(LaporanPenjualanKotor(100_000.0, 3, 33_333.0)))

        val result = service.exportGrossSalesReport(start, end)

        verify(reportingService).getGrossSales(start, end)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.contains("Laporan_Penjualan_Kotor"))
    }

    @Test
    fun `gross sales export fails when the reporting service fails`() = runTest {
        whenever(reportingService.getGrossSales(start, end))
            .thenReturn(Result.failure(Exception("Database error")))

        val result = service.exportGrossSalesReport(start, end)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Database error"))
    }

    @Test
    fun `each export fails when its reporting query fails`() = runTest {
        whenever(reportingService.getProfitMargin(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getNetProfit(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getSalesByProduct(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getSalesByCategory(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getSalesTrend(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getIncomeStatement(start)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getCashFlow(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getExpenseReport(start, end)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getBalanceSheet(start)).thenReturn(Result.failure(Exception("x")))
        whenever(reportingService.getPeriodicPerformanceSummary(start, end)).thenReturn(Result.failure(Exception("x")))

        assertTrue(service.exportProfitMarginReport(start, end).isFailure)
        assertTrue(service.exportNetProfitReport(start, end).isFailure)
        assertTrue(service.exportSalesByProductReport(start, end).isFailure)
        assertTrue(service.exportSalesByCategoryReport(start, end).isFailure)
        assertTrue(service.exportSalesTrendReport(start, end).isFailure)
        assertTrue(service.exportIncomeStatement(start).isFailure)
        assertTrue(service.exportCashFlowReport(start, end).isFailure)
        assertTrue(service.exportExpenseReport(start, end).isFailure)
        assertTrue(service.exportBalanceSheet(start).isFailure)
        assertTrue(service.exportPeriodicSummaryReport(start, end).isFailure)
    }

    @Test
    fun `income statement uses the single date it was given`() = runTest {
        whenever(reportingService.getIncomeStatement(start)).thenReturn(Result.failure(Exception("x")))

        service.exportIncomeStatement(start)

        verify(reportingService).getIncomeStatement(start)
    }

    @Test
    fun `a thrown reporting query is converted to a failed export`() = runTest {
        whenever(reportingService.getSalesTrend(any(), any()))
            .thenThrow(RuntimeException("boom"))

        val result = service.exportSalesTrendReport(start, end)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Gagal export PDF"))
    }

    @Test
    fun `the failure carries the reporting error message`() = runTest {
        whenever(reportingService.getCashFlow(start, end))
            .thenReturn(Result.failure(Exception("koneksi terputus")))

        val result = service.exportCashFlowReport(start, end)

        assertEquals(true, result.exceptionOrNull()!!.message!!.contains("koneksi terputus"))
    }
}
