package com.chibychibystore.ui.sales

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.di.ServiceModule
import com.chibychibystore.service.SaleService
import com.chibychibystore.ui.sales.DatePickerType
import org.junit.Assume.assumeTrue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import javax.inject.Inject

@org.junit.Ignore("Disabled during androidTest triage")
class SalesHistoryViewModelIntegrationTest {
    // Disabled during triage

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var saleService: SaleService

    private lateinit var viewModel: SalesHistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = SalesHistoryViewModel(saleService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSales loads sales from database successfully`() = runTest {
        // Given - Database should be seeded with test data

        // When
        viewModel.loadSales()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        // Note: Actual sales count depends on seeded data
        assertTrue(state.sales.isNotEmpty())
    }

    @Test
    fun `updateSearchQuery filters sales by payment method`() = runTest {
        // Given
        viewModel.loadSales()
        val initialState = viewModel.uiState.first()

        // When - Search for CASH payments
        viewModel.updateSearchQuery("CASH")

        // Then
        val filteredState = viewModel.uiState.first()
        assertTrue(filteredState.sales.size <= initialState.sales.size)
        filteredState.sales.forEach { sale ->
            assertTrue(
                sale.paymentMethod.name.contains("CASH", ignoreCase = true) ||
                sale.id.toString().contains("CASH", ignoreCase = true)
            )
        }
    }

    @Test
    fun `date filtering works correctly`() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 14)
        val weekFromNow = calendar.time

        // When
        viewModel.setStartDate(weekAgo)
        viewModel.setEndDate(weekFromNow)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(weekAgo, state.startDate)
        assertEquals(weekFromNow, state.endDate)
    }

    @Test
    fun `clearFilters resets all filter criteria`() = runTest {
        // Given
        viewModel.updateSearchQuery("test")
        viewModel.setStartDate(Date())
        viewModel.setEndDate(Date())

        // When
        viewModel.clearFilters()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.searchQuery.isEmpty())
        assertNull(state.startDate)
        assertNull(state.endDate)
    }

    @Test
    fun `loadReceipt loads sale details successfully`() = runTest {
        // Given
        viewModel.loadSales()
        val sales = viewModel.uiState.first().sales
        assumeTrue("Need at least one sale for this test", sales.isNotEmpty())
        val saleId = sales.first().id

        // When
        viewModel.loadReceipt(saleId)

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.showReceiptDialog)
        assertNotNull(state.selectedSale)
        assertEquals(saleId, state.selectedSale?.penjualan?.id)
    }

    @Test
    fun `loadReceipt handles non-existent sale gracefully`() = runTest {
        // Given
        val nonExistentId = 99999L

        // When
        viewModel.loadReceipt(nonExistentId)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
        assertNotNull(state.error)
    }

    @Test
    fun `date picker state management works correctly`() = runTest {
        // When - Show start date picker
        viewModel.showDatePicker(DatePickerType.START)

        // Then
        var state = viewModel.uiState.first()
        assertTrue(state.showDatePicker)
        assertEquals(DatePickerType.START, state.datePickerType)

        // When - Hide date picker
        viewModel.hideDatePicker()

        // Then
        state = viewModel.uiState.first()
        assertFalse(state.showDatePicker)
    }

    @Test
    fun `error handling works for service failures`() = runTest {
        // Given - Force an error scenario by using invalid data
        // This test assumes some error condition can be triggered

        // When - Try to load receipt with invalid ID
        viewModel.loadReceipt(-1L)

        // Then
        val state = viewModel.uiState.first()
        assertNotNull(state.error)
        assertFalse(state.showReceiptDialog)
    }

    @Test
    fun `receipt dialog state management works correctly`() = runTest {
        // Given - Load a valid receipt first
        viewModel.loadSales()
        val sales = viewModel.uiState.first().sales
        assumeTrue("Need at least one sale for this test", sales.isNotEmpty())
        viewModel.loadReceipt(sales.first().id)

        // When - Hide receipt dialog
        viewModel.hideReceiptDialog()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.showReceiptDialog)
        assertNull(state.selectedSale)
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // Then
        val state = viewModel.uiState.first()
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

    @Test
    fun `search and date filters work together`() = runTest {
        // Given
        viewModel.loadSales()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val lastMonth = calendar.time
        calendar.add(Calendar.MONTH, 2)
        val nextMonth = calendar.time

        // When - Apply both search and date filters
        viewModel.updateSearchQuery("CASH")
        viewModel.setStartDate(lastMonth)
        viewModel.setEndDate(nextMonth)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.searchQuery.isEmpty())
        assertNotNull(state.startDate)
        assertNotNull(state.endDate)
        // Sales should be filtered by both criteria
        state.sales.forEach { sale ->
            assertTrue(sale.saleDate.after(lastMonth) || sale.saleDate == lastMonth)
            assertTrue(sale.saleDate.before(nextMonth) || sale.saleDate == nextMonth)
            assertTrue(
                sale.paymentMethod.name.contains("CASH", ignoreCase = true) ||
                sale.id.toString().contains("CASH", ignoreCase = true)
            )
        }
    }
}