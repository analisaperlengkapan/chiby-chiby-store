package com.chibychibystore.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.impl.ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.chibychibystore.testutils.BaseTest
import org.mockito.Mockito
import java.util.Date
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReportingServiceIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var saleRepo: PenjualanRepository
    private lateinit var purchaseRepo: PembelianRepository
    private lateinit var productRepo: ProdukRepository
    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        saleRepo = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        purchaseRepo = PembelianRepository(db.pembelianDao())
        productRepo = ProdukRepository(db.produkDao())

        val balanceSheetService = Mockito.mock(BalanceSheetService::class.java)
        val cashManagementService = Mockito.mock(CashManagementService::class.java)
        val authService = Mockito.mock(AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }

        reportingService = ReportingServiceImpl(
            saleRepo,
            com.chibychibystore.repository.ItemPenjualanRepository(db.itemPenjualanDao()),
            productRepo,
            com.chibychibystore.repository.PengeluaranRepository(db.pengeluaranDao()),
            purchaseRepo,
            balanceSheetService,
            cashManagementService,
            authService,
            db
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getGrossSales_and_getProfitMargin_calculations() = runBlocking {
        // Seed required foreign keys
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "R1"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "G1"))
        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "rpt", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))

        // Product
        val prod = Produk(name = "P1", barcode = "b1", categoryId = kategoriId, costPrice = 5000.0, sellingPrice = 10000.0, stockQuantity = 50, warehouseId = gudangId)
        val prodId = db.produkDao().insertProduk(prod)

        // Two sales
        val sale1 = Penjualan(saleDate = Date(), totalAmount = 10000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val saleId1 = db.penjualanDao().insertPenjualan(sale1)
        db.itemPenjualanDao().insertItemPenjualan(com.chibychibystore.data.local.entity.ItemPenjualan(saleId = saleId1, productId = prodId, quantity = 1, unitPrice = 10000.0, totalPrice = 10000.0))

        val sale2 = Penjualan(saleDate = Date(), totalAmount = 20000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId)
        val saleId2 = db.penjualanDao().insertPenjualan(sale2)
        db.itemPenjualanDao().insertItemPenjualan(com.chibychibystore.data.local.entity.ItemPenjualan(saleId = saleId2, productId = prodId, quantity = 2, unitPrice = 10000.0, totalPrice = 20000.0))

        // Purchase (cost)
        val pemasokId = db.pemasokDao().insertPemasok(com.chibychibystore.data.local.entity.Pemasok(name = "S1"))
        val pembelian = com.chibychibystore.data.local.entity.Pembelian(purchaseDate = Date(), supplierId = pemasokId, totalAmount = 5000.0, receivedBy = cashierId, invoiceNumber = "INV-TEST-001")
        val pembelianId = db.pembelianDao().insertPembelian(pembelian)

        val start = LocalDate.now().minusDays(1)
        val end = LocalDate.now().plusDays(1)

        // Double-check repository state before calling service
        val repoSales = saleRepo.getSalesInDateRange(start, end)
        println("[DEBUG] repoSales.size=${repoSales.size} repoSales=${repoSales}")

        val grossRes = reportingService.getGrossSales(start, end)
        assertTrue(grossRes.isSuccess)
        val gross = grossRes.getOrNull()!!
        // gross is a Map<String, Any> in the current implementation
        println("[DEBUG] gross=$gross")
        assertEquals(30000.0, gross.totalPenjualan, 0.001)
        assertEquals(2, gross.totalTransaksi)

        // Refund one sale and verify gross sales excludes refunded sale
        val saleToRefund = db.penjualanDao().getPenjualanById(saleId1)!!
        // mark refunded via repository
        saleRepo.updatePenjualan(saleToRefund.copy(isRefunded = true))

        val repoSalesAfterRefund = saleRepo.getSalesInDateRange(start, end)
        println("[DEBUG] repoSalesAfterRefund.size=${repoSalesAfterRefund.size} repoSalesAfterRefund=${repoSalesAfterRefund}")

        val grossAfterRefund = reportingService.getGrossSales(start, end)
        assertTrue(grossAfterRefund.isSuccess)
        val gross2 = grossAfterRefund.getOrNull()!!
        // gross2 is a Map<String, Any>
        println("[DEBUG] grossAfterRefund=$gross2")
        assertEquals("Refunded sale should be excluded from gross sales", 20000.0, gross2.totalPenjualan, 0.001)
        assertEquals(1, gross2.totalTransaksi)

        val profitRes = reportingService.getProfitMargin(start, end)
        assertTrue(profitRes.isSuccess)
        val profit = profitRes.getOrNull()!!
        // profit is a Map<String, Any>
        // sale1 was refunded above, so only sale2 (20000) counts as revenue
        assertEquals(20000.0, profit.totalPendapatan, 0.001)
        // totalCost comes from purchases sum (in implementation: costOfGoodsSold)
        assertEquals(5000.0, profit.totalBiaya, 0.001)

    }

    @Test
    fun getSalesTrend_groupsByDateCorrectly() = runBlocking {
        val cal = java.util.Calendar.getInstance()
        cal.set(2025, 0, 1, 0, 0, 0)
        val d1 = cal.time
        cal.set(2025, 0, 1, 12, 0, 0)
        val d2 = cal.time
        cal.set(2025, 0, 2, 0, 0, 0)
        val d3 = cal.time

        val cashierId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "t1", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))
        db.penjualanDao().insertPenjualan(Penjualan(saleDate = d1, totalAmount = 10000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId))
        db.penjualanDao().insertPenjualan(Penjualan(saleDate = d2, totalAmount = 5000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId))
        db.penjualanDao().insertPenjualan(Penjualan(saleDate = d3, totalAmount = 7000.0, paymentMethod = PaymentMethod.CASH, cashierId = cashierId))

        val start = java.time.LocalDate.of(2025, 1, 1)
        val end = java.time.LocalDate.of(2025, 1, 3)

        val trendRes = reportingService.getSalesTrend(start, end)
        assertTrue(trendRes.isSuccess)
        val trends = trendRes.getOrNull()!!
        // trends is a List<Map<String, Any>> in the current implementation
        // The implementation emits one entry per day in the range, including empty days.
        assertEquals(3, trends.size)
        val day1 = trends.first { it.tanggal == java.time.LocalDate.of(2025, 1, 1) }
        assertEquals(15000.0, day1.penjualan, 0.001)
        assertEquals(2, day1.transaksi)
        val day2 = trends.first { it.tanggal == java.time.LocalDate.of(2025, 1, 2) }
        assertEquals(7000.0, day2.penjualan, 0.001)
        assertEquals(1, day2.transaksi)
        val day3 = trends.first { it.tanggal == java.time.LocalDate.of(2025, 1, 3) }
        assertEquals(0.0, day3.penjualan, 0.001)
        assertEquals(0, day3.transaksi)
    }
}
