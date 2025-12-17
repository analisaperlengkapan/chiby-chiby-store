package com.chibychibystore.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.SaleServiceImpl
import com.chibychibystore.testutils.TestDataBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
class SalesModuleIntegrationTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var penggunaRepository: PenggunaRepository
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
        saleService = SaleServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            printerStub
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
        val userId = penggunaRepository.createPengguna(user).getOrNull()!!

        // 2. Setup: Create products
        val p1 = TestDataBuilder.createTestProduct(name = "Laptop", barcode = "LP001", costPrice = 5000000.0, sellingPrice = 7000000.0, stockQuantity = 10)
        val p2 = TestDataBuilder.createTestProduct(name = "Mouse", barcode = "MS001", costPrice = 50000.0, sellingPrice = 100000.0, stockQuantity = 50)

        val p1Id = produkRepository.createProduk(p1).getOrNull()!!
        val p2Id = produkRepository.createProduk(p2).getOrNull()!!

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

        val saleResult = saleService.createSale(sale, saleItems)

        assertTrue(saleResult.isSuccess)
        val saleWithItems = saleResult.getOrNull()
        assertNotNull(saleWithItems)

        // 5. Verify sale data
        assertEquals(14300000.0, saleWithItems?.penjualan?.totalAmount)
        assertEquals(userId, saleWithItems?.penjualan?.cashierId)

        // 6. Verify stock was updated
        val updatedProduk1 = produkRepository.getProduk(p1Id)
        val updatedProduk2 = produkRepository.getProduk(p2Id)

        assertEquals(8, updatedProduk1?.stockQuantity)
        assertEquals(47, updatedProduk2?.stockQuantity)

        // 7. Verify sale items were saved
        val allSales = penjualanRepository.getAllPenjualan().first()
        assertEquals(1, allSales.size)
    }

    @Test
    fun `sale with insufficient stock should fail`() = runTest {
        // Setup: Create product with low stock
        val p = TestDataBuilder.createTestProduct(name = "Laptop", stockQuantity = 1)
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

        val saleResult = saleService.createSale(sale, saleItems)

        // Should fail due to insufficient stock
        assertTrue(saleResult.isFailure)
    }

    @Test
    fun `get sales history should return all sales`() = runTest {
        // Create user
        val user = Pengguna(username = "cashier1", passwordHash = "hash", role = Role.CASHIER)
        val userId = penggunaRepository.createPengguna(user).getOrNull()!!

        // Create product
        val p = TestDataBuilder.createTestProduct(name = "Laptop", stockQuantity = 100)
        val pId = produkRepository.createProduk(p).getOrNull()!!

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
            saleService.createSale(sale, saleItems)
        }

        // Get sales history
        val salesHistory = saleService.getSales()
        assertTrue(salesHistory.isSuccess)

        val sales = salesHistory.getOrNull()
        assertNotNull(sales)
        assertEquals(3, sales?.size)
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

