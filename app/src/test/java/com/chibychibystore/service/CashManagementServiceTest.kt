package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.PenjualanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

@ExperimentalCoroutinesApi
class CashManagementServiceTest {

    @Mock
    private lateinit var saleRepository: PenjualanRepository

    @Mock
    private lateinit var expenseRepository: PengeluaranRepository

    private lateinit var cashManagementService: CashManagementService

    private val testSale = Penjualan(
        id = 1,
        saleDate = java.util.Date(),
        totalAmount = 100000.0,
        paymentMethod = "CASH",
        cashierId = 1,
        createdAt = java.util.Date()
    )

    private val testExpense = Pengeluaran(
        id = 1,
        expenseDate = java.util.Date(),
        category = ExpenseCategory.UTILITIES,
        amount = 50000.0,
        description = "Test expense",
        approvedBy = 1,
        createdBy = 1,
        createdAt = java.util.Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        cashManagementService = CashManagementService(saleRepository, expenseRepository)
    }

    @Test
    fun `calculateOperatingCashFlow should return correct calculation`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        val sales = listOf(testSale)
        val expenses = listOf(testExpense)

        `when`(saleRepository.getSalesInDateRange(startDate, endDate)).thenReturn(sales)
        `when`(expenseRepository.getExpensesInDateRange(startDate, endDate)).thenReturn(expenses)

        // When
        val result = cashManagementService.calculateOperatingCashFlow(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val cashFlow = (result as Result.Success).data
        // Operating cash flow = sales (100000) - operating expenses (50000) - inventory purchases (0)
        assertEquals(50000.0, cashFlow, 0.01)
    }

    @Test
    fun `calculateOperatingCashFlow should handle empty data`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()

        `when`(saleRepository.getSalesInDateRange(startDate, endDate)).thenReturn(emptyList())
        `when`(expenseRepository.getExpensesInDateRange(startDate, endDate)).thenReturn(emptyList())

        // When
        val result = cashManagementService.calculateOperatingCashFlow(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `getCashFlowSummary should return complete summary`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        val sales = listOf(testSale)
        val expenses = listOf(testExpense)

        `when`(saleRepository.getSalesInDateRange(startDate, endDate)).thenReturn(sales)
        `when`(expenseRepository.getExpensesInDateRange(startDate, endDate)).thenReturn(expenses)

        // When
        val result = cashManagementService.getCashFlowSummary(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val summary = (result as Result.Success).data
        assertEquals(50000.0, summary.operatingCashFlow, 0.01)
        assertEquals(0.0, summary.investingCashFlow, 0.01) // Not implemented yet
        assertEquals(0.0, summary.financingCashFlow, 0.01) // Not implemented yet
        assertEquals(50000.0, summary.netCashFlow, 0.01)
    }

    @Test
    fun `getCurrentCashPosition should return zero for now`() = runTest {
        // When
        val result = cashManagementService.getCurrentCashPosition()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 0.01)
    }
}