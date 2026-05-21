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
class PenggunaDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var dao: PenggunaDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        dao = database.penggunaDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGetPengguna() = runBlocking {
        val id = dao.insertPengguna(Pengguna(username = "admin", passwordHash = "hash", role = Role.OWNER))
        val fetched = dao.getPenggunaById(id)
        Assert.assertNotNull(fetched)
        Assert.assertEquals("admin", fetched?.username)
    }
}
