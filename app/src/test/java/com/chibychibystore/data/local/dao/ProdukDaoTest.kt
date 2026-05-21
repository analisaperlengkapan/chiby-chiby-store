package com.chibychibystore.data.local.dao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProdukDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var produkDao: ProdukDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        produkDao = database.produkDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGetProduct() = runBlocking {
        val catId = database.kategoriDao().insertKategori(Kategori(name = "C"))
        val whId = database.gudangDao().insertGudang(Gudang(name = "W"))
        val id = produkDao.insertProduk(Produk(name = "P", barcode = "B", costPrice = 1.0, sellingPrice = 2.0, categoryId = catId, warehouseId = whId, stockQuantity = 10))
        Assert.assertNotNull(produkDao.getProdukById(id))
    }
    @Test
    fun adjustStock() = runBlocking {
        val catId = database.kategoriDao().insertKategori(Kategori(name = "C"))
        val whId = database.gudangDao().insertGudang(Gudang(name = "W"))
        val id = produkDao.insertProduk(Produk(name = "P", categoryId = catId, warehouseId = whId, costPrice = 1.0, sellingPrice = 2.0, stockQuantity = 10))
        produkDao.adjustStock(id, 5)
        Assert.assertEquals(15, produkDao.getProdukById(id)?.stockQuantity)
    }
}
