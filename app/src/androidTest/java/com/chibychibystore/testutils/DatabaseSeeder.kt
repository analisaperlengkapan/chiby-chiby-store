package com.chibychibystore.testutils

import com.chibychibystore.data.local.AppDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import kotlinx.coroutines.runBlocking

/**
 * Database seeding utilities for integration tests
 */
class DatabaseSeeder(private val database: AppDatabase) {

    /**
     * Seed all test data
     */
    fun seedAllData() = runBlocking {
        seedCategories()
        seedWarehouses()
        seedProducts()
        seedSales()
        seedSaleItems()
        seedExpenses()
    }

    /**
     * Seed test categories
     */
    suspend fun seedCategories(categories: List<Kategori> = TestDataBuilder.testCategories) {
        categories.forEach { category ->
            database.kategoriDao().insert(category)
        }
    }

    /**
     * Seed test warehouses
     */
    suspend fun seedWarehouses(warehouses: List<Gudang> = TestDataBuilder.testWarehouses) {
        warehouses.forEach { warehouse ->
            database.gudangDao().insert(warehouse)
        }
    }

    /**
     * Seed test products
     */
    suspend fun seedProducts(products: List<Produk> = TestDataBuilder.testProducts) {
        products.forEach { product ->
            database.produkDao().insert(product)
        }
    }

    /**
     * Seed test sales
     */
    suspend fun seedSales(sales: List<Penjualan> = TestDataBuilder.testSales) {
        sales.forEach { sale ->
            database.penjualanDao().insert(sale)
        }
    }

    /**
     * Seed test sale items
     */
    suspend fun seedSaleItems(saleItems: List<ItemPenjualan> = TestDataBuilder.testSaleItems) {
        saleItems.forEach { item ->
            database.itemPenjualanDao().insert(item)
        }
    }

    /**
     * Seed test expenses
     */
    suspend fun seedExpenses(expenses: List<Pengeluaran> = TestDataBuilder.testExpenses) {
        expenses.forEach { expense ->
            database.pengeluaranDao().insert(expense)
        }
    }

    /**
     * Clear all data
     */
    fun clearAllData() = runBlocking {
        database.clearAllTables()
    }

    /**
     * Seed minimal data for specific tests
     */
    suspend fun seedMinimalData() {
        seedCategories(listOf(TestDataBuilder.testCategories.first()))
        seedWarehouses(listOf(TestDataBuilder.testWarehouses.first()))
        seedProducts(listOf(TestDataBuilder.testProducts.first()))
        seedSales(listOf(TestDataBuilder.testSales.first()))
        seedSaleItems(TestDataBuilder.testSaleItems.filter { it.penjualanId == TestDataBuilder.testSales.first().id })
        seedExpenses(listOf(TestDataBuilder.testExpenses.first()))
    }

    /**
     * Get current product count
     */
    suspend fun getProductCount(): Int {
        return database.produkDao().getAll().size
    }

    /**
     * Get current warehouse count
     */
    suspend fun getWarehouseCount(): Int {
        return database.gudangDao().getAll().size
    }

    /**
     * Get current category count
     */
    suspend fun getCategoryCount(): Int {
        return database.kategoriDao().getAll().size
    }

    /**
     * Get current sales count
     */
    suspend fun getSalesCount(): Int {
        return database.penjualanDao().getAll().size
    }

    /**
     * Get current sale items count
     */
    suspend fun getSaleItemsCount(): Int {
        return database.itemPenjualanDao().getAll().size
    }

    /**
     * Get current expenses count
     */
    suspend fun getExpensesCount(): Int {
        return database.pengeluaranDao().getAll().size
    }
}