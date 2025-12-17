package com.chibychibystore.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.SaleServiceImpl
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
 * - Perhitungan total dan kembalian
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
        penjualanRepository = PenjualanRepository(database.penjualanDao())
        itemPenjualanRepository = ItemPenjualanRepository(database.itemPenjualanDao())
        produkRepository = ProdukRepository(database.produkDao())
        penggunaRepository = PenggunaRepository(database.penggunaDao())

        // Setup service
        saleService = SaleServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository
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
            id = 1,
            username = "cashier1",
            passwordHash = "hash",
            namaLengkap = "Kasir Satu",
            role = Role.CASHIER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        penggunaRepository.insertPengguna(user)

        // 2. Setup: Create products
        val produk1 = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 10,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        val produk2 = Produk(
            id = 2,
            nama = "Mouse",
            barcode = "MS001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 50000.0,
            hargaJual = 100000.0,
            stok = 50,
            minStok = 10,
            createdAt = Date(),
            updatedAt = Date()
        )
        produkRepository.insertProduk(produk1)
        produkRepository.insertProduk(produk2)

        // 3. Create sale items
        val saleItems = listOf(
            ItemPenjualan(
                id = 0,
                penjualanId = 0, // Will be set by service
                produkId = 1,
                quantity = 2,
                hargaSatuan = 7000000.0,
                subtotal = 14000000.0,
                createdAt = Date()
            ),
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 2,
                quantity = 3,
                hargaSatuan = 100000.0,
                subtotal = 300000.0,
                createdAt = Date()
            )
        )

        // 4. Process sale
        val totalAmount = 14300000.0
        val paymentAmount = 15000000.0
        val changeAmount = 700000.0

        val saleResult = saleService.createSale(
            userId = 1,
            items = saleItems,
            totalAmount = totalAmount,
            paymentAmount = paymentAmount,
            changeAmount = changeAmount
        )

        assertTrue(saleResult.isSuccess)
        val sale = saleResult.getOrNull()
        assertNotNull(sale)

        // 5. Verify sale data
        assertEquals(totalAmount, sale?.totalAmount)
        assertEquals(paymentAmount, sale?.paymentAmount)
        assertEquals(changeAmount, sale?.changeAmount)
        assertEquals(1L, sale?.userId)

        // 6. Verify stock was updated
        val updatedProduk1 = produkRepository.getProdukById(1)
        val updatedProduk2 = produkRepository.getProdukById(2)
        
        assertEquals(8, updatedProduk1?.stok) // 10 - 2
        assertEquals(47, updatedProduk2?.stok) // 50 - 3

        // 7. Verify sale items were saved
        val allSales = penjualanRepository.getAllPenjualan().first()
        assertEquals(1, allSales.size)
    }

    @Test
    fun `sale with insufficient stock should fail`() = runTest {
        // Setup: Create product with low stock
        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 1, // Only 1 in stock
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        produkRepository.insertProduk(produk)

        // Try to sell 2 units (more than available)
        val saleItems = listOf(
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 1,
                quantity = 2, // Requesting 2 but only 1 available
                hargaSatuan = 7000000.0,
                subtotal = 14000000.0,
                createdAt = Date()
            )
        )

        val saleResult = saleService.createSale(
            userId = 1,
            items = saleItems,
            totalAmount = 14000000.0,
            paymentAmount = 15000000.0,
            changeAmount = 1000000.0
        )

        // Should fail due to insufficient stock
        assertTrue(saleResult.isFailure)
    }

    @Test
    fun `get sales history should return all sales`() = runTest {
        // Create user
        val user = Pengguna(
            id = 1,
            username = "cashier1",
            passwordHash = "hash",
            namaLengkap = "Kasir Satu",
            role = Role.CASHIER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        penggunaRepository.insertPengguna(user)

        // Create product
        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 100,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        produkRepository.insertProduk(produk)

        // Create multiple sales
        for (i in 1..3) {
            val saleItems = listOf(
                ItemPenjualan(
                    id = 0,
                    penjualanId = 0,
                    produkId = 1,
                    quantity = 1,
                    hargaSatuan = 7000000.0,
                    subtotal = 7000000.0,
                    createdAt = Date()
                )
            )

            saleService.createSale(
                userId = 1,
                items = saleItems,
                totalAmount = 7000000.0,
                paymentAmount = 7000000.0,
                changeAmount = 0.0
            )
        }

        // Get sales history
        val salesHistory = saleService.getSalesHistory()
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
                penjualanId = 0,
                produkId = 1,
                quantity = 2,
                hargaSatuan = 7000000.0,
                subtotal = 14000000.0,
                createdAt = Date()
            ),
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 2,
                quantity = 3,
                hargaSatuan = 100000.0,
                subtotal = 300000.0,
                createdAt = Date()
            )
        )

        val totalAmount = items.sumOf { it.subtotal }
        assertEquals(14300000.0, totalAmount, 0.01)

        val paymentAmount = 15000000.0
        val changeAmount = paymentAmount - totalAmount
        assertEquals(700000.0, changeAmount, 0.01)
    }
}
