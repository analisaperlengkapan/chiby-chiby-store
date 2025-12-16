package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

class SaleServiceUnitTest {

    @Test
    fun refund_onAlreadyRefundedSale_returnsFailure() = runBlocking {
        val penjualanId = 99L
        val penjualan = Penjualan(
            id = penjualanId,
            saleDate = Date(),
            totalAmount = 100.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = 1,
            isRefunded = true
        )

        val penjualanRepo = Mockito.mock(PenjualanRepository::class.java)
        // Mock suspend call inside runBlocking
        runBlocking {
            Mockito.`when`(penjualanRepo.getPenjualanById(penjualanId)).thenReturn(Result.success(penjualan))
        }

        val itemRepo = Mockito.mock(ItemPenjualanRepository::class.java)
        val produkRepo = Mockito.mock(ProdukRepository::class.java)
        val printer = Mockito.mock(PrinterService::class.java)

        val service = SaleServiceImpl(penjualanRepo, itemRepo, produkRepo, printer)

        val res = service.refundSale(penjualanId)
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex!!.message!!.contains("sudah direfund"))
    }
}
