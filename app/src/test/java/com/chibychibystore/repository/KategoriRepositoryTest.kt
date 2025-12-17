package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.KategoriDao
import com.chibychibystore.data.local.entity.Kategori
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Test untuk KategoriRepository
 */
class KategoriRepositoryTest {

    @Mock
    private lateinit var kategoriDao: KategoriDao

    private lateinit var repository: KategoriRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = KategoriRepository(kategoriDao)
    }

    @Test
    fun `getAllKategori should return flow from dao`() = runTest {
        val kategoriList = listOf(
            Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date()),
            Kategori(id = 2L, name = "Makanan", description = "Produk makanan", createdAt = Date())
        )
        whenever(kategoriDao.getAllKategori()).thenReturn(flowOf(kategoriList))

        val result = repository.getAllKategori()
        
        assertNotNull(result)
        verify(kategoriDao).getAllKategori()
    }

    @Test
    fun `getKategoriById should return kategori from dao`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        whenever(kategoriDao.getKategoriById(1L)).thenReturn(kategori)

        val result = repository.getKategoriById(1L)
        
        assertTrue(result.isSuccess)
        assertEquals(kategori, result.getOrNull())
        verify(kategoriDao).getKategoriById(1L)
    }

    @Test
    fun `createKategori should call dao insert and return id`() = runTest {
        val kategori = Kategori(id = 0L, name = "New Kategori", description = "Desc", createdAt = Date())
        whenever(kategoriDao.insertKategori(kategori)).thenReturn(5L)

        val result = repository.createKategori(kategori)

        assertTrue(result.isSuccess)
        assertEquals(5L, result.getOrNull())
        verify(kategoriDao).insertKategori(kategori)
    }

    @Test
    fun `updateKategori should call dao update`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        whenever(kategoriDao.getKategoriById(1L)).thenReturn(kategori)
        whenever(kategoriDao.getKategoriByName(kategori.name)).thenReturn(kategori)

        val result = repository.updateKategori(kategori)

        assertTrue(result.isSuccess)
        verify(kategoriDao).updateKategori(kategori)
    }

    @Test
    fun `deleteKategori should call dao delete`() = runTest {
        val kategori = Kategori(id = 1L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        whenever(kategoriDao.getKategoriById(1L)).thenReturn(kategori)

        val result = repository.deleteKategori(1L)

        assertTrue(result.isSuccess)
        verify(kategoriDao).deleteKategoriById(1L)
    }
}
