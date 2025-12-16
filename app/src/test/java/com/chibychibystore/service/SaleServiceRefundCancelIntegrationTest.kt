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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class SaleServiceRefundCancelIntegrationTest {

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
        saleService = SaleServiceImpl(penjualanRepo, itemPenjualanRepo, produkRepo, printerService)
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
    fun doubleRefund_willIncreaseStockTwice_currentBehavior() = runBlocking {
        // Seed minimal data
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "CatX"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Gx"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cx", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(name = "DblRefund", barcode = "DR1", categoryId = kategoriId, costPrice = 1000.0, sellingPrice = 2000.0, stockQuantity = 5, warehouseId = gudangId)
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 4000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 2000.0, totalPrice = 4000.0)
        val res = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(res.isSuccess)
        val saleId = res.getOrNull()!!.penjualan.id

        // Refund once
        val r1 = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(r1.isSuccess)
        val after1 = db.produkDao().getProdukById(prodId)!!
        assertEquals(5, after1.stockQuantity)

        // Second refund should now be rejected (idempotent behavior)
        val r2 = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue("Second refund should fail to enforce idempotency", r2.isFailure)
        val after2 = db.produkDao().getProdukById(prodId)!!
        // Stock should remain unchanged after rejected second refund
        assertEquals(5, after2.stockQuantity)
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
