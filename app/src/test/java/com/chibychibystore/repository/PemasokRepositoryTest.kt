package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PemasokRepositoryTest {

    private lateinit var repository: PemasokRepository
    private lateinit var pemasokDao: PemasokDao
    private lateinit var pembelianDao: PembelianDao

    @Before
    fun setup() {
        pemasokDao = mockk(relaxed = true)
        pembelianDao = mockk(relaxed = true)
        repository = PemasokRepository(pemasokDao, pembelianDao)
    }

    @Test
    fun `deletePemasok success when not used in purchases`() = runTest {
        val pemasokId = 1L
        coEvery { pemasokDao.getPemasokById(pemasokId) } returns Pemasok(id = pemasokId, name = "Vendor A")
        coEvery { pembelianDao.countPembelianByPemasok(pemasokId) } returns 0
        coEvery { pemasokDao.deletePemasokById(pemasokId) } returns Unit

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result is Result.Success)
        coVerify { pemasokDao.deletePemasokById(pemasokId) }
    }

    @Test
    fun `deletePemasok failure when used in purchases`() = runTest {
        val pemasokId = 1L
        coEvery { pemasokDao.getPemasokById(pemasokId) } returns Pemasok(id = pemasokId, name = "Vendor A")
        coEvery { pembelianDao.countPembelianByPemasok(pemasokId) } returns 5

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result is Result.Failure)
        val exception = (result as Result.Failure).exception as ChibyChibyException.DatabaseError
        assertEquals("Pemasok tidak dapat dihapus karena memiliki riwayat pembelian", exception.message)
        coVerify(exactly = 0) { pemasokDao.deletePemasokById(any()) }
    }

    @Test
    fun `deletePemasok failure when pemasok not found`() = runTest {
        val pemasokId = 1L
        coEvery { pemasokDao.getPemasokById(pemasokId) } returns null

        val result = repository.deletePemasok(pemasokId)

        assertTrue(result is Result.Failure)
        val exception = (result as Result.Failure).exception as ChibyChibyException.DatabaseError
        assertEquals("Pemasok tidak ditemukan", exception.message)
    }
}
