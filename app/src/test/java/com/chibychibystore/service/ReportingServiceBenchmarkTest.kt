package com.chibychibystore.service
import org.robolectric.annotation.Config

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.impl.ReportingServiceImpl
import com.chibychibystore.testutils.BaseTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Date
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Config(manifest = Config.NONE)
class ReportingServiceBenchmarkTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var saleRepo: PenjualanRepository
    private lateinit var itemSaleRepo: ItemPenjualanRepository
    private lateinit var productRepo: ProdukRepository
    private lateinit var expenseRepo: PengeluaranRepository
    private lateinit var purchaseRepo: PembelianRepository
    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        saleRepo = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        itemSaleRepo = ItemPenjualanRepository(db.itemPenjualanDao())
        purchaseRepo = PembelianRepository(db.pembelianDao())
        productRepo = ProdukRepository(db.produkDao())
        expenseRepo = PengeluaranRepository(db.pengeluaranDao())

        val balanceSheetService = Mockito.mock(BalanceSheetService::class.java)
        val cashManagementService = Mockito.mock(CashManagementService::class.java)
        val authService = Mockito.mock(AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }

        reportingService = ReportingServiceImpl(
            saleRepo,
            itemSaleRepo,
            productRepo,
            expenseRepo,
            purchaseRepo,
            balanceSheetService,
            cashManagementService,
            authService
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun benchmarkAndVerifyGetIncomeStatement() = runBlocking {
        // 1. Seed Data
        val productCount = 50 // Reduced for faster verification loop in test if needed
        val salesCount = 200
        val itemsPerSale = 3

        println("Seeding data: $productCount products, $salesCount sales with $itemsPerSale items each...")

        // Create Categories, Warehouse, User
        val catId = db.kategoriDao().insertKategori(Kategori(name = "Cat1"))
        val whId = db.gudangDao().insertGudang(Gudang(name = "WH1"))
        val userId = db.penggunaDao().insertPengguna(Pengguna(username = "u1", passwordHash = "pw", role = Role.CASHIER))

        // Create Products
        val productIds = (1..productCount).map { i ->
            db.produkDao().insertProduk(
                Produk(
                    name = "P$i",
                    categoryId = catId,
                    costPrice = 100.0 * i, // varied cost
                    sellingPrice = 150.0 * i,
                    stockQuantity = 10000,
                    warehouseId = whId
                )
            )
        }

        // Create Sales and Items and calculate Expected Values
        val date = Date()
        val localDate = LocalDate.now()

        var expectedRevenue = 0.0
        var expectedCogs = 0.0

        for (i in 1..salesCount) {
             val sale = Penjualan(
                 saleDate = date,
                 totalAmount = 0.0,
                 paymentMethod = PaymentMethod.CASH,
                 cashierId = userId
             )
             val saleId = db.penjualanDao().insertPenjualan(sale)

             var saleTotal = 0.0
             val items = (1..itemsPerSale).map {
                val pId = productIds.random()
                val product = db.produkDao().getProdukById(pId)!!
                val qty = (1..5).random()

                expectedCogs += product.costPrice * qty
                val itemTotal = 150.0 * qty // selling price approx
                saleTotal += itemTotal

                ItemPenjualan(
                    saleId = saleId,
                    productId = pId,
                    quantity = qty,
                    unitPrice = 150.0,
                    totalPrice = itemTotal
                )
             }
             db.itemPenjualanDao().insertItemPenjualanList(items)

             // Update sale totalAmount
             val updatedSale = sale.copy(id = saleId, totalAmount = saleTotal)
             db.penjualanDao().updatePenjualan(updatedSale)
             expectedRevenue += saleTotal
        }

        println("Data seeded. Expected Revenue: $expectedRevenue, Expected COGS: $expectedCogs")

        // 2. Verify Logic
        val result = reportingService.getIncomeStatement(localDate)
        if (result.isFailure) {
            throw result.exceptionOrNull()!!
        }
        val report = (result as com.chibychibystore.data.model.Result.Success).data

        println("Actual Revenue: ${report.totalPendapatan}, Actual COGS: ${report.hpp}")

        assertEquals("Revenue mismatch", expectedRevenue, report.totalPendapatan, 0.01)
        assertEquals("COGS mismatch", expectedCogs, report.hpp, 0.01)

        // 3. Measure Performance
        val iterations = 10
        var totalTime = 0L

        // Warmup
        reportingService.getIncomeStatement(localDate)

        for (i in 1..iterations) {
            val time = measureTimeMillis {
                reportingService.getIncomeStatement(localDate)
            }
            totalTime += time
            println("Iteration $i: ${time}ms")
        }

        val avgTime = totalTime / iterations
        println("Average time: ${avgTime}ms")
    }
}
