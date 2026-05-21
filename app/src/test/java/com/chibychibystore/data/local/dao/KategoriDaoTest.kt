package com.chibychibystore.data.local.dao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Kategori
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KategoriDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var dao: KategoriDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        dao = database.kategoriDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGetAllKategori() = runBlocking {
        dao.insertKategori(Kategori(name = "Kategori A"))
        val all = dao.getAllKategori().first()
        Assert.assertEquals(1, all.size)
        Assert.assertEquals("Kategori A", all[0].name)
    }
}
