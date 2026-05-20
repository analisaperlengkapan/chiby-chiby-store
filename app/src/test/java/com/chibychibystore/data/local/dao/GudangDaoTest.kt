package com.chibychibystore.data.local.dao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GudangDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var dao: GudangDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        dao = database.gudangDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGet() = runBlocking {
        val id = dao.insertGudang(Gudang(name = "G"))
        Assert.assertEquals("G", dao.getGudangById(id)?.name)
    }
}
