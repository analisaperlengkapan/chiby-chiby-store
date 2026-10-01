package com.chibychibystore.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Restores the DAO coverage that the deleted `ProdukDaoTest` used to provide.
 * The original was written against a DAO that no longer exists (`updateStock`,
 * `insertProdukList` returning a single id); these cases exercise the real
 * queries the repository and services depend on.
 */
@RunWith(RobolectricTestRunner::class)
class ProdukDaoTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var produkDao: ProdukDao
    private var kategoriId: Long = 0
    private var gudangId: Long = 0

    @Before
    fun setup() {
        runBlocking {
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                ChibyChibyDatabase::class.java
            ).allowMainThreadQueries().build()

            produkDao = database.produkDao()
            kategoriId = database.kategoriDao().insertKategori(Kategori(name = "Test Category"))
            gudangId = database.gudangDao().insertGudang(Gudang(name = "Test Warehouse"))
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun produk(
        name: String,
        barcode: String? = null,
        stock: Int = 0,
        minStock: Int = 0
    ) = Produk(
        name = name,
        barcode = barcode,
        categoryId = kategoriId,
        costPrice = 10.0,
        sellingPrice = 15.0,
        stockQuantity = stock,
        warehouseId = gudangId,
        minStock = minStock,
        createdAt = Date()
    )

    @Test
    fun `insert and get produk round trips the row`() = runTest {
        val id = produkDao.insertProduk(produk("Test Product", barcode = "123456789", stock = 100))

        val retrieved = produkDao.getProdukById(id)

        assertNotNull(retrieved)
        assertEquals("Test Product", retrieved?.name)
        assertEquals("123456789", retrieved?.barcode)
        assertEquals(100, retrieved?.stockQuantity)
    }

    @Test
    fun `getProdukByBarcode finds the matching product`() = runTest {
        produkDao.insertProduk(produk("Test Product", barcode = "123456789"))

        assertNotNull(produkDao.getProdukByBarcode("123456789"))
        assertNull(produkDao.getProdukByBarcode("does-not-exist"))
    }

    @Test
    fun `searchProduk matches name and barcode substrings`() = runTest {
        produkDao.insertProduk(produk("Apple", barcode = "111"))
        produkDao.insertProduk(produk("Banana", barcode = "222"))

        val byName = produkDao.searchProduk("app").first()
        assertEquals(1, byName.size)
        assertEquals("Apple", byName[0].name)

        val byBarcode = produkDao.searchProduk("222").first()
        assertEquals(1, byBarcode.size)
        assertEquals("Banana", byBarcode[0].name)
    }

    @Test
    fun `getLowStockProduk returns only items at or below the minimum`() = runTest {
        produkDao.insertProduk(produk("Low Stock Item", stock = 5, minStock = 10))
        produkDao.insertProduk(produk("Normal Stock Item", stock = 50, minStock = 10))
        // Zero stock is intentionally excluded by the query (it is "out of stock").
        produkDao.insertProduk(produk("Out Of Stock Item", stock = 0, minStock = 10))

        val lowStock = produkDao.getLowStockProduk().first()

        assertEquals(1, lowStock.size)
        assertEquals("Low Stock Item", lowStock[0].name)
    }

    @Test
    fun `setStock replaces the stored quantity`() = runTest {
        val id = produkDao.insertProduk(produk("Test Product", stock = 100))

        produkDao.setStock(id, 90)

        assertEquals(90, produkDao.getProdukById(id)?.stockQuantity)
    }

    @Test
    fun `adjustStock adds a delta to the stored quantity`() = runTest {
        val id = produkDao.insertProduk(produk("Test Product", stock = 100))

        produkDao.adjustStock(id, -10)

        assertEquals(90, produkDao.getProdukById(id)?.stockQuantity)
    }

    @Test
    fun `counts and totals aggregate the table`() = runTest {
        assertEquals(0, produkDao.getProdukCount())

        produkDao.insertProduk(produk("Product 1", stock = 10))
        produkDao.insertProduk(produk("Product 2", stock = 20))

        assertEquals(2, produkDao.getProdukCount())
        assertEquals(30, produkDao.getTotalStock())
        assertEquals(300.0, produkDao.getTotalInventoryValue()!!, 0.001)
    }

    @Test
    fun `countProdukByKategori and countProdukByGudang filter correctly`() = runTest {
        produkDao.insertProduk(produk("Product 1"))
        produkDao.insertProduk(produk("Product 2"))

        assertEquals(2, produkDao.countProdukByKategori(kategoriId))
        assertEquals(2, produkDao.countProdukByGudang(gudangId))
        assertEquals(0, produkDao.countProdukByKategori(kategoriId + 99))
    }
}
