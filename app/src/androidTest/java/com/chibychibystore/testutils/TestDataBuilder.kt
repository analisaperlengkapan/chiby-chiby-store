package com.chibychibystore.testutils

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test data builders for integration tests
 */
object TestDataBuilder {

    fun createTestCategory(
        id: String = "test_category_1",
        name: String = "Test Category",
        description: String = "Test category description"
    ) = Kategori(
        id = id,
        nama = name,
        deskripsi = description
    )

    fun createTestWarehouse(
        id: String = "test_warehouse_1",
        name: String = "Test Warehouse",
        location: String = "Test Location",
        capacity: Int = 1000
    ) = Gudang(
        id = id,
        nama = name,
        lokasi = location,
        kapasitas = capacity
    )

    fun createTestProduct(
        id: String = "test_product_1",
        name: String = "Test Product",
        barcode: String = "123456789012",
        categoryId: String = "test_category_1",
        costPrice: Double = 10000.0,
        sellingPrice: Double = 15000.0,
        stock: Int = 50,
        warehouseId: String = "test_warehouse_1",
        minStock: Int = 10
    ) = Produk(
        id = id,
        nama = name,
        barcode = barcode,
        kategoriId = categoryId,
        hargaBeli = costPrice,
        hargaJual = sellingPrice,
        stok = stock,
        gudangId = warehouseId,
        minStok = minStock
    )

    fun createLowStockProduct(
        id: String = "low_stock_product",
        name: String = "Low Stock Product",
        stock: Int = 2,
        minStock: Int = 10
    ) = createTestProduct(
        id = id,
        name = name,
        stock = stock,
        minStock = minStock
    )

    fun createOutOfStockProduct(
        id: String = "out_of_stock_product",
        name: String = "Out of Stock Product",
        stock: Int = 0
    ) = createTestProduct(
        id = id,
        name = name,
        stock = stock
    )

    fun createTestSale(
        id: String = "test_sale_1",
        date: LocalDateTime = LocalDateTime.now().minusDays(1),
        totalAmount: Double = 22500.0,
        paymentMethod: String = "CASH",
        cashierId: String = "owner"
    ) = Penjualan(
        id = id,
        tanggalPenjualan = date,
        totalJumlah = totalAmount,
        metodePembayaran = paymentMethod,
        kasirId = cashierId
    )

    fun createTestSaleItem(
        id: String = "test_sale_item_1",
        saleId: String = "test_sale_1",
        productId: String = "prod_1",
        quantity: Int = 3,
        unitPrice: Double = 7500.0,
        totalPrice: Double = 22500.0
    ) = ItemPenjualan(
        id = id,
        penjualanId = saleId,
        produkId = productId,
        jumlah = quantity,
        hargaSatuan = unitPrice,
        totalHarga = totalPrice
    )

    fun createTestExpense(
        id: String = "test_expense_1",
        date: LocalDateTime = LocalDateTime.now().minusDays(2),
        category: String = "Utilities",
        amount: Double = 50000.0,
        description: String = "Electricity bill",
        approvedBy: String = "owner",
        createdBy: String = "manager"
    ) = Pengeluaran(
        id = id,
        tanggalPengeluaran = date,
        kategori = category,
        jumlah = amount,
        deskripsi = description,
        disetujuiOleh = approvedBy,
        dibuatOleh = createdBy
    )

    // Test data collections
    val testCategories = listOf(
        createTestCategory("cat_1", "Food", "Food products"),
        createTestCategory("cat_2", "Beverages", "Drink products"),
        createTestCategory("cat_3", "Electronics", "Electronic products")
    )

    val testWarehouses = listOf(
        createTestWarehouse("wh_1", "Main Warehouse", "Jakarta", 1000),
        createTestWarehouse("wh_2", "Branch Warehouse", "Bandung", 500)
    )

    val testProducts = listOf(
        createTestProduct("prod_1", "Apple", "111111111111", "cat_1", 5000.0, 7500.0, 100, "wh_1", 20),
        createTestProduct("prod_2", "Orange", "222222222222", "cat_1", 4000.0, 6000.0, 80, "wh_1", 15),
        createTestProduct("prod_3", "Coca Cola", "333333333333", "cat_2", 3000.0, 4500.0, 200, "wh_2", 30),
        createTestProduct("prod_4", "Sprite", "444444444444", "cat_2", 3000.0, 4500.0, 150, "wh_2", 25),
        createLowStockProduct("prod_5", "Low Stock Item", 5, 20),
        createOutOfStockProduct("prod_6", "Out of Stock Item")
    )

    val testSales = listOf(
        createTestSale("sale_1", LocalDateTime.now().minusDays(1), 22500.0, "CASH", "owner"),
        createTestSale("sale_2", LocalDateTime.now().minusDays(2), 13500.0, "CASH", "cashier"),
        createTestSale("sale_3", LocalDateTime.now().minusDays(3), 30000.0, "CARD", "owner"),
        createTestSale("sale_4", LocalDateTime.now().minusDays(4), 18000.0, "CASH", "cashier"),
        createTestSale("sale_5", LocalDateTime.now().minusDays(5), 42000.0, "CARD", "owner")
    )

    val testSaleItems = listOf(
        createTestSaleItem("item_1", "sale_1", "prod_1", 3, 7500.0, 22500.0),
        createTestSaleItem("item_2", "sale_2", "prod_2", 2, 6000.0, 12000.0),
        createTestSaleItem("item_3", "sale_2", "prod_3", 1, 4500.0, 1500.0),
        createTestSaleItem("item_4", "sale_3", "prod_4", 4, 4500.0, 18000.0),
        createTestSaleItem("item_5", "sale_3", "prod_1", 2, 7500.0, 15000.0),
        createTestSaleItem("item_6", "sale_4", "prod_2", 3, 6000.0, 18000.0),
        createTestSaleItem("item_7", "sale_5", "prod_3", 5, 4500.0, 22500.0),
        createTestSaleItem("item_8", "sale_5", "prod_4", 4, 4500.0, 18000.0)
    )

    val testExpenses = listOf(
        createTestExpense("exp_1", LocalDateTime.now().minusDays(2), "Utilities", 50000.0, "Electricity bill", "owner", "manager"),
        createTestExpense("exp_2", LocalDateTime.now().minusDays(4), "Supplies", 25000.0, "Office supplies", "owner", "cashier"),
        createTestExpense("exp_3", LocalDateTime.now().minusDays(6), "Rent", 200000.0, "Monthly rent", "owner", "owner")
    )
}