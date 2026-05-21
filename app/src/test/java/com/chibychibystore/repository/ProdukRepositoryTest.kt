package com.chibychibystore.repository
import com.chibychibystore.data.local.dao.*
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
class ProdukRepositoryTest {
    @Test
    fun getProdukByIdSuccess() = runTest {
        val dao: ProdukDao = mock()
        val repo = ProdukRepository(dao)
        val p = Produk(id = 1L, name = "P", costPrice = 1.0, sellingPrice = 2.0, categoryId = 1L, warehouseId = 1L)
        whenever(dao.getProdukById(1L)).thenReturn(p)
        val res = repo.getProdukById(1L)
        Assert.assertEquals(p, (res as Result.Success).data)
    }
}
