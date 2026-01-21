package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.ProdukTerpopulerDto
import com.chibychibystore.repository.*
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

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
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(false)
        val result = reportingService.getInventoryReport()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getInventoryReport should return data when permitted`() = runBlocking {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)
        whenever(produkRepository.getProdukCount()).thenReturn(Result.success(10))
        whenever(produkRepository.countLowStock()).thenReturn(Result.success(1))
        whenever(produkRepository.countOutOfStock()).thenReturn(Result.success(0))

        val result = reportingService.getInventoryReport()

        assertTrue(result.isSuccess)
        val report = (result as Result.Success).data
        assertEquals(10, report.totalProduk)
        assertEquals(1, report.stokRendahCount)
        assertEquals(0, report.stokHabisCount)
    }

    @Test
    fun `getTopSellingProducts should return hydrated data`() = runBlocking {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)

        val topDto = listOf(ProdukTerpopulerDto(1L, 50, 50000.0))
        whenever(itemPenjualanRepository.getTopSellingProduks(10)).thenReturn(Result.success(topDto))

        val product = Produk(
            id = 1L,
            name = "Test Product",
            sellingPrice = 1000.0,
            costPrice = 800.0,
            stockQuantity = 50,
            minStock = 5,
            barcode = "123",
            categoryId = 1L,
            warehouseId = 1L
        )
        whenever(produkRepository.getProductsByIds(listOf(1L))).thenReturn(Result.success(listOf(product)))

        val result = reportingService.getTopSellingProducts(10)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Test Product", list[0].namaProduk)
        assertEquals(50, list[0].jumlahTerjual)
    }
}
