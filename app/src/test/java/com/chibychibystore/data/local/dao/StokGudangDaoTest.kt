package com.chibychibystore.data.local.dao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StokGudangDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var dao: StokGudangDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        dao = database.stokGudangDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGetStok() = runBlocking {
        val catId = database.kategoriDao().insertKategori(Kategori(name = "C"))
        val whId = database.gudangDao().insertGudang(Gudang(name = "W"))
        val prodId = database.produkDao().insertProduk(Produk(id = 1L, name = "P", categoryId = catId, warehouseId = whId, costPrice = 1.0, sellingPrice = 2.0))
        val stok = StokGudang(productId = prodId, warehouseId = whId, quantity = 50)
        dao.insertOrUpdateStock(stok)
        val fetched = dao.getStock(prodId, whId)
        Assert.assertEquals(50, fetched?.quantity)
    }
}
