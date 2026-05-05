package com.chibychibystore.service

import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ShiftRepository
import com.chibychibystore.service.impl.CashManagementServiceImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.Date

@ExperimentalCoroutinesApi
class CashManagementServiceTest {

    @Mock
    private lateinit var db: ChibyChibyDatabase

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    @Mock
    private lateinit var shiftRepository: ShiftRepository

    @Mock
    private lateinit var authService: AuthService

    private lateinit var cashManagementService: CashManagementService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        cashManagementService = CashManagementServiceImpl(
            db,
            penjualanRepository,
            pengeluaranRepository,
            shiftRepository,
            authService
        )
    }

    @Test
    fun `calculateOperatingCashFlow should return correct calculation`() = runTest {
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        val approvedExpenseMap = mapOf(KategoriPengeluaran.UTILITIES to 50000.0)

        whenever(penjualanRepository.getTotalRevenue(startDate, endDate))
            .thenReturn(Result.success(100000.0))
        whenever(pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(
            any<Date>(), any<Date>()
        )).thenReturn(Result.success(approvedExpenseMap))

        val result = cashManagementService.calculateOperatingCashFlow(startDate, endDate)

        assertTrue(result is Result.Success)
        val cashFlow = (result as Result.Success).data
        // Revenue (100000) - UTILITIES operating expense (50000) - COGS (0)
        assertEquals(50000.0, cashFlow, 0.01)
    }

    @Test
    fun `calculateOperatingCashFlow should handle empty data`() = runTest {
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()

        whenever(penjualanRepository.getTotalRevenue(startDate, endDate))
            .thenReturn(Result.success(0.0))
        whenever(pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(
            any<Date>(), any<Date>()
        )).thenReturn(Result.success(emptyMap()))

        val result = cashManagementService.calculateOperatingCashFlow(startDate, endDate)

        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `calculateFinancingCashFlow should return correct calculation`() = runTest {
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        val approvedExpenseMap = mapOf(KategoriPengeluaran.LOAN_REPAYMENT to 25000.0)

        whenever(pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(
            any<Date>(), any<Date>()
        )).thenReturn(Result.success(approvedExpenseMap))

        val result = cashManagementService.calculateFinancingCashFlow(startDate, endDate)

        assertTrue(result is Result.Success)
        // Financing Cash Flow should be negative of financing expenses
        assertEquals(-25000.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `getCashFlowSummary should return complete summary`() = runTest {
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        val approvedExpenseMap = mapOf(
            KategoriPengeluaran.UTILITIES to 50000.0,
            KategoriPengeluaran.EQUIPMENT to 20000.0,
            KategoriPengeluaran.LOAN_REPAYMENT to 10000.0
        )

        whenever(penjualanRepository.getTotalRevenue(startDate, endDate))
            .thenReturn(Result.success(100000.0))
        whenever(pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(
            any<Date>(), any<Date>()
        )).thenReturn(Result.success(approvedExpenseMap))

        val result = cashManagementService.getCashFlowSummary(startDate, endDate)

        assertTrue(result is Result.Success)
        val summary = (result as Result.Success).data
        assertEquals(50000.0, summary.operatingCashFlow, 0.01)
        assertEquals(-20000.0, summary.investingCashFlow, 0.01)
        assertEquals(-10000.0, summary.financingCashFlow, 0.01)
        // Net = 50000 - 20000 - 10000 = 20000
        assertEquals(20000.0, summary.netCashFlow, 0.01)
    }

    @Test
    fun `calculateNetCashFlow should propagate failure from sub-calculations`() = runTest {
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()

        whenever(penjualanRepository.getTotalRevenue(startDate, endDate))
            .thenReturn(Result.failure(Exception("DB error")))

        val result = cashManagementService.calculateNetCashFlow(startDate, endDate)

        assertTrue(result is Result.Failure)
    }
}
