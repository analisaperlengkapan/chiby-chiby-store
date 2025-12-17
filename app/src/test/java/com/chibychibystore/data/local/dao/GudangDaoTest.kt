package com.chibychibystore.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Test untuk GudangDao
 * 
 * Memastikan operasi CRUD pada tabel gudang berfungsi dengan benar.
 */
@RunWith(RobolectricTestRunner::class)
class GudangDaoTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var gudangDao: GudangDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()
        
        gudangDao = database.gudangDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and get gudang should work`() = runTest {
        val gudang = Gudang(
            id = 1,
            nama = "Gudang Utama",
            lokasi = "Jakarta",
            deskripsi = "Gudang pusat",
            createdAt = Date(),
            updatedAt = Date()
        )

        gudangDao.insertGudang(gudang)
        val retrieved = gudangDao.getGudangById(1)

        assertNotNull(retrieved)
        assertEquals("Gudang Utama", retrieved?.nama)
        assertEquals("Jakarta", retrieved?.lokasi)
    }

    @Test
    fun `get all gudang should return list`() = runTest {
        val gudang1 = Gudang(
            id = 1,
            nama = "Gudang Utama",
            lokasi = "Jakarta",
            deskripsi = "Gudang pusat",
            createdAt = Date(),
            updatedAt = Date()
        )
        val gudang2 = Gudang(
            id = 2,
            nama = "Gudang Cabang",
            lokasi = "Bandung",
            deskripsi = "Gudang cabang",
            createdAt = Date(),
            updatedAt = Date()
        )

        gudangDao.insertGudang(gudang1)
        gudangDao.insertGudang(gudang2)

        val allGudang = gudangDao.getAllGudang().first()
        assertEquals(2, allGudang.size)
    }

    @Test
    fun `update gudang should work`() = runTest {
        val gudang = Gudang(
            id = 1,
            nama = "Gudang Utama",
            lokasi = "Jakarta",
            deskripsi = "Gudang pusat",
            createdAt = Date(),
            updatedAt = Date()
        )

        gudangDao.insertGudang(gudang)
        
        val updated = gudang.copy(
            nama = "Gudang Utama Updated",
            lokasi = "Jakarta Selatan"
        )
        gudangDao.updateGudang(updated)

        val retrieved = gudangDao.getGudangById(1)
        assertEquals("Gudang Utama Updated", retrieved?.nama)
        assertEquals("Jakarta Selatan", retrieved?.lokasi)
    }

    @Test
    fun `delete gudang should work`() = runTest {
        val gudang = Gudang(
            id = 1,
            nama = "Gudang Utama",
            lokasi = "Jakarta",
            deskripsi = "Gudang pusat",
            createdAt = Date(),
            updatedAt = Date()
        )

        gudangDao.insertGudang(gudang)
        gudangDao.deleteGudang(gudang)

        val retrieved = gudangDao.getGudangById(1)
        assertNull(retrieved)
    }
}
