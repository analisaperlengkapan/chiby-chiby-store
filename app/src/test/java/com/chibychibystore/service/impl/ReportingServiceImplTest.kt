package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.TopProductDto
import com.chibychibystore.repository.*
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.InventoryReport
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ReportingServiceImplTest {

    @Mock private lateinit var penjualanRepository: PenjualanRepository
    @Mock private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock private lateinit var produkRepository: ProdukRepository
    @Mock private lateinit var pengeluaranRepository: PengeluaranRepository
    @Mock private lateinit var pembelianRepository: PembelianRepository
    @Mock private lateinit var balanceSheetService: BalanceSheetService
    @Mock private lateinit var cashManagementService: CashManagementService
    @Mock private lateinit var authService: AuthService

    private lateinit var reportingService: ReportingServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        reportingService = ReportingServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            pengeluaranRepository,
            pembelianRepository,
            balanceSheetService,
            cashManagementService,
            authService
        )
    }

    @Test
    fun `getInventoryReport should return failure when no permission`() = runBlocking {
        `when`(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(false)
        val result = reportingService.getInventoryReport()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getInventoryReport should return data when permitted`() = runBlocking {
        `when`(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)
        `when`(produkRepository.getProdukCount()).thenReturn(Result.success(10))
        `when`(produkRepository.countLowStock()).thenReturn(Result.success(1))
        `when`(produkRepository.countOutOfStock()).thenReturn(Result.success(0))

        val result = reportingService.getInventoryReport()

        assertTrue(result.isSuccess)
        val report = (result as Result.Success).data
        assertEquals(10, report.totalProducts)
        assertEquals(1, report.lowStockCount)
        assertEquals(0, report.outOfStockCount)
    }

    @Test
    fun `getTopSellingProducts should return hydrated data`() = runBlocking {
        `when`(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)

        val topDto = listOf(TopProductDto(1L, 50, 50000.0))
        `when`(itemPenjualanRepository.getTopSellingProducts(10)).thenReturn(Result.success(topDto))

        val product = Produk(1L, "Test Product", 1000.0, 100, 10, "123", null, null, null, java.util.Date())
        `when`(produkRepository.getProdukByIds(listOf(1L))).thenReturn(Result.success(listOf(product)))

        val result = reportingService.getTopSellingProducts(10)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Test Product", list[0].productName)
        assertEquals(50, list[0].quantitySold)
    }
}
