package com.chibychibystore.service

import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.impl.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Date

class SecurityPermissionTest {

    @Mock lateinit var authService: AuthService
    @Mock lateinit var productRepository: ProdukRepository
    @Mock lateinit var saleRepository: PenjualanRepository
    @Mock lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock lateinit var printerService: PrinterService
    @Mock lateinit var warehouseRepository: GudangRepository
    @Mock lateinit var pengeluaranRepository: PengeluaranRepository
    @Mock lateinit var pembelianRepository: PembelianRepository
    @Mock lateinit var balanceSheetService: BalanceSheetService
    @Mock lateinit var cashManagementService: CashManagementService
    @Mock lateinit var stokGudangRepository: StokGudangRepository
    @Mock lateinit var shiftRepository: com.chibychibystore.repository.ShiftRepository
    @Mock lateinit var promoService: PromoService
    @Mock lateinit var db: com.chibychibystore.data.local.database.ChibyChibyDatabase

    private lateinit var productService: ProductService
    private lateinit var saleService: SaleService
    private lateinit var warehouseService: WarehouseService
    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        productService = ProductServiceImpl(productRepository, stokGudangRepository, authService)
        saleService = SaleServiceImpl(
            db,
            saleRepository,
            itemPenjualanRepository,
            productRepository,
            stokGudangRepository,
            shiftRepository,
            authService,
            printerService,
            promoService
        )
        warehouseService = WarehouseServiceImpl(warehouseRepository, productRepository, stokGudangRepository, authService, db)
        reportingService = ReportingServiceImpl(
            saleRepository,
            itemPenjualanRepository,
            productRepository,
            pengeluaranRepository,
            pembelianRepository,
            balanceSheetService,
            cashManagementService,
            authService,
            db
        )
    }

    @Test
    fun `productService createProduct should fail without EDIT_INVENTORY permission`() = runBlocking<Unit> {
        whenever(authService.hasPermission("EDIT_INVENTORY")).thenReturn(false)
        
        val product = Produk(name = "Test", barcode = "123", categoryId = 1L, costPrice = 10.0, sellingPrice = 20.0, stockQuantity = 10, warehouseId = 1L)
        val result = productService.createProduk(product)
        
        assertTrue("Expected failure for EDIT_INVENTORY", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("izin") == true)
    }

    @Test
    fun `saleService createSale should fail without CREATE_SALES permission`() = runBlocking<Unit> {
        whenever(authService.hasPermission("CREATE_SALES")).thenReturn(false)
        
        val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        val result = saleService.createPenjualan(sale, emptyList())
        
        assertTrue("Expected failure for CREATE_SALES", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("izin") == true)
    }

    @Test
    fun `warehouseService createWarehouse should fail without MANAGE_WAREHOUSES permission`() = runBlocking<Unit> {
        whenever(authService.hasPermission("MANAGE_WAREHOUSES")).thenReturn(false)
        
        val warehouse = Gudang(name = "Test", location = "Loc", capacity = 100)
        val result = warehouseService.createGudang(warehouse)
        
        assertTrue("Expected failure for MANAGE_WAREHOUSES", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("izin") == true)
    }

    @Test
    fun `reportingService daily report should fail without VIEW_SALES_REPORTS permission`() = runBlocking<Unit> {
        whenever(authService.hasPermission("VIEW_SALES_REPORTS")).thenReturn(false)
        
        val result = reportingService.getDailySalesReport(LocalDate.now())
        
        assertTrue("Expected failure for VIEW_SALES_REPORTS", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("izin") == true)
    }
}
