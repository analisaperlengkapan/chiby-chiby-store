package com.chibychibystore.ui.viewmodel

import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import java.time.LocalDate
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.ReportingService
import com.chibychibystore.ui.dashboard.DashboardViewModel
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
import org.mockito.kotlin.*
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

    @Mock
    private lateinit var reportingService: ReportingService

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

        whenever(saleService.getTotalPenjualanByRentangTanggal(any(), any())).thenReturn(com.chibychibystore.data.model.Result.success(150000.0))
        whenever(saleService.getPenjualanCountByRentangTanggal(any(), any())).thenReturn(com.chibychibystore.data.model.Result.success(2))
        whenever(saleService.getRecentPenjualan(any())).thenReturn(com.chibychibystore.data.model.Result.success(listOf(sale1, sale2)))
        whenever(reportingService.getSalesTrend(any(), any())).thenReturn(com.chibychibystore.data.model.Result.success(emptyList()))
        whenever(reportingService.observeSalesMetrics()).thenReturn(flowOf(com.chibychibystore.service.MetrikPenjualan(150000.0, 2, 0.0, 0.0)))
        whenever(produkRepository.getLowStockProduk()).thenReturn(flowOf(listOf(Produk(name = "Low Stock", categoryId = 1, warehouseId = 1, costPrice = 1000.0, sellingPrice = 2000.0))))

        viewModel = DashboardViewModel(saleService, reportingService, produkRepository)

        // Refresh to ensure data loaded
        viewModel.refresh()

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
        whenever(saleService.getTotalPenjualanByRentangTanggal(any(), any())).thenThrow(RuntimeException("service failure"))
        whenever(reportingService.observeSalesMetrics()).thenReturn(flowOf(com.chibychibystore.service.MetrikPenjualan(0.0, 0, 0.0, 0.0)))
        whenever(produkRepository.getLowStockProduk()).thenReturn(flowOf(emptyList()))

        viewModel = DashboardViewModel(saleService, reportingService, produkRepository)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
}