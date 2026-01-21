package com.chibychibystore.testutils

import com.chibychibystore.data.local.entity.*
import java.time.LocalDateTime

/**
 * Minimal TestDataBuilder for unit tests (moved from androidTest)
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

    fun createTestSale(
        id: Long = 0L,
        totalAmount: Double = 22500.0,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cashierId: Long = 1L
    ) = Penjualan(
        id = id,
        saleDate = java.util.Date.from(LocalDateTime.now().minusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant()),
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
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
        amount: Double = 50000.0,
        category: KategoriPengeluaran = KategoriPengeluaran.UTILITIES,
        description: String = "Electricity bill",
        approvedBy: Long? = null,
        createdBy: Long = 1L
    ) = Pengeluaran(
        id = id,
        expenseDate = java.util.Date.from(LocalDateTime.now().minusDays(2).atZone(java.time.ZoneId.systemDefault()).toInstant()),
        category = category,
        amount = amount,
        description = description,
        approvedBy = approvedBy,
        createdBy = createdBy
    )

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
        createTestProduct(5L, "Low Stock Item", "555555555555", 1L, 10000.0, 15000.0, 5, 1L, 20),
        createTestProduct(6L, "Out of Stock Item", "666666666666", 1L, 10000.0, 15000.0, 0, 1L, 0)
    )

    val testSales = listOf(
        createTestSale(1L, 22500.0, PaymentMethod.CASH, 1L),
        createTestSale(2L, 13500.0, PaymentMethod.CASH, 2L)
    )

    val testSaleItems = listOf(
        createTestSaleItem(1L, 1L, 1L, 3, 7500.0, 22500.0),
        createTestSaleItem(2L, 2L, 2L, 2, 6000.0, 12000.0)
    )

    val testExpenses = listOf(
        createTestExpense(1L, 50000.0, KategoriPengeluaran.UTILITIES, "Electricity bill", null, 1L),
        createTestExpense(2L, 25000.0, KategoriPengeluaran.SUPPLIES_MAINTENANCE, "Office supplies", null, 2L),
        createTestExpense(3L, 200000.0, KategoriPengeluaran.RENT_LEASE, "Monthly rent", null, 1L)
    )

    object ExpectedCalculations {
        val INVENTORY_VALUE = 1920000.0
        val TOTAL_ASSETS = 1920000.0
        val TOTAL_LIABILITIES = 0.0
        val TOTAL_EQUITY = 1920000.0
        val TOTAL_EXPENSES = 275000.0
    }
}
