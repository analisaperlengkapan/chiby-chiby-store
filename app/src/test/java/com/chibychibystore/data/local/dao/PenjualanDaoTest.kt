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
import java.util.Date
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PenjualanDaoTest {
    private lateinit var database: ChibyChibyDatabase
    private lateinit var dao: PenjualanDao
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChibyChibyDatabase::class.java).allowMainThreadQueries().build()
        dao = database.penjualanDao()
    }
    @After
    fun tearDown() = database.close()
    @Test
    fun insertAndGetPenjualan() = runBlocking {
        val userId = database.penggunaDao().insertPengguna(Pengguna(username = "u", passwordHash = "h", role = Role.CASHIER))
        val sale = Penjualan(saleDate = Date(), totalAmount = 100.0, paymentMethod = PaymentMethod.CASH, cashierId = userId)
        val id = dao.insertPenjualan(sale)
        Assert.assertNotNull(dao.getPenjualanById(id))
    }
}
