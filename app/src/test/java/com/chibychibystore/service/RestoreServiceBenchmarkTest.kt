package com.chibychibystore.service

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import org.junit.Test
import java.util.Date
import kotlin.system.measureTimeMillis

class RestoreServiceBenchmarkTest {

    @Test
    fun benchmarkFiltering() {
        // Setup Data
        val salesCount = 2000
        val itemsPerSale = 5
        val sales = (1..salesCount).map { id ->
            Penjualan(
                id = id.toLong(),
                saleDate = Date(),
                totalAmount = 100.0,
                paymentMethod = PaymentMethod.CASH,
                cashierId = 1L
            )
        }
        val items = sales.flatMap { sale ->
            (1..itemsPerSale).map { itemId ->
                ItemPenjualan(
                    id = (sale.id * 1000 + itemId),
                    saleId = sale.id,
                    productId = itemId.toLong(),
                    quantity = 1,
                    unitPrice = 20.0,
                    totalPrice = 20.0
                )
            }
        }

        println("Benchmarking with ${sales.size} sales and ${items.size} items.")

        // Current Implementation (O(N*M))
        val currentTime = measureTimeMillis {
            var matchCount = 0
            for (sale in sales) {
                val saleItems = items.filter { it.saleId == sale.id }
                matchCount += saleItems.size
            }
            println("Current Impl matched items: $matchCount")
        }
        println("Current Implementation Time: ${currentTime}ms")

        // Optimized Implementation (O(M))
        val optimizedTime = measureTimeMillis {
            var matchCount = 0
            val itemsMap = items.groupBy { it.saleId }
            for (sale in sales) {
                val saleItems = itemsMap[sale.id] ?: emptyList()
                matchCount += saleItems.size
            }
            println("Optimized Impl matched items: $matchCount")
        }
        println("Optimized Implementation Time: ${optimizedTime}ms")

        val improvement = currentTime.toDouble() / optimizedTime.toDouble()
        println("Speedup: ${String.format("%.2f", improvement)}x")
    }
}
