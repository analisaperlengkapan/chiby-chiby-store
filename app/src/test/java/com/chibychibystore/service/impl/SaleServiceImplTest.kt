package com.chibychibystore.service.impl

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.repository.ShiftRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.PromoService
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class SaleServiceImplTest {

    private lateinit var db: ChibyChibyDatabase
    @Mock private lateinit var penjualanRepository: PenjualanRepository
    @Mock private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock private lateinit var productRepository: ProdukRepository
    @Mock private lateinit var stokGudangRepository: StokGudangRepository
    @Mock private lateinit var shiftRepository: ShiftRepository
    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var printerService: PrinterService
    @Mock private lateinit var promoService: PromoService

    private lateinit var saleService: SaleServiceImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // Create an in-memory database to allow 'withTransaction' to work
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        saleService = SaleServiceImpl(
            db,
            penjualanRepository,
            itemPenjualanRepository,
            productRepository,
            stokGudangRepository,
            shiftRepository,
            authService,
            printerService,
            promoService
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createPenjualan should fail when stock is insufficient`() = runTest {
        // Arrange
        val sale = Penjualan(
            saleDate = Date(),
            totalAmount = 10000.0,
            tax = 0.0,
            discount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = 1L,
            warehouseId = 1L
        )
        val items = listOf(
            ItemPenjualan(saleId = 0, productId = 1L, quantity = 10, unitPrice = 1000.0, totalPrice = 10000.0)
        )

        val product = Produk(
            id = 1L,
            name = "Test Product",
            sellingPrice = 1000.0,
            costPrice = 800.0,
            stockQuantity = 5, // Less than requested 10 (Legacy)
            minStock = 5,
            barcode = "123",
            categoryId = 1L,
            warehouseId = 1L,
            createdAt = Date()
        )

        val stokGudang = StokGudang(
            productId = 1L,
            warehouseId = 1L,
            quantity = 5 // Less than requested 10
        )

        whenever(authService.hasPermission(any())).thenReturn(true)
        whenever(productRepository.getProductsByIds(any())).thenReturn(Result.success(listOf(product)))
        whenever(stokGudangRepository.getStocks(any(), any())).thenReturn(Result.success(listOf(stokGudang)))
        whenever(promoService.calculateDiscount(any())).thenReturn(0.0)

        // Act
        val result = saleService.createPenjualan(sale, items)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Stok tidak mencukupi untuk Test Product di Gudang 1. Tersedia: 5, Dibutuhkan: 10", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPenjualan should succeed when stock is sufficient`() = runTest {
        // Arrange
        val sale = Penjualan(
            saleDate = Date(),
            totalAmount = 5000.0,
            tax = 0.0,
            discount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = 1L,
            warehouseId = 1L
        )
        val items = listOf(
            ItemPenjualan(saleId = 0, productId = 1L, quantity = 5, unitPrice = 1000.0, totalPrice = 5000.0)
        )
        
        whenever(promoService.calculateDiscount(any())).thenReturn(0.0)

        val product = Produk(
            id = 1L,
            name = "Test Product",
            sellingPrice = 1000.0,
            costPrice = 800.0,
            stockQuantity = 10, // More than requested 5
            minStock = 5,
            barcode = "123",
            categoryId = 1L,
            warehouseId = 1L,
            createdAt = Date()
        )

        val stokGudang = StokGudang(
            productId = 1L,
            warehouseId = 1L,
            quantity = 10 // More than requested 5
        )

        whenever(authService.hasPermission(any())).thenReturn(true)
        whenever(productRepository.getProductsByIds(any())).thenReturn(Result.success(listOf(product)))
        whenever(stokGudangRepository.getStocks(any(), any())).thenReturn(Result.success(listOf(stokGudang)))
        whenever(stokGudangRepository.adjustStockBatch(any())).thenReturn(Result.success(Unit))
        whenever(penjualanRepository.createPenjualan(any(), any())).thenReturn(Result.success(PenjualanWithItems(sale, items)))
        whenever(promoService.calculateDiscount(any())).thenReturn(0.0)

        // Act
        val result = saleService.createPenjualan(sale, items)

        // Assert
        assertTrue(result.isSuccess)
        verify(stokGudangRepository).adjustStockBatch(
            listOf(com.chibychibystore.data.model.StockAdjustment(1L, 1L, -5))
        )
    }

}
