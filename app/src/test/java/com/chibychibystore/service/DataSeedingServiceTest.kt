package com.chibychibystore.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.KategoriRepository
import com.chibychibystore.repository.PemasokRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Restores coverage for [DataSeedingService], the first-run seeder. The deleted
 * test exercised private methods through reflection and asserted nothing; this
 * drives the real service over an in-memory database and checks the seeded rows.
 */
@RunWith(RobolectricTestRunner::class)
class DataSeedingServiceTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var service: DataSeedingService

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        service = DataSeedingService(
            PenggunaRepository(database.penggunaDao()),
            KategoriRepository(database.kategoriDao(), database.produkDao()),
            GudangRepository(database.gudangDao(), database.produkDao()),
            ProdukRepository(database.produkDao()),
            PemasokRepository(database.pemasokDao(), database.pembelianDao()),
            PengeluaranRepository(database.pengeluaranDao())
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `seedInitialData populates every table`() = runBlocking {
        service.seedInitialData()

        assertEquals(4, database.penggunaDao().getPenggunaCount())
        assertEquals(4, database.kategoriDao().getKategoriCount())
        assertEquals(2, database.gudangDao().getGudangCount())
        assertEquals(2, database.pemasokDao().getPemasokCount())
        assertEquals(4, database.produkDao().getProdukCount())
        assertEquals(5, database.pengeluaranDao().getAllPengeluarans().first().size)
    }

    @Test
    fun `seedInitialData creates the four default roles with hashed passwords`() = runBlocking {
        service.seedInitialData()

        val owner = database.penggunaDao().getPenggunaByUsername("owner")
        assertNotNull(owner)
        assertEquals(Role.OWNER, owner?.role)
        // Passwords are stored hashed, never in clear text.
        assertTrue(owner!!.passwordHash.isNotBlank())
        assertTrue(owner.passwordHash != "owner123")

        assertNotNull(database.penggunaDao().getPenggunaByUsername("manager"))
        assertNotNull(database.penggunaDao().getPenggunaByUsername("cashier"))
        assertNotNull(database.penggunaDao().getPenggunaByUsername("warehouse"))
    }

    @Test
    fun `seeded products have unique barcodes`() = runBlocking {
        service.seedInitialData()

        val barcodes = database.produkDao().getAllProduk().first().mapNotNull { it.barcode }
        assertEquals(barcodes.size, barcodes.toSet().size)
    }
}
