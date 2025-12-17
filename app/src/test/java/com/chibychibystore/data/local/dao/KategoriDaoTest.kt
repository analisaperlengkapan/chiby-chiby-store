package com.chibychibystore.data.local.dao

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
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )

        kategoriDao.insertKategori(kategori)
        val retrieved = kategoriDao.getKategoriById(1)

        assertNotNull(retrieved)
        assertEquals("Elektronik", retrieved?.nama)
        assertEquals("Produk elektronik", retrieved?.deskripsi)
    }

    @Test
    fun `get all kategori should return list`() = runTest {
        val kategori1 = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        val kategori2 = Kategori(
            id = 2,
            nama = "Makanan",
            deskripsi = "Produk makanan",
            createdAt = Date(),
            updatedAt = Date()
        )

        kategoriDao.insertKategori(kategori1)
        kategoriDao.insertKategori(kategori2)

        val allKategori = kategoriDao.getAllKategori().first()
        assertEquals(2, allKategori.size)
    }

    @Test
    fun `update kategori should work`() = runTest {
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )

        kategoriDao.insertKategori(kategori)
        
        val updated = kategori.copy(
            nama = "Elektronik Updated",
            deskripsi = "Deskripsi baru"
        )
        kategoriDao.updateKategori(updated)

        val retrieved = kategoriDao.getKategoriById(1)
        assertEquals("Elektronik Updated", retrieved?.nama)
        assertEquals("Deskripsi baru", retrieved?.deskripsi)
    }

    @Test
    fun `delete kategori should work`() = runTest {
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )

        kategoriDao.insertKategori(kategori)
        kategoriDao.deleteKategori(kategori)

        val retrieved = kategoriDao.getKategoriById(1)
        assertNull(retrieved)
    }
}
