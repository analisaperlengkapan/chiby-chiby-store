package com.chibychibystore.data.local.dao
import org.robolectric.annotation.Config

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
@Config(sdk = [34])
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
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date())

        gudangDao.insertGudang(gudang)
        val retrieved = gudangDao.getGudangById(1L)

        assertNotNull(retrieved)
        assertEquals("Gudang Utama", retrieved?.name)
        assertEquals("Jakarta", retrieved?.location)
    }

    @Test
    fun `get all gudang should return list`() = runTest {
        val gudang1 = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date())
        val gudang2 = Gudang(id = 2L, name = "Gudang Cabang", location = "Bandung", capacity = 50, createdAt = Date())

        gudangDao.insertGudang(gudang1)
        gudangDao.insertGudang(gudang2)

        val allGudang = gudangDao.getAllGudang().first()
        assertEquals(2, allGudang.size)
    }

    @Test
    fun `update gudang should work`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date())

        gudangDao.insertGudang(gudang)
        
        val updated = gudang.copy(
            name = "Gudang Utama Updated",
            location = "Jakarta Selatan"
        )
        gudangDao.updateGudang(updated)

        val retrieved = gudangDao.getGudangById(1L)
        assertEquals("Gudang Utama Updated", retrieved?.name)
        assertEquals("Jakarta Selatan", retrieved?.location)
    }

    @Test
    fun `delete gudang should work`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date())

        gudangDao.insertGudang(gudang)
        gudangDao.deleteGudangById(1L)

        val retrieved = gudangDao.getGudangById(1L)
        assertNull(retrieved)
    }
}
