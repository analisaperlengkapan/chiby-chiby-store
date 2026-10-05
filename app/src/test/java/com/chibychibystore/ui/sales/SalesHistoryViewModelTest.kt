package com.chibychibystore.ui.sales

import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.SaleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SalesHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var saleService: SaleService

    @Before
    fun setup() = runBlocking<Unit> {
        Dispatchers.setMain(testDispatcher)
        saleService = mock()
        whenever(saleService.getPenjualanByRentangTanggal(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(emptyList()))
        whenever(saleService.observePenjualanFiltered(any(), any(), anyOrNull())).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSales publishes the queried range`() = runTest {
        val sales = listOf(createTestSale(1L, 100_000.0), createTestSale(2L, 50_000.0))
        whenever(saleService.getPenjualanByRentangTanggal(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(sales))
        whenever(saleService.observePenjualanFiltered(any(), any(), anyOrNull())).thenReturn(flowOf(sales))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(sales, state.sales)
    }

    @Test
    fun `loadSales failure records the error`() = runTest {
        whenever(saleService.getPenjualanByRentangTanggal(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(Exception("Database error")))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Database error", state.error)
        assertTrue(state.sales.isEmpty())
    }

    @Test
    fun `updateSearchQuery stores the query and reloads`() = runTest {
        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateSearchQuery("CASH")
        advanceUntilIdle()

        assertEquals("CASH", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `setStartDate and setEndDate store the range`() = runTest {
        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val start = Date(1_000_000L)
        val end = Date(2_000_000L)
        viewModel.setStartDate(start)
        viewModel.setEndDate(end)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(start, state.startDate)
        assertEquals(end, state.endDate)
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `date picker visibility toggles with its type`() = runTest {
        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.showDatePicker(DatePickerType.START)
        assertTrue(viewModel.uiState.value.showDatePicker)
        assertEquals(DatePickerType.START, viewModel.uiState.value.datePickerType)

        viewModel.hideDatePicker()
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `clearFilters resets the search and range`() = runTest {
        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateSearchQuery("test")
        viewModel.setStartDate(Date())
        viewModel.setEndDate(Date())
        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.searchQuery.isEmpty())
        assertNull(state.startDate)
        assertNull(state.endDate)
    }

    @Test
    fun `loadReceipt opens the dialog with the sale`() = runTest {
        val saleWithItems = createTestSaleWithItems(1L)
        whenever(saleService.getPenjualanById(1L)).thenReturn(Result.success(saleWithItems))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.loadReceipt(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showReceiptDialog)
        assertEquals(saleWithItems, state.selectedSale)
    }

    @Test
    fun `loadReceipt failure records the error and keeps the dialog closed`() = runTest {
        whenever(saleService.getPenjualanById(1L)).thenReturn(Result.failure(Exception("Sale not found")))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.loadReceipt(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
        assertEquals("Sale not found", state.error)
    }

    @Test
    fun `hideReceiptDialog clears the selected sale`() = runTest {
        val saleWithItems = createTestSaleWithItems(1L)
        whenever(saleService.getPenjualanById(1L)).thenReturn(Result.success(saleWithItems))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.loadReceipt(1L)
        advanceUntilIdle()
        viewModel.hideReceiptDialog()

        val state = viewModel.uiState.value
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
    }

    @Test
    fun `clearError removes the recorded error`() = runTest {
        whenever(saleService.getPenjualanByRentangTanggal(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(Exception("Test error")))

        val viewModel = SalesHistoryViewModel(saleService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    private fun createTestSale(id: Long, totalAmount: Double): Penjualan = Penjualan(
        id = id,
        saleDate = Date(),
        totalAmount = totalAmount,
        paymentMethod = if (id % 2 == 0L) PaymentMethod.CARD else PaymentMethod.CASH,
        cashierId = 1
    )

    private fun createTestSaleWithItems(saleId: Long): PenjualanWithItems =
        PenjualanWithItems(sale = createTestSale(saleId, 100_000.0), items = emptyList())
}
