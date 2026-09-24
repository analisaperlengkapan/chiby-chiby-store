package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PemasokRepositoryTest {

    private lateinit var repository: PemasokRepository
    private lateinit var pemasokDao: PemasokDao
    private lateinit var pembelianDao: PembelianDao

    @Before
    fun setup() {
        pemasokDao = mock()
        pembelianDao = mock()
        repository = PemasokRepository(pemasokDao, pembelianDao)
    }

    @Test
    fun `deletePemasok success when not used in purchases`() = runTest {
        val pemasokId = 1L
        whenever(pemasokDao.getPemasokById(pemasokId)).thenReturn(Pemasok(id = pemasokId, name = "Vendor A"))
        whenever(pembelianDao.countPembelianByPemasok(pemasokId)).thenReturn(0)
        whenever(pemasokDao.deletePemasokById(pemasokId)).thenReturn(Unit)

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result.isSuccess)
        verify(pemasokDao).deletePemasokById(pemasokId)
    }

    @Test
    fun `deletePemasok failure when used in purchases`() = runTest {
        val pemasokId = 1L
        whenever(pemasokDao.getPemasokById(pemasokId)).thenReturn(Pemasok(id = pemasokId, name = "Vendor A"))
        whenever(pembelianDao.countPembelianByPemasok(pemasokId)).thenReturn(5)

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ChibyChibyException.BusinessLogicError
        assertTrue(exception.message!!.contains("Pemasok tidak dapat dihapus karena memiliki riwayat pembelian"))
        verify(pemasokDao, never()).deletePemasokById(any())
    }

    @Test
    fun `deletePemasok failure when pemasok not found`() = runTest {
        val pemasokId = 1L
        whenever(pemasokDao.getPemasokById(pemasokId)).thenReturn(null)

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ChibyChibyException.DatabaseError
        assertEquals("Error database saat Pemasok tidak ditemukan", exception.message)
    }
}
