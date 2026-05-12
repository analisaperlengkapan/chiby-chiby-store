package com.chibychibystore.integration
import org.robolectric.annotation.Config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.impl.SaleServiceImpl
import com.chibychibystore.service.impl.PromoServiceImpl
import com.chibychibystore.testutils.TestDataBuilder
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
import com.chibychibystore.testutils.BaseTest
import java.util.Date

/**
 * Integration test untuk Sales Module (POS)
 *
 * Test ini memastikan integrasi end-to-end untuk:
 * - Proses penjualan
 * - Update stok otomatis
 * - Perhitungan total
 * - Riwayat penjualan
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SalesModuleIntegrationTest : BaseTest() {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var penggunaRepository: PenggunaRepository
    private lateinit var productService: com.chibychibystore.service.ProductService
    private lateinit var saleService: SaleService

    @Before
    fun setup() {
        // Setup in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        // Setup repositories
        penjualanRepository = PenjualanRepository(database.penjualanDao(), database.itemPenjualanDao())
        itemPenjualanRepository = ItemPenjualanRepository(database.itemPenjualanDao())
        produkRepository = ProdukRepository(database.produkDao())
        penggunaRepository = PenggunaRepository(database.penggunaDao())

        // Setup service (use a test printer stub)
        val printerStub = com.chibychibystore.testutils.TestPrinterService()
        val authService = Mockito.mock(com.chibychibystore.service.AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }
        val stokGudangRepository = StokGudangRepository(database.stokGudangDao(), database.produkDao())
        val promoService = PromoServiceImpl(PromotionRepository(database.promotionDao()))
        productService = com.chibychibystore.service.impl.ProductServiceImpl(produkRepository, stokGudangRepository, authService)
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
    fun `complete sales transaction should work end-to-end`() = runTest {
        // 1. Setup: Create user
        val user = Pengguna(
            username = "cashier1",
            passwordHash = "hash",
            role = Role.CASHIER
        )
        val createUserRes = penggunaRepository.createPengguna(user)
        assertTrue("User creation failed: ${createUserRes.exceptionOrNull()?.message}", createUserRes.isSuccess)
        println("createUserRes: $createUserRes")
        val userId = createUserRes.getOrNull() ?: error("user id null")

        // 2. Setup: Create products
            // Ensure referenced category and warehouse exist (foreign key constraints)
            val categoryId = database.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Electronics"))
            val warehouseId = database.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Main", location = "Jakarta"))

            val p1 = TestDataBuilder.createTestProduct(name = "Laptop", barcode = "LP001", costPrice = 5000000.0, sellingPrice = 7000000.0, stockQuantity = 10, categoryId = categoryId, warehouseId = warehouseId)
            val p2 = TestDataBuilder.createTestProduct(name = "Mouse", barcode = "MS001", costPrice = 50000.0, sellingPrice = 100000.0, stockQuantity = 50, categoryId = categoryId, warehouseId = warehouseId)

        val p1Res = productService.createProduk(p1)
        assertTrue("Product p1 creation failed: ${p1Res.exceptionOrNull()?.message}", p1Res.isSuccess)
        val p1Id = p1Res.getOrNull()?.id ?: error("p1 id null")
        val p2Res = productService.createProduk(p2)
        assertTrue("Product p2 creation failed: ${p2Res.exceptionOrNull()?.message}", p2Res.isSuccess)
        val p2Id = p2Res.getOrNull()?.id ?: error("p2 id null")

        // 3. Create sale items
        val saleItems = listOf(
            ItemPenjualan(
                id = 0,
                saleId = 0,
                productId = p1Id,
                quantity = 2,
                unitPrice = 7000000.0,
                totalPrice = 14000000.0
            ),
            ItemPenjualan(
                id = 0,
                saleId = 0,
                productId = p2Id,
                quantity = 3,
                unitPrice = 100000.0,
                totalPrice = 300000.0
            )
        )

        // 4. Process sale
        val sale = Penjualan(
            saleDate = Date(),
            totalAmount = 0.0, // will be computed by service
            paymentMethod = PaymentMethod.CASH,
            cashierId = userId
        )

        val saleResult = saleService.createPenjualan(sale, saleItems)

        assertTrue(saleResult.isSuccess)
        val saleWithItems = saleResult.getOrNull()
        assertNotNull(saleWithItems)

        // 5. Verify sale data
        assertEquals(14300000.0, saleWithItems?.sale?.totalAmount)
        assertEquals(userId, saleWithItems?.sale?.cashierId)

        // 6. Verify stock was updated
        val updatedProduk1 = produkRepository.getProdukById(p1Id).getOrNull()
        val updatedProduk2 = produkRepository.getProdukById(p2Id).getOrNull()

        assertEquals(8, updatedProduk1?.stockQuantity)
        assertEquals(47, updatedProduk2?.stockQuantity)

        // 7. Verify sale items were saved
        val allSales = penjualanRepository.getAllPenjualan().first()
        assertEquals(1, allSales.size)
    }

    @Test
    fun `sale with insufficient stock should fail`() = runTest {
        // Setup: Create category and warehouse required by product
        val categoryId = database.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "TestCategory"))
        val warehouseId = database.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "TestWarehouse", location = "Nowhere"))

        // Setup: Create product with low stock
        val p = TestDataBuilder.createTestProduct(name = "Laptop", stockQuantity = 1, categoryId = categoryId, warehouseId = warehouseId)
        val pId = produkRepository.createProduk(p).getOrNull()!!

        // Try to sell 2 units (more than available)
        val saleItems = listOf(
            ItemPenjualan(
                id = 0,
                saleId = 0,
                productId = pId,
                quantity = 2,
                unitPrice = 7000000.0,
                totalPrice = 14000000.0
            )
        )

        val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)

        val saleResult = saleService.createPenjualan(sale, saleItems)

        // Should fail due to insufficient stock
        assertTrue(saleResult.isFailure)
    }

    @Test
    fun `get sales history should return all sales`() = runTest {
        // Create user
        val user = Pengguna(username = "cashier1", passwordHash = "hash", role = Role.CASHIER)
        val createUserRes = penggunaRepository.createPengguna(user)
        assertTrue("User creation failed in history test: ${createUserRes.exceptionOrNull()?.message}", createUserRes.isSuccess)
        val userId = createUserRes.getOrNull()!!

        // Create category and warehouse required by product
        val categoryId = database.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "TestCategory"))
        val warehouseId = database.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "TestWarehouse", location = "Nowhere"))

        // Create product
        val p = TestDataBuilder.createTestProduct(name = "Laptop", stockQuantity = 100, categoryId = categoryId, warehouseId = warehouseId)
        val pRes = productService.createProduk(p)
        assertTrue("Product creation failed in history test: ${pRes.exceptionOrNull()?.message}", pRes.isSuccess)
        val pId = pRes.getOrNull()?.id ?: error("product id null")

        // Create multiple sales
        for (i in 1..3) {
            val saleItems = listOf(
                ItemPenjualan(
                    id = 0,
                    saleId = 0,
                    productId = pId,
                    quantity = 1,
                    unitPrice = 7000000.0,
                    totalPrice = 7000000.0
                )
            )

            val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = userId)
            val result = saleService.createPenjualan(sale, saleItems)
            assertTrue("Sale creation failed at iteration $i: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }

        // Get sales history
        val salesHistory = saleService.getPenjualanByRentangTanggal()
        assertTrue(salesHistory.isSuccess)

        val sales = salesHistory.getOrNull()
        assertNotNull("Sales history should not be null", sales)
        assertEquals("Should have exactly 3 sales in history. Found: ${sales?.size}", 3, sales?.size)
    }

    @Test
    fun `calculate total and change should be accurate`() = runTest {
        val items = listOf(
            ItemPenjualan(
                id = 0,
                saleId = 0,
                productId = 1,
                quantity = 2,
                unitPrice = 7000000.0,
                totalPrice = 14000000.0
            ),
            ItemPenjualan(
                id = 0,
                saleId = 0,
                productId = 2,
                quantity = 3,
                unitPrice = 100000.0,
                totalPrice = 300000.0
            )
        )

        val totalAmount = items.sumOf { it.totalPrice }
        assertEquals(14300000.0, totalAmount, 0.01)

        val paymentAmount = 15000000.0
        val changeAmount = paymentAmount - totalAmount
        assertEquals(700000.0, changeAmount, 0.01)
    }
}

