package com.chibychibystore.service
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.service.impl.ExpenseServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
import java.util.*
class ExpenseServiceTest {
    @Test
    fun createPengeluaranSuccess() = runTest {
        val repo: PengeluaranRepository = mock()
        val service = ExpenseServiceImpl(repo)
        val p = Pengeluaran(expenseDate = Date(), category = KategoriPengeluaran.UTILITIES, amount = 10.0, createdBy = 1L)
        whenever(repo.createPengeluaran(any())).thenReturn(Result.success(1L))
        whenever(repo.getPengeluaranById(1L)).thenReturn(Result.success(p.copy(id = 1L)))
        val res = service.createPengeluaran(p)
        Assert.assertEquals(1L, (res as Result.Success).data.id)
    }
}
