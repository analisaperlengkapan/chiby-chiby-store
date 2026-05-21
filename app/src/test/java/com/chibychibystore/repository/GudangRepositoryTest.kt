package com.chibychibystore.repository
import com.chibychibystore.data.local.dao.*
import com.chibychibystore.data.local.entity.Gudang
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
class GudangRepositoryTest {
    @Test
    fun getAllGudangSuccess() = runTest {
        val dao: GudangDao = mock()
        val repo = GudangRepository(dao, mock())
        whenever(dao.getAllGudang()).thenReturn(flowOf(listOf(Gudang(name = "G"))))
        val res = repo.getAllGudang()
        Assert.assertNotNull(res)
    }
}
