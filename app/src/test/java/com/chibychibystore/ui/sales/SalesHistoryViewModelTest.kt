package com.chibychibystore.ui.sales

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.service.SaleService
import com.chibychibystore.ui.sales.SalesHistoryViewModel.DatePickerType
import com.chibychibystore.ui.sales.SalesHistoryViewModel.SalesHistoryUiState
import com.chibychibystore.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

@ExperimentalCoroutinesApi
class SalesHistoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var saleService: SaleService

    private lateinit var viewModel: SalesHistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SalesHistoryViewModel(saleService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSales success updates state with sales list`() = runTest {
        // Given
        val sales = listOf(
            createTestSale(1L, 100000.0),
            createTestSale(2L, 50000.0)
        )
        `when`(saleService.observeSales()).thenReturn(flowOf(sales))

        // When
        viewModel.loadSales()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(sales, state.sales)
    }

    @Test
    fun `loadSales failure updates state with error`() = runTest {
        // Given
        val errorMessage = "Database error"
        `when`(saleService.observeSales()).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.loadSales()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
        assertTrue(state.sales.isEmpty())
    }

    @Test
    fun `updateSearchQuery filters sales correctly`() = runTest {
        // Given
        val sales = listOf(
            createTestSale(1L, 100000.0), // Will match "CASH"
            createTestSale(2L, 50000.0)   // Will match "CARD"
        )
        `when`(saleService.observeSales()).thenReturn(flowOf(sales))
        viewModel.loadSales()

        // When
        viewModel.updateSearchQuery("CASH")

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.sales.size)
        assertEquals(PaymentMethod.CASH, state.sales[0].paymentMethod)
    }

    @Test
    fun `setStartDate updates date filter correctly`() = runTest {
        // Given
        val startDate = Date()

        // When
        viewModel.setStartDate(startDate)

        // Then
        val state = viewModel.uiState.value
        assertEquals(startDate, state.startDate)
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `setEndDate updates date filter correctly`() = runTest {
        // Given
        val endDate = Date()

        // When
        viewModel.setEndDate(endDate)

        // Then
        val state = viewModel.uiState.value
        assertEquals(endDate, state.endDate)
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `showDatePicker updates state correctly`() = runTest {
        // When
        viewModel.showDatePicker(DatePickerType.START)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showDatePicker)
        assertEquals(DatePickerType.START, state.datePickerType)
    }

    @Test
    fun `hideDatePicker updates state correctly`() = runTest {
        // Given
        viewModel.showDatePicker(DatePickerType.END)

        // When
        viewModel.hideDatePicker()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `clearFilters resets all filters`() = runTest {
        // Given
        val sales = listOf(createTestSale(1L, 100000.0))
        `when`(saleService.observeSales()).thenReturn(flowOf(sales))
        viewModel.loadSales()
        viewModel.updateSearchQuery("test")
        viewModel.setStartDate(Date())
        viewModel.setEndDate(Date())

        // When
        viewModel.clearFilters()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.searchQuery.isEmpty())
        assertNull(state.startDate)
        assertNull(state.endDate)
    }

    @Test
    fun `loadReceipt success updates state with receipt data`() = runTest {
        // Given
        val saleId = 1L
        val saleWithItems = createTestSaleWithItems(saleId)
        `when`(saleService.getSale(saleId)).thenReturn(Result.Success(saleWithItems))

        // When
        viewModel.loadReceipt(saleId)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showReceiptDialog)
        assertEquals(saleWithItems, state.selectedSale)
    }

    @Test
    fun `loadReceipt failure updates state with error`() = runTest {
        // Given
        val saleId = 1L
        val errorMessage = "Sale not found"
        `when`(saleService.getSale(saleId)).thenReturn(Result.Error(errorMessage))

        // When
        viewModel.loadReceipt(saleId)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `hideReceiptDialog updates state correctly`() = runTest {
        // Given
        val saleWithItems = createTestSaleWithItems(1L)
        `when`(saleService.getSale(1L)).thenReturn(Result.Success(saleWithItems))
        viewModel.loadReceipt(1L)

        // When
        viewModel.hideReceiptDialog()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        // Given
        viewModel.loadSales() // This will trigger error if service throws
        `when`(saleService.observeSales()).thenThrow(RuntimeException("Test error"))
        viewModel.loadSales()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.sales.isEmpty())
        assertTrue(state.searchQuery.isEmpty())
        assertNull(state.startDate)
        assertNull(state.endDate)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.showDatePicker)
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
    }

    private fun createTestSale(id: Long, totalAmount: Double): Penjualan {
        return Penjualan(
            id = id,
            saleDate = Date(),
            totalAmount = totalAmount,
            paymentMethod = if (id % 2 == 0L) PaymentMethod.CARD else PaymentMethod.CASH,
            cashierId = 1
        )
    }

    private fun createTestSaleWithItems(saleId: Long): PenjualanWithItems {
        val sale = createTestSale(saleId, 100000.0)
        return PenjualanWithItems(
            penjualan = sale,
            items = emptyList() // Simplified for testing
        )
    }
}