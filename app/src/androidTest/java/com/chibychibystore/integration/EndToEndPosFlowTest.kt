package com.chibychibystore.integration

import app.cash.turbine.test
import com.chibychibystore.service.ReportingService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.ProductService
import com.chibychibystore.testutils.TestDataBuilder
import com.chibychibystore.testutils.BaseIntegrationTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import javax.inject.Inject
import java.util.Date
import java.time.LocalDate

@HiltAndroidTest
class EndToEndPosFlowTest : BaseIntegrationTest() {

    @Inject
    lateinit var productService: ProductService

    @Inject
    lateinit var saleService: SaleService

    @Inject
    lateinit var reportingService: ReportingService

    override fun setupDatabase() {
        super.setupDatabase()
        // Additional seeding done in test as needed
    }

    @Test
    fun `pos flow creates sale updates inventory and appears in reports`() = runTest {
        // Given - create a fresh product with known stock
        val initialProduct = TestDataBuilder.createTestProduct().copy(stockQuantity = 10)
        val created = productService.createProduct(initialProduct).getOrNull()
        assertNotNull("Product creation should succeed", created)

        val productId = created!!.id
        val unitPrice = created.sellingPrice
        val quantity = 3
        val totalPrice = unitPrice * quantity

        val sale = com.chibychibystore.data.local.entity.Penjualan(
            saleDate = Date(),
            totalAmount = 0.0, // will be computed by service
            paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH,
            cashierId = 1L
        )

        val saleItem = com.chibychibystore.data.local.entity.ItemPenjualan(
            id = 0L,
            saleId = 0L,
            productId = productId,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = totalPrice
        )

        // When - create sale
        val createResult = saleService.createSale(sale, listOf(saleItem))
        assertTrue("Sale creation should succeed", createResult.isSuccess)
        val saleWithItems = createResult.getOrNull()
        assertNotNull(saleWithItems)
        assertTrue("Sale should have items", saleWithItems!!.items.isNotEmpty())

        // Then - inventory updated
        val updatedProduct = productService.getProduct(productId.toString()).getOrNull()
        assertNotNull(updatedProduct)
        assertEquals("Stock should be reduced by quantity", 10 - quantity, updatedProduct?.stockQuantity)

        // And - reporting reflects sale in date range
        val today = LocalDate.now()
        val gross = reportingService.getGrossSales(today.minusDays(1), today.plusDays(1))
        assertTrue("Reporting gross sales should succeed", gross.isSuccess)
        val report = gross.getOrNull()
        assertNotNull(report)
        assertTrue("Report total should include sale amount", report!!.totalSales >= totalPrice)
    }

    @Test
    fun `pos flow refund setsIsRefunded and prevents double refund`() = runTest {
        // Given - create a fresh product with known stock
        val initialProduct = TestDataBuilder.createTestProduct().copy(stockQuantity = 5)
        val created = productService.createProduct(initialProduct).getOrNull()
        assertNotNull("Product creation should succeed", created)

        val productId = created!!.id
        val unitPrice = created.sellingPrice
        val quantity = 2
        val totalPrice = unitPrice * quantity

        val sale = com.chibychibystore.data.local.entity.Penjualan(
            saleDate = Date(),
            totalAmount = 0.0,
            paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH,
            cashierId = 1L
        )

        val saleItem = com.chibychibystore.data.local.entity.ItemPenjualan(
            id = 0L,
            saleId = 0L,
            productId = productId,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = totalPrice
        )

        // When - create sale
        val createResult = saleService.createSale(sale, listOf(saleItem))
        assertTrue("Sale creation should succeed", createResult.isSuccess)
        val saleWithItems = createResult.getOrNull()
        assertNotNull(saleWithItems)
        val saleId = saleWithItems!!.penjualan.id

        // When - refund
        val refundRes = saleService.refundSale(saleId)
        assertTrue("Refund should succeed", refundRes.isSuccess)

        // Then - sale flagged as refunded and stock restored
        val fetched = saleService.getSale(saleId)
        assertTrue("getSale should succeed", fetched.isSuccess)
        val fetchedSale = fetched.getOrNull()!!
        assertTrue("Sale should be marked as refunded", fetchedSale.penjualan.isRefunded)

        val updatedProduct = productService.getProduct(productId.toString()).getOrNull()
        assertNotNull(updatedProduct)
        assertEquals("Stock should be restored after refund", 5, updatedProduct?.stockQuantity)

        // Second refund should fail
        val secondRefund = saleService.refundSale(saleId)
        assertTrue("Second refund should fail", secondRefund.isFailure)
    }
}

