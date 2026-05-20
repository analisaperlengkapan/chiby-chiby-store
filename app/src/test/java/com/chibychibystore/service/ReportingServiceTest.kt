package com.chibychibystore.service
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.service.impl.ReportingServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
import java.time.LocalDate
class ReportingServiceTest {
    @Test
    fun getGrossSalesSuccess() = runTest {
        val pr: PenjualanRepository = mock()
        val auth: AuthService = mock()
        whenever(auth.hasPermission(any())).thenReturn(true)
        whenever(pr.getTotalCashReceipts(any(), any())).thenReturn(Result.success(100.0))
        whenever(pr.getPenjualanCountNonRefunded(any(), any())).thenReturn(Result.success(5))
        val rs = ReportingServiceImpl(pr, mock(), mock(), mock(), mock(), mock(), mock(), auth, mock())
        val res = rs.getGrossSales(LocalDate.now(), LocalDate.now())
        Assert.assertEquals(100.0, (res as Result.Success).data.totalPenjualan, 0.0)
    }
}
