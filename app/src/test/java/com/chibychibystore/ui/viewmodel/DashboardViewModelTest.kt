package com.chibychibystore.ui.viewmodel

import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.SaleService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @org.junit.Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @org.junit.After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Mock
    private lateinit var saleService: SaleService

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadDashboardData success updates all metrics`() = runTest {
        val now = Date()
        val sale1 = Penjualan(id = 1, saleDate = now, totalAmount = 100000.0, paymentMethod = PaymentMethod.CASH, cashierId = 1)
        val sale2 = Penjualan(id = 2, saleDate = now, totalAmount = 50000.0, paymentMethod = PaymentMethod.CASH, cashierId = 1)

        Mockito.`when`(saleService.getSales(Mockito.anyString(), Mockito.anyString(), Mockito.isNull())).thenReturn(com.chibychibystore.data.model.Result.success(listOf(sale1, sale2)))
        Mockito.`when`(saleService.getSales(Mockito.isNull(), Mockito.isNull(), Mockito.isNull())).thenReturn(com.chibychibystore.data.model.Result.success(listOf(sale1, sale2)))

        val lowStock = listOf(Produk(id = 1, name = "Item A", barcode = "123", sellingPrice = 10000.0, costPrice = 8000.0, stockQuantity = 2, categoryId = 1, warehouseId = 1))
        Mockito.`when`(produkRepository.getLowStockProduk()).thenReturn(flowOf(lowStock))

        viewModel = DashboardViewModel(saleService, produkRepository)

        // Refresh to ensure data loaded
        viewModel.refreshData()

        // Allow coroutines to complete
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(150000.0, state.todaySales, 0.001)
        assertEquals(2, state.todayTransactionCount)
        assertEquals(1, state.lowStockItems.size)
        assertEquals(2, state.recentTransactions.size)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadDashboardData handles service exception`() = runTest {
        Mockito.`when`(saleService.getSales(Mockito.anyString(), Mockito.anyString(), Mockito.isNull())).thenThrow(RuntimeException("service failure"))
        Mockito.`when`(produkRepository.getLowStockProduk()).thenReturn(flowOf(emptyList()))

        viewModel = DashboardViewModel(saleService, produkRepository)
        viewModel.refreshData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
}