package com.chibychibystore.data.local.dao
import org.robolectric.annotation.Config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Kategori
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
 * Test untuk KategoriDao
 * 
 * Memastikan operasi CRUD pada tabel kategori berfungsi dengan benar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KategoriDaoTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var kategoriDao: KategoriDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()
        
        kategoriDao = database.kategoriDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and get kategori should work`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())

        kategoriDao.insertKategori(kategori)
        val retrieved = kategoriDao.getKategoriById(1L)

        assertNotNull(retrieved)
        assertEquals("Elektronik", retrieved?.name)
        assertEquals("Produk elektronik", retrieved?.description)
    }

    @Test
    fun `get all kategori should return list`() = runTest {
        val kategori1 = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        val kategori2 = Kategori(id = 2L, name = "Makanan", description = "Produk makanan", createdAt = Date())

        kategoriDao.insertKategori(kategori1)
        kategoriDao.insertKategori(kategori2)

        val allKategori = kategoriDao.getAllKategori().first()
        assertEquals(2, allKategori.size)
    }

    @Test
    fun `update kategori should work`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())

        kategoriDao.insertKategori(kategori)
        
        val updated = kategori.copy(
            name = "Elektronik Updated",
            description = "Deskripsi baru"
        )
        kategoriDao.updateKategori(updated)

        val retrieved = kategoriDao.getKategoriById(1L)
        assertEquals("Elektronik Updated", retrieved?.name)
        assertEquals("Deskripsi baru", retrieved?.description)
    }

    @Test
    fun `delete kategori should work`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())

        kategoriDao.insertKategori(kategori)
        kategoriDao.deleteKategoriById(1L)

        val retrieved = kategoriDao.getKategoriById(1L)
        assertNull(retrieved)
    }
}
