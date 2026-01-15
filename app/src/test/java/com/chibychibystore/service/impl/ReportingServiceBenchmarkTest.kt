package com.chibychibystore.service.impl

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.Date
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class ReportingServiceBenchmarkTest {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var reportingService: ReportingServiceImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Populate Data
        runBlocking {
            populateData()
        }

        // Setup Service with real repositories
        val penjualanRepository = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        val itemPenjualanRepository = ItemPenjualanRepository(db.itemPenjualanDao())
        val produkRepository = ProdukRepository(db.produkDao())
        val pengeluaranRepository = PengeluaranRepository(db.pengeluaranDao())
        val pembelianRepository = PembelianRepository(db.pembelianDao())

        // Mocks for unused services
        val balanceSheetService = Mockito.mock(BalanceSheetService::class.java)
        val cashManagementService = Mockito.mock(CashManagementService::class.java)
        val authService = Mockito.mock(AuthService::class.java)
        Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)

        reportingService = ReportingServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            pengeluaranRepository,
            pembelianRepository,
            balanceSheetService,
            cashManagementService,
            authService
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun populateData() {
        // Create Categories
        val categories = (1..10).map {
            Kategori(id = it.toLong(), name = "Category $it")
        }
        categories.forEach { db.kategoriDao().insertKategori(it) }

        // Create Products (100 products, distributed across categories)
        val products = (1..100).map { i ->
            Produk(
                id = i.toLong(),
                name = "Product $i",
                categoryId = (i % 10 + 1).toLong(),
                costPrice = 100.0,
                price = 150.0,
                stockQuantity = 1000,
                minStock = 10,
                barcode = "BC$i",
                warehouseId = 1L,
                description = "Desc",
                image = null,
                createdAt = Date()
            )
        }
        db.produkDao().insertProdukList(products)

        // Create Sales (1000 sales)
        val sales = (1..1000).map { i ->
            Penjualan(
                id = i.toLong(),
                saleDate = Date(),
                totalAmount = 0.0, // Will be updated? No, we just need it for the query
                paymentMethod = PaymentMethod.CASH,
                cashierId = 1L
            )
        }
        // Insert sales individually or need a bulk insert if available. PenjualanDao only has insertPenjualan (single).
        // To be fast, loop. In-memory DB is fast.
        sales.forEach { db.penjualanDao().insertPenjualan(it) }

        // Create Sale Items (5 items per sale = 5000 items)
        val items = sales.flatMap { sale ->
            (1..5).map { j ->
                val prodId = ((sale.id.toInt() * 5 + j) % 100 + 1).toLong()
                ItemPenjualan(
                    saleId = sale.id,
                    productId = prodId,
                    quantity = 2,
                    unitPrice = 150.0,
                    totalPrice = 300.0
                )
            }
        }
        db.itemPenjualanDao().insertItemPenjualanList(items)
    }

    @Test
    fun benchmarkGetSalesByCategory() = runBlocking {
        val startDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(1)

        // Warmup
        reportingService.getSalesByCategory(startDate, endDate)

        val time = measureTimeMillis {
            val result = reportingService.getSalesByCategory(startDate, endDate)
            assert(result.isSuccess)
            // Ensure we get results
            val data = result.getOrNull()
            assert(data != null && data.isNotEmpty())
        }
        println("BENCHMARK_RESULT: Execution time: $time ms")
    }
}
