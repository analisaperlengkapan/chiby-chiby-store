package com.chibychibystore.data.local.dao
import org.robolectric.annotation.Config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProdukDaoTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var produkDao: ProdukDao
    private lateinit var kategoriDao: KategoriDao
    private lateinit var gudangDao: GudangDao

    private var kategoriId: Long = 0
    private var gudangId: Long = 0

    @Before
    fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).build()

        produkDao = database.produkDao()
        kategoriDao = database.kategoriDao()
        gudangDao = database.gudangDao()

        // Insert test data
        kategoriId = kategoriDao.insertKategori(Kategori(name = "Test Category"))
        gudangId = gudangDao.insertGudang(Gudang(name = "Test Warehouse"))
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetProduk() = runBlocking {
        val produk = Produk(
            name = "Test Product",
            barcode = "123456789",
            categoryId = kategoriId,
            costPrice = 10.0,
            sellingPrice = 15.0,
            stockQuantity = 100,
            warehouseId = gudangId
        )

        val id = produkDao.insertProduk(produk)
        val retrieved = produkDao.getProdukById(id)

        assertNotNull(retrieved)
        assertEquals("Test Product", retrieved?.name)
        assertEquals("123456789", retrieved?.barcode)
        assertEquals(100, retrieved?.stockQuantity)
    }

    @Test
    fun getProdukByBarcode() = runBlocking {
        val produk = Produk(
            name = "Test Product",
            barcode = "123456789",
            categoryId = kategoriId,
            costPrice = 10.0,
            sellingPrice = 15.0,
            warehouseId = gudangId
        )

        produkDao.insertProduk(produk)
        val retrieved = produkDao.getProdukByBarcode("123456789")

        assertNotNull(retrieved)
        assertEquals("Test Product", retrieved?.name)
    }

    @Test
    fun searchProduk() = runBlocking {
        val produk1 = Produk(
            name = "Apple",
            barcode = "111",
            categoryId = kategoriId,
            costPrice = 5.0,
            sellingPrice = 7.0,
            warehouseId = gudangId
        )
        val produk2 = Produk(
            name = "Banana",
            barcode = "222",
            categoryId = kategoriId,
            costPrice = 3.0,
            sellingPrice = 5.0,
            warehouseId = gudangId
        )

        produkDao.insertProduk(produk1)
        produkDao.insertProduk(produk2)

        val results = produkDao.searchProduk("app").first()

        assertEquals(1, results.size)
        assertEquals("Apple", results[0].name)
    }

    @Test
    fun getLowStockProduk() = runBlocking {
        val lowStockProduk = Produk(
            name = "Low Stock Item",
            categoryId = kategoriId,
            costPrice = 10.0,
            sellingPrice = 15.0,
            stockQuantity = 5,
            minStock = 10,
            warehouseId = gudangId
        )
        val normalStockProduk = Produk(
            name = "Normal Stock Item",
            categoryId = kategoriId,
            costPrice = 10.0,
            sellingPrice = 15.0,
            stockQuantity = 50,
            minStock = 10,
            warehouseId = gudangId
        )

        produkDao.insertProduk(lowStockProduk)
        produkDao.insertProduk(normalStockProduk)

        val lowStockItems = produkDao.getLowStockProduk().first()

        assertEquals(1, lowStockItems.size)
        assertEquals("Low Stock Item", lowStockItems[0].name)
    }

    @Test
    fun updateStock() = runBlocking {
        val produk = Produk(
            name = "Test Product",
            categoryId = kategoriId,
            costPrice = 10.0,
            sellingPrice = 15.0,
            stockQuantity = 100,
            warehouseId = gudangId
        )

        val id = produkDao.insertProduk(produk)
        produkDao.adjustStock(id, -10) // Reduce stock by 10

        val updated = produkDao.getProdukById(id)
        assertEquals(90, updated?.stockQuantity)
    }

    @Test
    fun getProdukCount() = runBlocking {
        val countBefore = produkDao.getProdukCount()
        assertEquals(0, countBefore)

        produkDao.insertProduk(Produk(
            name = "Test",
            categoryId = kategoriId,
            costPrice = 1.0,
            sellingPrice = 2.0,
            warehouseId = gudangId
        ))

        val countAfter = produkDao.getProdukCount()
        assertEquals(1, countAfter)
    }

    @Test
    fun getTotalStock() = runBlocking {
        val produk1 = Produk(
            name = "Product 1",
            categoryId = kategoriId,
            costPrice = 1.0,
            sellingPrice = 2.0,
            stockQuantity = 10,
            warehouseId = gudangId
        )
        val produk2 = Produk(
            name = "Product 2",
            categoryId = kategoriId,
            costPrice = 1.0,
            sellingPrice = 2.0,
            stockQuantity = 20,
            warehouseId = gudangId
        )

        produkDao.insertProduk(produk1)
        produkDao.insertProduk(produk2)

        val totalStock = produkDao.getTotalStock()
        assertEquals(30, totalStock)
    }
}