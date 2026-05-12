package com.chibychibystore.service
import org.robolectric.annotation.Config

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
import com.chibychibystore.service.impl.SaleServiceImpl
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.PromoService
import com.chibychibystore.data.local.database.ChibyChibyDatabase

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
        val authService = Mockito.mock(AuthService::class.java)
        val db = Mockito.mock(ChibyChibyDatabase::class.java)
        val stokGudangRepo = Mockito.mock(StokGudangRepository::class.java)
        val promoService = Mockito.mock(PromoService::class.java)

        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }

        val service = SaleServiceImpl(
            db,
            penjualanRepo,
            itemRepo,
            produkRepo,
            stokGudangRepo,
            authService,
            printer,
            promoService
        )

        val res = service.refundPenjualan(penjualanId)
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("sudah direfund") == true)
    }
}
