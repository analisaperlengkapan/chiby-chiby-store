package com.chibychibystore.ui.inventory

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.impl.WarehouseServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*
import com.chibychibystore.testutils.BaseTest

@OptIn(ExperimentalCoroutinesApi::class)
class WarehouseViewModelIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var warehouseService: WarehouseServiceImpl

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val gudangRepo = GudangRepository(db.gudangDao())
        val produkRepo = ProdukRepository(db.produkDao())

        warehouseService = WarehouseServiceImpl(gudangRepo, produkRepo)
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun transferStock_moves_product_between_warehouses() = runTest(testDispatcher) {
        // Create two warehouses
        val w1 = Gudang(id = 0, name = "W1", location = "Jakarta", capacity = 100)
        val w2 = Gudang(id = 0, name = "W2", location = "Bandung", capacity = 100)

        val w1Id = warehouseService.createWarehouse(w1).getOrNull()!!.id
        val w2Id = warehouseService.createWarehouse(w2).getOrNull()!!.id

        // Insert product in warehouse 1 with full stock 10
        val product = Produk(
            id = 0,
            name = "Sample Product",
            barcode = "12345",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = w1Id,
            createdAt = Date(),
            updatedAt = Date()
        )

        val productId = db.produkDao().insertProduk(product)

        // Verify initial location
        val before = warehouseService.getWarehouseStock(w1Id).getOrNull()
        assertNotNull(before)
        assertTrue(before!!.any { it.id == productId && it.warehouseId == w1Id })

        // Perform transfer of the full stock
        val res = warehouseService.transferStock(productId, w1Id, w2Id, 10)
        assertTrue("Expected transfer success: ${res.exceptionOrNull()}", res.isSuccess)

        // Verify product moved to warehouse 2
        val afterW1 = warehouseService.getWarehouseStock(w1Id).getOrNull()
        val afterW2 = warehouseService.getWarehouseStock(w2Id).getOrNull()

        assertNotNull(afterW1)
        assertNotNull(afterW2)

        assertFalse(afterW1!!.any { it.id == productId })
        assertTrue(afterW2!!.any { it.id == productId && it.warehouseId == w2Id })
    }
}
