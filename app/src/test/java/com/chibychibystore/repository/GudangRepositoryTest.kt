package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.GudangDao
import com.chibychibystore.data.local.entity.Gudang
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

    private lateinit var repository: GudangRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GudangRepository(gudangDao)
    }

    @Test
    fun `getAllGudang should return flow from dao`() = runTest {
        val gudangList = listOf(
            Gudang(1, "Gudang Utama", "Jakarta", "Gudang pusat", Date(), Date()),
            Gudang(2, "Gudang Cabang", "Bandung", "Gudang cabang", Date(), Date())
        )
        whenever(gudangDao.getAllGudang()).thenReturn(flowOf(gudangList))

        val result = repository.getAllGudang()
        
        assertNotNull(result)
        verify(gudangDao).getAllGudang()
    }

    @Test
    fun `getGudangById should return gudang from dao`() = runTest {
        val gudang = Gudang(1, "Gudang Utama", "Jakarta", "Gudang pusat", Date(), Date())
        whenever(gudangDao.getGudangById(1)).thenReturn(gudang)

        val result = repository.getGudangById(1)
        
        assertEquals(gudang, result)
        verify(gudangDao).getGudangById(1)
    }

    @Test
    fun `insertGudang should call dao insert`() = runTest {
        val gudang = Gudang(1, "Gudang Utama", "Jakarta", "Gudang pusat", Date(), Date())

        repository.insertGudang(gudang)
        
        verify(gudangDao).insertGudang(gudang)
    }

    @Test
    fun `updateGudang should call dao update`() = runTest {
        val gudang = Gudang(1, "Gudang Utama", "Jakarta", "Gudang pusat", Date(), Date())

        repository.updateGudang(gudang)
        
        verify(gudangDao).updateGudang(gudang)
    }

    @Test
    fun `deleteGudang should call dao delete`() = runTest {
        val gudang = Gudang(1, "Gudang Utama", "Jakarta", "Gudang pusat", Date(), Date())

        repository.deleteGudang(gudang)
        
        verify(gudangDao).deleteGudang(gudang)
    }
}
