package com.chibychibystore.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.Date
import com.chibychibystore.service.impl.SaleServiceImpl

@RunWith(RobolectricTestRunner::class)
class SaleServiceUnitTest {

    private lateinit var db: ChibyChibyDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun refund_onAlreadyRefundedSale_returnsFailure() = runBlocking {
        val penjualanId = 99L
        val penjualanRepo = Mockito.mock(PenjualanRepository::class.java)
        val itemRepo = Mockito.mock(ItemPenjualanRepository::class.java)
        val produkRepo = Mockito.mock(ProdukRepository::class.java)
        val printer = Mockito.mock(PrinterService::class.java)
        val authService = Mockito.mock(AuthService::class.java)
        val stokGudangRepo = Mockito.mock(StokGudangRepository::class.java)
        val promoService = Mockito.mock(PromoService::class.java)
        val shiftRepo = Mockito.mock(com.chibychibystore.repository.ShiftRepository::class.java)

        Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        Mockito.`when`(promoService.calculateDiscount(Mockito.anyDouble())).thenReturn(0.0)

        val alreadyRefunded = Penjualan(
            id = penjualanId,
            saleDate = Date(),
            totalAmount = 100.0,
            paymentMethod = PaymentMethod.CASH,
            cashierId = 1,
            isRefunded = true
        )
        Mockito.`when`(penjualanRepo.getPenjualanWithItemsById(penjualanId))
            .thenReturn(com.chibychibystore.data.model.Result.success(PenjualanWithItems(alreadyRefunded, emptyList())))

        val service = SaleServiceImpl(
            db,
            penjualanRepo,
            itemRepo,
            produkRepo,
            stokGudangRepo,
            shiftRepo,
            authService,
            printer,
            promoService
        )

        val res = service.refundPenjualan(penjualanId)
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("sudah di-refund") == true)
    }
}
