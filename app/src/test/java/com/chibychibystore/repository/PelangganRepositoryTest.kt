package com.chibychibystore.repository
import com.chibychibystore.data.local.dao.PelangganDao
import com.chibychibystore.data.local.entity.Pelanggan
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
class PelangganRepositoryTest {
    @Test
    fun getAllPelangganSuccess() = runTest {
        val dao: PelangganDao = mock()
        val repo = PelangganRepository(dao)
        whenever(dao.getAllPelanggan()).thenReturn(flowOf(listOf(Pelanggan(name = "P"))))
        val res = repo.getAllPelanggan()
        Assert.assertNotNull(res)
    }
}
