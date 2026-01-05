package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.GudangDao
import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.error.ChibyChibyException
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
 * Test untuk GudangRepository
 */
class GudangRepositoryTest {

    @Mock
    private lateinit var gudangDao: GudangDao

    @Mock
    private lateinit var produkDao: ProdukDao

    private lateinit var repository: GudangRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GudangRepository(gudangDao, produkDao)
    }

    @Test
    fun `getAllGudang should return flow from dao`() = runTest {
        val gudangList = listOf(
            Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date()),
            Gudang(id = 2L, name = "Gudang Cabang", location = "Bandung", capacity = 50, createdAt = Date())
        )
        whenever(gudangDao.getAllGudang()).thenReturn(flowOf(gudangList))

        val result = repository.getAllGudang()

        assertNotNull(result)
        verify(gudangDao).getAllGudang()
    }

    @Test
    fun `getGudangById should return gudang from dao`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 100, createdAt = Date())
        whenever(gudangDao.getGudangById(1L)).thenReturn(gudang)

        val result = repository.getGudangById(1L)

        assertTrue(result.isSuccess)
        assertEquals(gudang, result.getOrNull())
        verify(gudangDao).getGudangById(1L)
    }

    @Test
    fun `createGudang should call dao insert and return id`() = runTest {
        val gudang = Gudang(id = 0L, name = "Gudang Baru", location = "Jakarta", capacity = 20, createdAt = Date())
        whenever(gudangDao.insertGudang(gudang)).thenReturn(10L)

        val result = repository.createGudang(gudang)

        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())
        verify(gudangDao).insertGudang(gudang)
    }

    @Test
    fun `updateGudang should call dao update`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 120, createdAt = Date())
        whenever(gudangDao.getGudangById(1L)).thenReturn(gudang)
        whenever(gudangDao.getGudangByName(gudang.name)).thenReturn(gudang)

        val result = repository.updateGudang(gudang)

        assertTrue(result.isSuccess)
        verify(gudangDao).updateGudang(gudang)
    }

    @Test
    fun `deleteGudang should call dao delete`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 120, createdAt = Date())
        whenever(gudangDao.getGudangById(1L)).thenReturn(gudang)
        whenever(produkDao.countProdukByGudang(1L)).thenReturn(0)

        val result = repository.deleteGudang(1L)

        assertTrue(result.isSuccess)
        verify(gudangDao).deleteGudangById(1L)
    }

    @Test
    fun `deleteGudang should return failure when gudang has products`() = runTest {
        val gudang = Gudang(id = 1L, name = "Gudang Utama", location = "Jakarta", capacity = 120, createdAt = Date())
        whenever(gudangDao.getGudangById(1L)).thenReturn(gudang)
        whenever(produkDao.countProdukByGudang(1L)).thenReturn(5)

        val result = repository.deleteGudang(1L)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.BusinessLogicError)
        assertEquals("Pelanggaran aturan bisnis: Gudang tidak dapat dihapus karena masih digunakan oleh produk", exception?.message)

        // Ensure delete is NOT called
        org.mockito.kotlin.verify(gudangDao, org.mockito.kotlin.never()).deleteGudangById(1L)
    }
}
