package com.chibychibystore.testutils

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.SaleService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SaleServiceSpyTest {
    private class DelayedSaleService : SaleService {
        override suspend fun createPenjualan(sale: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
            delay(100)
            val created = PenjualanWithItems(sale.copy(totalAmount = items.sumOf { it.totalPrice }), items)
            return Result.success(created)
        }

        // Minimal implementations for other interface members
        override suspend fun getPenjualanById(id: Long) = Result.success(null as PenjualanWithItems?)
        override suspend fun getOpenShift(kasirId: Long) = Result.success(null as com.chibychibystore.data.local.entity.Shift?)
        override suspend fun getPenjualanByRentangTanggal(startDate: String?, endDate: String?, cashierId: Long?, query: String?) = Result.success(emptyList<Penjualan>())
        override suspend fun getRecentPenjualan(limit: Int) = Result.success(emptyList<Penjualan>())
        override suspend fun searchPenjualan(query: String) = Result.success(emptyList<Penjualan>())
        override suspend fun updatePenjualan(id: Long, sale: Penjualan) = Result.failure(Exception("not implemented"))
        override suspend fun deletePenjualan(id: Long) = Result.failure(Exception("not implemented"))
        override suspend fun refundPenjualan(id: Long) = Result.failure(Exception("not implemented"))
        override suspend fun cancelPenjualan(id: Long) = Result.failure(Exception("not implemented"))
        override suspend fun getTotalPenjualanByRentangTanggal(startDate: String, endDate: String) = Result.success(0.0)
        override suspend fun getPenjualanCountByRentangTanggal(startDate: String, endDate: String) = Result.success(0)
        override fun observePenjualan() = kotlinx.coroutines.flow.flowOf<List<Penjualan>>(emptyList())
        override fun observePenjualanWithItems() = kotlinx.coroutines.flow.flowOf<List<PenjualanWithItems>>(emptyList())
        override fun observePenjualanFiltered(startDate: String, endDate: String, query: String?) = kotlinx.coroutines.flow.flowOf<List<Penjualan>>(emptyList())
        override suspend fun cetakStruk(saleId: Long, storeName: String, storeAddress: String, cashierName: String) = Result.failure(Exception("not implemented"))
        override fun observePenjualanWithItemsByRentangTanggal(startDate: String, endDate: String) = kotlinx.coroutines.flow.flowOf<List<PenjualanWithItems>>(emptyList())
    }

    @Test
    fun `spy records invocation and await works`() = runTest {
        val delegate = DelayedSaleService()
        val spy = SaleServiceSpy(delegate)

        val sale = Penjualan(saleDate = java.util.Date(), totalAmount = 0.0, paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH, cashierId = 1L)
        val item = ItemPenjualan(id = 0L, saleId = 0L, productId = 1L, quantity = 1, unitPrice = 100.0, totalPrice = 100.0)

        // Start invocation in background
        // Start invocation in background on a separate thread
        val thread = Thread {
            runBlocking { spy.createPenjualan(sale, listOf(item)) }
        }
        thread.start()

        // Await invocation
        val invoked = spy.awaitInvocation(1_000)
        assertTrue("Expected spy to observe invocation", invoked)
        assertEquals(1, spy.invocationCount)

        thread.join()
    }
}
