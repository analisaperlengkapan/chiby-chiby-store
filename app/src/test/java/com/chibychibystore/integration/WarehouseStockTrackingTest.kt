package com.chibychibystore.integration
import org.robolectric.annotation.Config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.impl.AuthServiceImpl
import com.chibychibystore.service.impl.ProductServiceImpl
import com.chibychibystore.service.impl.SaleServiceImpl
import com.chibychibystore.service.impl.WarehouseServiceImpl
import com.chibychibystore.testutils.BaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.Date
import com.chibychibystore.service.impl.PromoServiceImpl
import com.chibychibystore.service.printer.PrinterService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WarehouseStockTrackingTest : BaseTest() {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var gudangRepository: GudangRepository
    private lateinit var kategoriRepository: KategoriRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var stokGudangRepository: StokGudangRepository
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    private lateinit var warehouseService: WarehouseServiceImpl
    private lateinit var productService: ProductServiceImpl
    private lateinit var saleService: SaleServiceImpl
    private lateinit var authService: AuthServiceImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        gudangRepository = GudangRepository(database.gudangDao(), database.produkDao())
        kategoriRepository = KategoriRepository(database.kategoriDao(), database.produkDao())
        produkRepository = ProdukRepository(database.produkDao())
        stokGudangRepository = StokGudangRepository(database.stokGudangDao(), database.produkDao())
        penjualanRepository = PenjualanRepository(database.penjualanDao(), database.itemPenjualanDao())
        itemPenjualanRepository = ItemPenjualanRepository(database.itemPenjualanDao())
        val penggunaSessionRepository = PenggunaSessionRepository(database.penggunaSessionDao())

        // Mock AuthService to bypass permissions
        authService = Mockito.spy(AuthServiceImpl(database.penggunaDao(), penggunaSessionRepository))
        runBlocking {
            Mockito.doReturn(true).`when`(authService).hasPermission(Mockito.anyString())
        }

        // Setup users needed for logic
        runBlocking {
            val user = Pengguna(id = 1L, username = "testuser", role = Role.OWNER, passwordHash = "hash")
            database.penggunaDao().insertPengguna(user)
            database.penggunaSessionDao().insertSession(PenggunaSession(userId = 1L))
        }

        warehouseService = WarehouseServiceImpl(gudangRepository, produkRepository, stokGudangRepository, authService, database)
        productService = ProductServiceImpl(produkRepository, stokGudangRepository, authService)

        val printerStub = com.chibychibystore.testutils.TestPrinterService()
        val promoService = PromoServiceImpl(PromotionRepository(database.promotionDao()))

        saleService = SaleServiceImpl(
            database,
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            stokGudangRepository,
            authService,
            printerStub,
            promoService
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `sale should decrease stock in specific warehouse`() = runTest {
        // 1. Setup Inventory
        val kategori = Kategori(name = "General", description = "General", createdAt = Date())
        val catId = kategoriRepository.createKategori(kategori).getOrNull()!!

        val gudang = Gudang(name = "Gudang Test", location = "Loc", capacity = 100)
        val whId = warehouseService.createGudang(gudang).getOrNull()!!.id

        val initialStock = 10
        val produk = Produk(
            name = "Item A",
            categoryId = catId,
            warehouseId = whId,
            stockQuantity = initialStock,
            costPrice = 100.0,
            sellingPrice = 200.0,
            createdAt = Date(),
            updatedAt = Date()
        )
        val prodId = productService.createProduk(produk).getOrNull()!!.id

        // Verify initial state
        val stockInitial = stokGudangRepository.getStock(prodId, whId).getOrNull()
        assertEquals(initialStock, stockInitial?.quantity)
        assertEquals(initialStock, produkRepository.getProdukById(prodId).getOrNull()?.stockQuantity)

        // 2. Perform Sale
        val soldQty = 2
        val saleItem = ItemPenjualan(
            saleId = 0,
            productId = prodId,
            quantity = soldQty,
            unitPrice = 200.0,
            totalPrice = 400.0
        )
        val sale = Penjualan(
            saleDate = Date(),
            totalAmount = 400.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = 1L,
            warehouseId = whId
        )

        val saleResult = saleService.createPenjualan(sale, listOf(saleItem))
        assertTrue(saleResult.isSuccess)

        // 3. Verify Product Stock (Global)
        val productAfter = produkRepository.getProdukById(prodId).getOrNull()
        assertEquals("Global stock should decrease", initialStock - soldQty, productAfter?.stockQuantity)

        // 4. Verify Warehouse Stock
        val stockAfter = stokGudangRepository.getStock(prodId, whId).getOrNull()
        assertEquals("Warehouse stock should decrease", initialStock - soldQty, stockAfter?.quantity)
    }
}
