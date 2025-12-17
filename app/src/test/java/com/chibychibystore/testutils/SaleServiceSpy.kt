package com.chibychibystore.testutils

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.SaleService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight test spy around [SaleService] that records invocation count and
 * provides a way to wait for the first invocation in tests.
 */
class SaleServiceSpy(private val delegate: SaleService) : SaleService by delegate {
    private val invocationLatch = CountDownLatch(1)
    private val _count = AtomicInteger(0)

    val invocationCount: Int
        get() = _count.get()

    suspend fun awaitInvocation(timeoutMs: Long = 2_000): Boolean {
        return invocationLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

    override suspend fun createSale(sale: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
        _count.incrementAndGet()
        try {
            return delegate.createSale(sale, items)
        } finally {
            // ensure latch is always counted down when the call completes
            invocationLatch.countDown()
        }
    }
}
