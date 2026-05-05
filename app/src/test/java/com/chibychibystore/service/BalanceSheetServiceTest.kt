package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.impl.BalanceSheetServiceImpl
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
import javax.inject.Provider

@ExperimentalCoroutinesApi
class BalanceSheetServiceTest {

    @Mock
    private lateinit var productRepository: ProdukRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    @Mock
    private lateinit var cashManagementService: CashManagementService

    private lateinit var balanceSheetService: BalanceSheetService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        balanceSheetService = BalanceSheetServiceImpl(
            productRepository,
            pengeluaranRepository,
            Provider { cashManagementService }
        )
    }

    @Test
    fun `calculateInventoryValue should return correct value`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.success(100000.0))

        val result = balanceSheetService.calculateInventoryValue()

        assertTrue(result is Result.Success)
        assertEquals(100000.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `calculateTotalAssets should return correct total`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.success(100000.0))
        whenever(cashManagementService.getCurrentCashPosition())
            .thenReturn(Result.success(50000.0))

        val result = balanceSheetService.calculateTotalAssets(LocalDate.now())

        assertTrue(result is Result.Success)
        // Assets = cash (50000) + inventory value (100000) = 150000
        assertEquals(150000.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `calculateTotalAssets should propagate failure from inventory`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.failure(Exception("DB error")))
        whenever(cashManagementService.getCurrentCashPosition())
            .thenReturn(Result.success(50000.0))

        val result = balanceSheetService.calculateTotalAssets(LocalDate.now())

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `calculateTotalLiabilities should return value from repository`() = runTest {
        whenever(pengeluaranRepository.getUnapprovedPengeluaranTotalBeforeDate(any<Date>()))
            .thenReturn(0.0)

        val result = balanceSheetService.calculateTotalLiabilities(LocalDate.now())

        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `calculateEquity should return assets minus liabilities`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.success(100000.0))
        whenever(cashManagementService.getCurrentCashPosition())
            .thenReturn(Result.success(50000.0))
        whenever(pengeluaranRepository.getUnapprovedPengeluaranTotalBeforeDate(any<Date>()))
            .thenReturn(0.0)

        val result = balanceSheetService.calculateEquity(LocalDate.now())

        assertTrue(result is Result.Success)
        // Equity = Assets (150000) - Liabilities (0) = 150000
        assertEquals(150000.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `generateBalanceSheet should return complete balance sheet`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.success(100000.0))
        whenever(cashManagementService.getCurrentCashPosition())
            .thenReturn(Result.success(50000.0))
        whenever(pengeluaranRepository.getUnapprovedPengeluaranTotalBeforeDate(any<Date>()))
            .thenReturn(0.0)

        val result = balanceSheetService.generateBalanceSheet(LocalDate.now())

        assertTrue(result is Result.Success)
        val balanceSheet = (result as Result.Success).data
        assertEquals(150000.0, balanceSheet.assets, 0.01)
        assertEquals(0.0, balanceSheet.liabilities, 0.01)
        assertEquals(150000.0, balanceSheet.equity, 0.01)
        assertEquals(100000.0, balanceSheet.inventoryValue, 0.01)
        assertEquals(50000.0, balanceSheet.cashBalance, 0.01)
    }

    @Test
    fun `generateBalanceSheet should propagate failure`() = runTest {
        whenever(productRepository.getTotalInventoryValue())
            .thenReturn(Result.failure(Exception("DB error")))
        whenever(cashManagementService.getCurrentCashPosition())
            .thenReturn(Result.success(50000.0))

        val result = balanceSheetService.generateBalanceSheet(LocalDate.now())

        assertTrue(result is Result.Failure)
    }
}
