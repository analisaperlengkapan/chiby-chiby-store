package com.chibychibystore.service
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.service.impl.CashManagementServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
import java.time.LocalDate
class CashManagementServiceTest {
    @Test
    fun getCashFlowSummarySuccess() = runTest {
        val pr: PenjualanRepository = mock()
        val er: PengeluaranRepository = mock()
        whenever(pr.getTotalRevenue(any(), any())).thenReturn(Result.success(100.0))
        whenever(er.getApprovedRingkasanPengeluaranPerKategoriResult(any(), any())).thenReturn(Result.success(emptyMap()))
        val service = CashManagementServiceImpl(mock(), pr, er, mock(), mock())
        val res = service.getCashFlowSummary(LocalDate.now(), LocalDate.now())
        Assert.assertEquals(100.0, (res as Result.Success).data.netCashFlow, 0.0)
    }
}
