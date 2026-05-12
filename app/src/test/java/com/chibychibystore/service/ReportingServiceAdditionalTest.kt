package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import com.chibychibystore.service.impl.ReportingServiceImpl

class ReportingServiceAdditionalTest {

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var pembelianRepository: PembelianRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    @Mock
    private lateinit var balanceSheetService: BalanceSheetService

    @Mock
    private lateinit var cashManagementService: CashManagementService

    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        reportingService = ReportingServiceImpl(
            penjualanRepository,
            com.chibychibystore.repository.ItemPenjualanRepository(Mockito.mock(com.chibychibystore.data.local.dao.ItemPenjualanDao::class.java)),
            produkRepository,
            pengeluaranRepository,
            pembelianRepository,
            balanceSheetService,
            cashManagementService,
            Mockito.mock(AuthService::class.java).apply {
                runBlocking {
                    Mockito.`when`(hasPermission(Mockito.anyString())).thenReturn(true)
                }
            }
        )
    }

    @Test
    fun `getGrossSales should return failure when repository throws exception`() = runTest {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)

        Mockito.`when`(penjualanRepository.getSalesInDateRange(start, end)).thenThrow(RuntimeException("DB down"))

        val result = reportingService.getGrossSales(start, end)

        assertTrue(result.isFailure)
        // Implementation returns a generic failure message; assert it failed due to underlying exception
        assertTrue(result.exceptionOrNull()?.message?.contains("getGrossSales failed") == true)
    }

    @Test
    fun `getCashFlow should return success when cash summary available`() = runTest {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)

        val summary = CashManagementService.CashFlowSummary(
            operatingCashFlow = 1000.0,
            investingCashFlow = -200.0,
            financingCashFlow = 0.0,
            netCashFlow = 800.0,
            period = "2025-01-01 - 2025-01-31"
        )

        Mockito.`when`(cashManagementService.getCashFlowSummary(start, end)).thenReturn(com.chibychibystore.data.model.Result.success(summary))

        val result = reportingService.getCashFlow(start, end)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals(1000.0, data?.arusKasOperasional)
        assertEquals(-200.0, data?.arusKasInvestasi)
        assertEquals(0.0, data?.arusKasPendanaan)
        assertEquals(800.0, data?.arusKasBersih)
    }
}
