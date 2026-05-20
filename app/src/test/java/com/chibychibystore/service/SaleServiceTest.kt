package com.chibychibystore.service
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import com.chibychibystore.service.impl.SaleServiceImpl
import org.junit.*
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking
import java.util.Date
class SaleServiceTest {
    private lateinit var saleService: SaleService
    @Before
    fun setup() {
        val db: ChibyChibyDatabase = mock()
        whenever(db.pelangganDao()).thenReturn(mock())
        whenever(db.produkDao()).thenReturn(mock())
        saleService = SaleServiceImpl(db, mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock())
    }
    @Test
    fun createPenjualanEmptyItems() = runBlocking {
        val res = saleService.createPenjualan(Penjualan(saleDate = Date(), totalAmount = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L), emptyList())
        Assert.assertTrue(res is Result.Failure)
    }
}
