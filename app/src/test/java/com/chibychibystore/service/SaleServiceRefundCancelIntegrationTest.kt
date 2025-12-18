package com.chibychibystore.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.chibychibystore.testutils.BaseTest
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SaleServiceRefundCancelIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var produkRepo: ProdukRepository
    private lateinit var penjualanRepo: PenjualanRepository
    private lateinit var itemPenjualanRepo: ItemPenjualanRepository
    private lateinit var saleService: SaleService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        produkRepo = ProdukRepository(db.produkDao())
        penjualanRepo = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        itemPenjualanRepo = ItemPenjualanRepository(db.itemPenjualanDao())

        val printerService = Mockito.mock(PrinterService::class.java)
        val authService = Mockito.mock(AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }
        saleService = SaleServiceImpl(penjualanRepo, itemPenjualanRepo, produkRepo, printerService, authService)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun refundSale_restoresStockAndKeepsSale() = runBlocking {
        // Seed data: category, warehouse, cashier, product
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Cat"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "G1"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "c", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(
            name = "RefundProduct",
            barcode = "R-1",
            categoryId = kategoriId,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 5,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        // Create sale
        val sale = Penjualan(saleDate = Date(), totalAmount = 15000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 15000.0, totalPrice = 30000.0)
        val res = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(res.isSuccess)
        val created = res.getOrNull()!!
        val saleId = created.penjualan.id

        // After sale, stock decreased
        val afterSale = db.produkDao().getProdukById(prodId)!!
        assertEquals(3, afterSale.stockQuantity)

        // Refund sale: stock should be restored (3 + 2 = 5)
        val refundRes = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(refundRes.isSuccess)

        val restored = db.produkDao().getProdukById(prodId)!!
        assertEquals(5, restored.stockQuantity)

        // Sale record should still exist
        val fetched = db.penjualanDao().getPenjualanById(saleId)
        assertNotNull(fetched)
    }

    @Test
    fun concurrentRefunds_onlyOneSucceeds_andStockUpdatedOnce() = runBlocking {
        // Seed minimal data
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Conc"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Gconc"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cc", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(name = "Concurrent", barcode = "C-1", categoryId = kategoriId, costPrice = 1000.0, sellingPrice = 2000.0, stockQuantity = 5, warehouseId = gudangId)
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 4000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 2000.0, totalPrice = 4000.0)
        val res = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(res.isSuccess)
        val saleId = res.getOrNull()!!.penjualan.id

        // Prepare two concurrent refund calls using executor to simulate race
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val f1 = executor.submit(java.util.concurrent.Callable<com.chibychibystore.data.model.Result<Unit>> {
                // refundSale may perform suspend operations internally; runBlocking ensures it runs on this thread
                runBlocking { (saleService as SaleServiceImpl).refundSale(saleId) }
            })
            val f2 = executor.submit(java.util.concurrent.Callable<com.chibychibystore.data.model.Result<Unit>> {
                runBlocking { (saleService as SaleServiceImpl).refundSale(saleId) }
            })

            val r1 = f1.get()
            val r2 = f2.get()

            val successes = listOf(r1, r2).count { it.isSuccess }
            val failures = listOf(r1, r2).count { it.isFailure }

            assertEquals("Only one refund should succeed", 1, successes)
            assertEquals("One refund should fail", 1, failures)
        } finally {
            executor.shutdown()
        }

        // Stock should have been restored only once (5 initial -> sale reduced to 3 -> refund back to 5)
        val finalProd = db.produkDao().getProdukById(prodId)!!
        assertEquals(5, finalProd.stockQuantity)

        // Sale should be marked refunded
        val saleAfter = db.penjualanDao().getPenjualanById(saleId)!!
        assertTrue(saleAfter.isRefunded)
    }

    @Test
    fun refundAfterCancel_shouldFail() = runBlocking {
        // Seed minimal data
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "CatY"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Gy"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cy", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(name = "CancelRef", barcode = "CR1", categoryId = kategoriId, costPrice = 1000.0, sellingPrice = 2000.0, stockQuantity = 3, warehouseId = gudangId)
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 4000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 2000.0, totalPrice = 4000.0)
        val res = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(res.isSuccess)
        val saleId = res.getOrNull()!!.penjualan.id

        // Cancel sale
        val cancelRes = (saleService as SaleServiceImpl).cancelSale(saleId)
        assertTrue(cancelRes.isSuccess)

        // Refund should fail because sale no longer exists
        val refundRes = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(refundRes.isFailure)
    }

    @Test
    fun refundIsIdempotent_secondRefundFailsAndStockUnchanged() = runBlocking {
        // Seed minimal data
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "CatZ"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Gz"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cz", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(name = "IdempProd", barcode = "ID1", categoryId = kategoriId, costPrice = 2000.0, sellingPrice = 3000.0, stockQuantity = 6, warehouseId = gudangId)
        val prodId = db.produkDao().insertProduk(prod)

        // Create sale
        val sale = Penjualan(saleDate = Date(), totalAmount = 6000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 3000.0, totalPrice = 6000.0)
        val createRes = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(createRes.isSuccess)
        val saleId = createRes.getOrNull()!!.penjualan.id

        // Refund once
        val firstRefund = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(firstRefund.isSuccess)
        val afterFirst = db.produkDao().getProdukById(prodId)!!
        assertEquals(6, afterFirst.stockQuantity)
        val saleAfterRefund = db.penjualanDao().getPenjualanById(saleId)!!
        assertTrue("Sale should be marked as refunded after first refund", saleAfterRefund.isRefunded)

        // Second refund should be rejected (idempotency). Expect failure and stock unchanged
        val secondRefund = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue("Second refund should fail to enforce idempotency", secondRefund.isFailure)
        val afterSecond = db.produkDao().getProdukById(prodId)!!
        assertEquals("Stock should not increase after repeated refund", 6, afterSecond.stockQuantity)
    }

    @Test
    fun cancelSale_restoresStockAndRemovesSale() = runBlocking {
        // Seed necessary entities
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Cat2"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "G2"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "c2", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(
            name = "CancelProduct",
            barcode = "C-1",
            categoryId = kategoriId,
            costPrice = 5000.0,
            sellingPrice = 8000.0,
            stockQuantity = 4,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 16000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 8000.0, totalPrice = 16000.0)
        val res = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(res.isSuccess)
        val created = res.getOrNull()!!
        val saleId = created.penjualan.id

        // After sale, stock decreased to 2
        val afterSale = db.produkDao().getProdukById(prodId)!!
        assertEquals(2, afterSale.stockQuantity)

        // Cancel sale
        val cancelRes = (saleService as SaleServiceImpl).cancelSale(saleId)
        assertTrue(cancelRes.isSuccess)

        // Stock restored to 4
        val restored = db.produkDao().getProdukById(prodId)!!
        assertEquals(4, restored.stockQuantity)

        // Sale record removed
        val fetched = db.penjualanDao().getPenjualanById(saleId)
        assertNull(fetched)

        // Items removed
        val itemCount = db.itemPenjualanDao().getItemCountBySaleId(saleId)
        assertEquals(0, itemCount)
    }
}
