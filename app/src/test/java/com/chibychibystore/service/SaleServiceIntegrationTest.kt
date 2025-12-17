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
import org.robolectric.annotation.Config
import com.chibychibystore.testutils.BaseTest
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SaleServiceIntegrationTest : BaseTest() {

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
    fun createSale_integration_reducesStockAndPersistsSale() = runBlocking {
        // Seed category and warehouse (foreign keys)
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Food"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Main Warehouse"))

        // Seed product
        val prod = Produk(
            name = "Test Product",
            barcode = "1234567890",
            categoryId = kategoriId,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        // Insert a cashier user required by foreign key
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cashier", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        // Build sale and items
        val sale = Penjualan(
            saleDate = Date(),
            totalAmount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = cashierId
        )
        val item = ItemPenjualan(
            saleId = 0,
            productId = prodId,
            quantity = 2,
            unitPrice = 15000.0,
            totalPrice = 30000.0
        )

        // Execute
        val result = (saleService as SaleServiceImpl).createSale(sale, listOf(item))

        if (!result.isSuccess) {
            fail("Sale creation failed: ${result.exceptionOrNull()?.message}")
        }

        val created = result.getOrNull()
        assertNotNull("Sale should be persisted", created)
        assertEquals("One item should be recorded", 1, created!!.items.size)

        // Verify stock updated
        val updatedProd = db.produkDao().getProdukById(prodId)!!
        assertEquals("Stock should decrease by 2", 8, updatedProd.stockQuantity)
    }

    @Test
    fun refundSale_restoresStockAndKeepsSale() = runBlocking {
        // Seed product and cashier
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Food2"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Warehouse2"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cashier2", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(
            name = "Refund Product",
            barcode = "9876543210",
            categoryId = kategoriId,
            costPrice = 5000.0,
            sellingPrice = 10000.0,
            stockQuantity = 5,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 10000.0, totalPrice = 20000.0)

        val createRes = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(createRes.isSuccess)
        val created = createRes.getOrNull()!!
        val saleId = created.penjualan.id

        // Stock decreased
        val afterSale = db.produkDao().getProdukById(prodId)!!
        assertEquals(3, afterSale.stockQuantity)

        // Refund
        val refundRes = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(refundRes.isSuccess)

        // Stock restored
        val afterRefund = db.produkDao().getProdukById(prodId)!!
        assertEquals(5, afterRefund.stockQuantity)

        // Sale still exists and flagged as refunded
        val existingSale = db.penjualanDao().getPenjualanById(saleId)
        assertNotNull(existingSale)
        assertTrue("Sale should be marked as refunded", existingSale!!.isRefunded)
    }

    @Test
    fun refund_setsIsRefunded_and_secondRefundFails() = runBlocking {
        // Seed product and cashier
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "RefundFlagCat"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "RefundFlagWh"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "refundflag", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(
            name = "FlagProduct",
            barcode = "F-123",
            categoryId = kategoriId,
            costPrice = 1000.0,
            sellingPrice = 2000.0,
            stockQuantity = 4,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 2000.0, totalPrice = 4000.0)

        val createRes = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(createRes.isSuccess)
        val saleId = createRes.getOrNull()!!.penjualan.id

        // Refund once
        val firstRefund = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue(firstRefund.isSuccess)

        // Check DB flag
        val saleAfterRefund = db.penjualanDao().getPenjualanById(saleId)!!
        assertTrue("isRefunded should be true after refund", saleAfterRefund.isRefunded)

        // Second refund should fail and stock unchanged
        val secondRefund = (saleService as SaleServiceImpl).refundSale(saleId)
        assertTrue("Second refund should fail to enforce idempotency", secondRefund.isFailure)
        val afterSecond = db.produkDao().getProdukById(prodId)!!
        assertEquals("Stock should not increase after repeated refund", 4, afterSecond.stockQuantity)
    }

    @Test
    fun cancelSale_deletesSaleAndRestoresStock() = runBlocking {
        // Seed product and cashier
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Food3"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Warehouse3"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "cashier3", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        val prod = Produk(
            name = "Cancel Product",
            barcode = "1928374650",
            categoryId = kategoriId,
            costPrice = 7000.0,
            sellingPrice = 12000.0,
            stockQuantity = 4,
            warehouseId = gudangId
        )
        val prodId = db.produkDao().insertProduk(prod)

        val sale = Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val item = ItemPenjualan(saleId = 0, productId = prodId, quantity = 2, unitPrice = 12000.0, totalPrice = 24000.0)

        val createRes = (saleService as SaleServiceImpl).createSale(sale, listOf(item))
        assertTrue(createRes.isSuccess)
        val created = createRes.getOrNull()!!
        val saleId = created.penjualan.id

        // Stock decreased
        val afterSale = db.produkDao().getProdukById(prodId)!!
        assertEquals(2, afterSale.stockQuantity)

        // Cancel
        val cancelRes = (saleService as SaleServiceImpl).cancelSale(saleId)
        assertTrue(cancelRes.isSuccess)

        // Stock restored and sale removed
        val afterCancel = db.produkDao().getProdukById(prodId)!!
        assertEquals(4, afterCancel.stockQuantity)

        val existingSale = db.penjualanDao().getPenjualanById(saleId)
        assertNull(existingSale)

        val itemCount = db.itemPenjualanDao().getItemCountBySaleId(saleId)
        assertEquals(0, itemCount)
    }
}
