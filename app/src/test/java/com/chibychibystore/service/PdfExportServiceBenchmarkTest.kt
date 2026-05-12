package com.chibychibystore.service
import org.robolectric.annotation.Config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Benchmark test for PdfExportService to measure export duration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfExportServiceBenchmarkTest {

    private lateinit var context: Context

    @Mock
    private lateinit var reportingService: ReportingService

    private lateinit var pdfExportService: PdfExportService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        pdfExportService = PdfExportService(context, reportingService, UnconfinedTestDispatcher())
    }

    @Test
    fun `exportSalesTrendReport benchmark`() = runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        // Create mock data
        val mockData = (1..100).map { i ->
            DataTren(LocalDate.of(2025, 1, 1).plusDays(i.toLong()), 100000.0 + i * 1000, 10 + i)
        }

        `when`(reportingService.getSalesTrend(startDate, endDate)).thenReturn(Result.Success(mockData))

        val startTime = System.nanoTime()
        val result = pdfExportService.exportSalesTrendReport(startDate, endDate)
        val endTime = System.nanoTime()

        println("Export duration: ${(endTime - startTime) / 1_000_000} ms")

        assertTrue("Result should be success", result is Result.Success)
    }
}
