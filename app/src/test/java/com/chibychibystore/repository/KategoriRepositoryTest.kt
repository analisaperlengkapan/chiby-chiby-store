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
            Kategori(1, "Elektronik", "Produk elektronik", Date(), Date()),
            Kategori(2, "Makanan", "Produk makanan", Date(), Date())
        )
        whenever(kategoriDao.getAllKategori()).thenReturn(flowOf(kategoriList))

        val result = repository.getAllKategori()
        
        assertNotNull(result)
        verify(kategoriDao).getAllKategori()
    }

    @Test
    fun `getKategoriById should return kategori from dao`() = runTest {
        val kategori = Kategori(1, "Elektronik", "Produk elektronik", Date(), Date())
        whenever(kategoriDao.getKategoriById(1)).thenReturn(kategori)

        val result = repository.getKategoriById(1)
        
        assertEquals(kategori, result)
        verify(kategoriDao).getKategoriById(1)
    }

    @Test
    fun `insertKategori should call dao insert`() = runTest {
        val kategori = Kategori(1, "Elektronik", "Produk elektronik", Date(), Date())

        repository.insertKategori(kategori)
        
        verify(kategoriDao).insertKategori(kategori)
    }

    @Test
    fun `updateKategori should call dao update`() = runTest {
        val kategori = Kategori(1, "Elektronik", "Produk elektronik", Date(), Date())

        repository.updateKategori(kategori)
        
        verify(kategoriDao).updateKategori(kategori)
    }

    @Test
    fun `deleteKategori should call dao delete`() = runTest {
        val kategori = Kategori(1, "Elektronik", "Produk elektronik", Date(), Date())

        repository.deleteKategori(kategori)
        
        verify(kategoriDao).deleteKategori(kategori)
    }
}
