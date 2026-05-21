package com.chibychibystore.repository
import com.chibychibystore.data.local.dao.*
import com.chibychibystore.data.local.entity.Kategori
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
class KategoriRepositoryTest {
    @Test
    fun getAllKategoriSuccess() = runTest {
        val dao: KategoriDao = mock()
        val repo = KategoriRepository(dao, mock())
        whenever(dao.getAllKategori()).thenReturn(flowOf(listOf(Kategori(name = "C"))))
        val res = repo.getAllKategori()
        Assert.assertNotNull(res)
    }
}
