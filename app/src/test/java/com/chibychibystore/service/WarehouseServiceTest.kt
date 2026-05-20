package com.chibychibystore.service
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.service.impl.WarehouseServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
class WarehouseServiceTest {
    @Test
    fun createGudangSuccess() = runTest {
        val repo: GudangRepository = mock()
        val auth: AuthService = mock()
        whenever(auth.hasPermission(any())).thenReturn(true)
        val g = Gudang(name = "G")
        whenever(repo.createGudang(any())).thenReturn(Result.success(1L))
        whenever(repo.getGudangById(1L)).thenReturn(Result.success(g.copy(id = 1L)))
        val service = WarehouseServiceImpl(repo, mock(), mock(), auth, mock())
        val res = service.createGudang(g)
        Assert.assertEquals(1L, (res as Result.Success).data.id)
    }
}
