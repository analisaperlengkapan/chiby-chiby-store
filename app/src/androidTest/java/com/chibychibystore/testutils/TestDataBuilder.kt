package com.chibychibystore.testutils

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

/**
 * Test data builders for integration tests
 */
object TestDataBuilder {

    fun createTestCategory(
        id: Long = 0L,
        name: String = "Test Category",
        description: String = "Test category description"
    ) = Kategori(
        id = id,
        name = name,
        description = description
    )

    fun createTestWarehouse(
        id: Long = 0L,
        name: String = "Test Warehouse",
        location: String = "Test Location",
        capacity: Int = 1000
    ) = Gudang(
        id = id,
        name = name,
        location = location,
        capacity = capacity
    )

    fun createTestProduct(
        id: Long = 0L,
        name: String = "Test Product",
        barcode: String = "123456789012",
        categoryId: Long = 1L,
        costPrice: Double = 10000.0,
        sellingPrice: Double = 15000.0,
        stockQuantity: Int = 50,
        warehouseId: Long = 1L,
        minStock: Int = 10
    ) = Produk(
        id = id,
        name = name,
        barcode = barcode,
        categoryId = categoryId,
        costPrice = costPrice,
        sellingPrice = sellingPrice,
        stockQuantity = stockQuantity,
        warehouseId = warehouseId,
        minStock = minStock
    )

    fun createLowStockProduct(
        id: Long = 0L,
        name: String = "Low Stock Product",
        stockQuantity: Int = 2,
        minStock: Int = 10
    ) = createTestProduct(
        id = id,
        name = name,
        stockQuantity = stockQuantity,
        minStock = minStock
    )

    fun createOutOfStockProduct(
        id: Long = 0L,
        name: String = "Out of Stock Product",
        stockQuantity: Int = 0
    ) = createTestProduct(
        id = id,
        name = name,
        stockQuantity = stockQuantity
    )

    fun createTestSale(
        id: Long = 0L,
        date: LocalDateTime = LocalDateTime.now().minusDays(1),
        totalAmount: Double = 22500.0,
        paymentMethod: String = "CASH",
        cashierId: Long = 1L
    ) = Penjualan(
        id = id,
        saleDate = Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()),
        totalAmount = totalAmount,
        paymentMethod = if (paymentMethod == "CASH") com.chibychibystore.data.local.entity.PaymentMethod.CASH else com.chibychibystore.data.local.entity.PaymentMethod.CARD,
        cashierId = cashierId
    )

    fun createTestSaleItem(
        id: Long = 0L,
        saleId: Long = 0L,
        productId: Long = 0L,
        quantity: Int = 3,
        unitPrice: Double = 7500.0,
        totalPrice: Double = 22500.0
    ) = ItemPenjualan(
        id = id,
        saleId = saleId,
        productId = productId,
        quantity = quantity,
        unitPrice = unitPrice,
        totalPrice = totalPrice
    )

    fun createTestExpense(
        id: Long = 0L,
        date: LocalDateTime = LocalDateTime.now().minusDays(2),
        category: com.chibychibystore.data.local.entity.ExpenseCategory = com.chibychibystore.data.local.entity.ExpenseCategory.UTILITIES,
        amount: Double = 50000.0,
        description: String = "Electricity bill",
        approvedBy: Long? = null,
        createdBy: Long = 1L
    ) = Pengeluaran(
        id = id,
        expenseDate = java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()),
        category = category,
        amount = amount,
        description = description,
        approvedBy = approvedBy,
        createdBy = createdBy
    )

    // Test data collections
    val testCategories = listOf(
        createTestCategory(1L, "Food", "Food products"),
        createTestCategory(2L, "Beverages", "Drink products"),
        createTestCategory(3L, "Electronics", "Electronic products")
    )

    val testWarehouses = listOf(
        createTestWarehouse(1L, "Main Warehouse", "Jakarta", 1000),
        createTestWarehouse(2L, "Branch Warehouse", "Bandung", 500)
    )

    val testProducts = listOf(
        createTestProduct(1L, "Apple", "111111111111", 1L, 5000.0, 7500.0, 100, 1L, 20),
        createTestProduct(2L, "Orange", "222222222222", 1L, 4000.0, 6000.0, 80, 1L, 15),
        createTestProduct(3L, "Coca Cola", "333333333333", 2L, 3000.0, 4500.0, 200, 2L, 30),
        createTestProduct(4L, "Sprite", "444444444444", 2L, 3000.0, 4500.0, 150, 2L, 25),
        createLowStockProduct(5L, "Low Stock Item", 5, 20),
        createOutOfStockProduct(6L, "Out of Stock Item")
    )

    val testSales = listOf(
        createTestSale(1L, LocalDateTime.now().minusDays(1), 22500.0, "CASH", 1L),
        createTestSale(2L, LocalDateTime.now().minusDays(2), 13500.0, "CASH", 2L),
        createTestSale(3L, LocalDateTime.now().minusDays(3), 30000.0, "CARD", 1L),
        createTestSale(4L, LocalDateTime.now().minusDays(4), 18000.0, "CASH", 2L),
        createTestSale(5L, LocalDateTime.now().minusDays(5), 42000.0, "CARD", 1L)
    )

    val testSaleItems = listOf(
        createTestSaleItem(1L, 1L, 1L, 3, 7500.0, 22500.0),
        createTestSaleItem(2L, 2L, 2L, 2, 6000.0, 12000.0),
        createTestSaleItem(3L, 2L, 3L, 1, 4500.0, 1500.0),
        createTestSaleItem(4L, 3L, 4L, 4, 4500.0, 18000.0),
        createTestSaleItem(5L, 3L, 1L, 2, 7500.0, 15000.0),
        createTestSaleItem(6L, 4L, 2L, 3, 6000.0, 18000.0),
        createTestSaleItem(7L, 5L, 3L, 5, 4500.0, 22500.0),
        createTestSaleItem(8L, 5L, 4L, 4, 4500.0, 18000.0)
    )

    val testExpenses = listOf(
        createTestExpense(1L, LocalDateTime.now().minusDays(2), com.chibychibystore.data.local.entity.ExpenseCategory.UTILITIES, 50000.0, "Electricity bill", approvedBy = null, createdBy = 1L),
        createTestExpense(2L, LocalDateTime.now().minusDays(4), com.chibychibystore.data.local.entity.ExpenseCategory.SUPPLIES_MAINTENANCE, 25000.0, "Office supplies", approvedBy = null, createdBy = 2L),
        createTestExpense(3L, LocalDateTime.now().minusDays(6), com.chibychibystore.data.local.entity.ExpenseCategory.RENT_LEASE, 200000.0, "Monthly rent", approvedBy = null, createdBy = 1L)
    )
}